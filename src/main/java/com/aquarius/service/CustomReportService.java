package com.aquarius.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Esecuzione sicura di query SQL custom — porting fedele da CReaM
 * ({@code com.cream.service.CustomReportService}).
 *
 * Adattamento multi-tenant: il {@link JdbcTemplate} è costruito sul
 * {@code tenantDataSource} (che è il routing). Le query partono SEMPRE
 * sul DB della società corrente — il TenantContext deve essere già
 * popolato (lo fa il TenantRequestFilter).
 *
 * Vincoli di sicurezza (ereditati da CReaM, immutati):
 *   - solo SELECT permessi
 *   - blacklist di parole chiave pericolose (DROP, INSERT, UPDATE, EXEC, ...)
 *   - parametri nominali (:paramName) trasformati in PreparedStatement (?)
 */
@Service
public class CustomReportService {

    private final JdbcTemplate jdbcTemplate;

    public CustomReportService(@Qualifier("tenantDataSource") DataSource tenantDs) {
        this.jdbcTemplate = new JdbcTemplate(tenantDs);
    }

    // ─── safety patterns (identici a CReaM) ──────────────────────────────
    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
        "(?i)(\\bDROP\\b|\\bDELETE\\b|\\bTRUNCATE\\b|\\bINSERT\\b|\\bUPDATE\\b|\\bALTER\\b|" +
        "\\bCREATE\\b|\\bGRANT\\b|\\bREVOKE\\b|\\bEXEC\\b|\\bEXECUTE\\b|\\bxp_|\\bsp_)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "^\\s*(?:WITH\\s+|SELECT\\s+)", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PARAM_PATTERN = Pattern.compile(
        ":([a-zA-Z_][a-zA-Z0-9_]*)"
    );

    // ─── validation ─────────────────────────────────────────────────────
    public boolean isQuerySafe(String sql) {
        if (sql == null || sql.trim().isEmpty()) return false;
        if (!SELECT_PATTERN.matcher(sql).find())  return false;
        if (DANGEROUS_KEYWORDS.matcher(sql).find()) return false;
        return true;
    }

    public List<String> extractParameters(String sql) {
        List<String> params = new ArrayList<>();
        Matcher m = PARAM_PATTERN.matcher(sql);
        while (m.find()) {
            String p = m.group(1);
            if (!params.contains(p)) params.add(p);
        }
        return params;
    }

    private String prepareQueryWithParams(String sql, List<String> paramOrder) {
        Matcher m = PARAM_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            paramOrder.add(m.group(1));
            m.appendReplacement(sb, "?");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ─── execution ──────────────────────────────────────────────────────
    public Map<String, Object> executeQuery(String sql) {
        return executeQuery(sql, new HashMap<>());
    }

    public Map<String, Object> executeQuery(String sql, Map<String, Object> params) {
        if (!isQuerySafe(sql)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Query non consentita. Solo SELECT statements sono permessi.");
            error.put("errorType", "SecurityException");
            return error;
        }

        try {
            List<String> paramOrder = new ArrayList<>();
            String preparedSql = prepareQueryWithParams(sql, paramOrder);

            Object[] paramValues = new Object[paramOrder.size()];
            for (int i = 0; i < paramOrder.size(); i++) {
                paramValues[i] = params.get(paramOrder.get(i));
            }

            List<Map<String, Object>> rows = paramValues.length > 0
                ? jdbcTemplate.queryForList(preparedSql, paramValues)
                : jdbcTemplate.queryForList(sql);

            List<String> columns = new ArrayList<>();
            if (!rows.isEmpty()) columns.addAll(rows.get(0).keySet());

            Map<String, Object> result = new HashMap<>();
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("rowCount", rows.size());
            result.put("success", true);
            return result;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("errorType", e.getClass().getSimpleName());
            return error;
        }
    }

    public Map<String, Object> validateQuery(String sql) {
        Map<String, Object> result = new HashMap<>();
        if (!isQuerySafe(sql)) {
            result.put("valid", false);
            result.put("error", "Query non consentita. Solo SELECT statements sono permessi.");
            return result;
        }
        try {
            // wrappa la query con WHERE 1=0 per validare la sintassi senza fetch
            jdbcTemplate.execute("SELECT * FROM (" + sql + ") AS validation_subquery WHERE 1=0");
            result.put("valid", true);
            result.put("message", "Query sintatticamente corretta");
        } catch (Exception e) {
            result.put("valid", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ─── schema introspection (utility per il form di creazione query) ──
    public List<String> getAvailableTables() {
        try {
            return jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME",
                String.class
            );
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, String>> getTableColumns(String tableName) {
        try {
            return jdbcTemplate.query(
                "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                (rs, rowNum) -> {
                    Map<String, String> col = new HashMap<>();
                    col.put("name", rs.getString("COLUMN_NAME"));
                    col.put("type", rs.getString("DATA_TYPE"));
                    return col;
                },
                tableName
            );
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
