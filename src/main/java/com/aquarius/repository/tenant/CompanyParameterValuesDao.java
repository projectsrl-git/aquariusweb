package com.aquarius.repository.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

/**
 * DAO in SOLA LETTURA dei valori correnti dei parametri aziendali:
 * {@code SELECT TOP 1 *} da U_AZI_AN / U_AZI_PA / U_AZI_PB per la societa'
 * corrente, restituiti come mappa COLONNA → valore formattato. Approccio
 * metadata-driven (ResultSetMetaData): nessuna entity da 700+ colonne, e le
 * colonne assenti in una release del DB semplicemente non compaiono nella
 * mappa (il viewer le segnala come non disponibili).
 *
 * I nomi tabella sono IDENTIFICATORI CONTROLLATI (lista fissa qui sotto,
 * stesso argomento di sicurezza di WarehouseValuationDao).
 */
@Repository
@Slf4j
public class CompanyParameterValuesDao {

    private static final String[] TABLES = {"U_AZI_AN", "U_AZI_PA", "U_AZI_PB"};

    private final NamedParameterJdbcTemplate jdbc;

    public CompanyParameterValuesDao(@Qualifier("tenantDataSource") DataSource tenantDataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(tenantDataSource);
    }

    /** Mappa COLONNA (upper-case) → valore leggibile, per la societa' data. */
    public Map<String, String> loadValues(String societyCode) {
        Map<String, String> out = new HashMap<>();
        for (String table : TABLES) {
            try {
                jdbc.query(
                    "SELECT TOP 1 * FROM " + table + " WHERE AZI_CODSOC = :soc",
                    new MapSqlParameterSource("soc", societyCode),
                    rs -> {
                        ResultSetMetaData md = rs.getMetaData();
                        for (int i = 1; i <= md.getColumnCount(); i++) {
                            String col = md.getColumnName(i).toUpperCase();
                            if (!col.startsWith("AZI_")) continue;
                            Object v = rs.getObject(i);
                            out.putIfAbsent(col, format(v));
                        }
                    });
            } catch (Exception e) {
                log.error("Lettura parametri da {} fallita: {}", table, e.getMessage());
            }
        }
        return out;
    }

    private static String format(Object v) {
        if (v == null) return "";
        if (v instanceof Boolean b) return b ? "true" : "false";
        if (v instanceof BigDecimal d) return d.stripTrailingZeros().toPlainString();
        String s = v.toString().trim();
        return s;
    }
}
