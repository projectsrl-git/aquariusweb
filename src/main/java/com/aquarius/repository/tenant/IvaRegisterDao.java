package com.aquarius.repository.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

/**
 * DAO in SOLA LETTURA dei registri IVA ("bollati").
 *
 * Semantica verificata in BOLLATI.PRG ("CARICAMENTO BOLLATI IVA CLIENTI /
 * FORNITORI / CORRISPETTIVI"):
 * - le righe dei registri sono CARICATE dalla prima nota (MOV_CONT) per
 *   societa'/anno/MESE: ogni caricamento cancella e rigenera il mese
 *   (IVAVENDI: "ANNULLO DEL BOLLATO PRECEDENTE"); il viewer mostra quindi
 *   i registri cosi' come caricati l'ultima volta per ciascun mese;
 * - VENDITE e CORRISPETTIVI → U_IVA_CL (corrispettivi = IVA_FATNOT='C',
 *   procedura CORRISP; vendite = 'F' fattura / 'N' nota di accredito);
 * - ACQUISTI → U_IVF_CL (con indeducibilita' IVF_IND100/IVF_DEDUCIBILE e
 *   flag bolla doganale IVF_BDOG);
 * - IVA_CHIAVE / IVF_CHIAVE = numero PROTOCOLLO del registro (VNUMPRT);
 * - totali di periodo per aliquota → U_IVA_TO, precalcolati dal
 *   caricamento, con ITO_CLIFOR: C=clienti, F=fornitori, D=fatture
 *   differite da DDT, E=fatture CEE, R=reverse charge, A=autofatture
 *   (commenti verbatim del PRG) e ITO_FATNOT F/N/C.
 *
 * Esistono varianti *_DTDOC (liquidazione per data documento, parametro
 * PUB_LIQDTDOC): non esposte qui, tracciate nel migration tracker.
 */
@Repository
@Slf4j
public class IvaRegisterDao {

    private final NamedParameterJdbcTemplate jdbc;

    public IvaRegisterDao(@Qualifier("tenantDataSource") DataSource tenantDataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(tenantDataSource);
    }

    // ─── DTO ─────────────────────────────────────────────────────────────

    @Data @AllArgsConstructor
    public static class RegisterRow {
        private long protocollo;        // IVA_CHIAVE / IVF_CHIAVE
        private String mese;
        private String dataFattura;     // varchar dd/mm/yyyy-like (itDate in UI)
        private String numeroFattura;
        private String numeroProtocolloPn; // MOV_NUMPRO (prima nota) — informativo
        private String codice;          // controparte (cliente o fornitore)
        private String ragioneSociale;
        private String fatNot;          // F=fattura N=nota accredito C=corrispettivo
        private String codIva;
        private BigDecimal aliquota;
        private String desIva;
        private String ind100;          // acquisti: 'S' = indeducibile 100%
        private Boolean deducibile;     // acquisti
        private Boolean bollaDoganale;  // acquisti
        private BigDecimal imponibile;
        private BigDecimal imposta;
        private BigDecimal totale;
    }

    @Data @AllArgsConstructor
    public static class AliquotaTotal {
        private String codIva;
        private String desIva;
        private BigDecimal aliquota;
        private long righe;
        private BigDecimal imponibile;
        private BigDecimal imposta;
    }

    /** Colonne ordinabili (identificatori controllati). */
    public enum SortKey {
        PROTOCOLLO("CHIAVE"), DATA("DTFATT"), RAGSOC("RAGSOC"),
        IMPONIBILE("IMPONI"), IMPOSTA("IMPOST"), TOTALE("TOTALE");
        private final String suffix;
        SortKey(String s) { this.suffix = s; }
        String expr(String prefix) { return prefix + "_" + suffix; }
        public static SortKey of(String s, SortKey def) {
            try { return s == null ? def : SortKey.valueOf(s.toUpperCase()); }
            catch (IllegalArgumentException e) { return def; }
        }
    }

    // ─── Registro vendite / corrispettivi (U_IVA_CL) ─────────────────────

    private static final String CL_WHERE = """
        IVA_CODSOC = :soc AND IVA_ANNO = :anno
        AND (:mese = '' OR IVA_MESE = :mese)
        AND ((:corrisp = 1 AND IVA_FATNOT = 'C') OR (:corrisp = 0 AND IVA_FATNOT <> 'C'))
        AND (:q = '' OR IVA_RAGSOC LIKE :qLike OR IVA_NUMFAT LIKE :qLike
             OR LTRIM(RTRIM(IVA_CODCLI)) LIKE :qLike)
        """;

    public long countSales(String soc, String anno, String mese, String q, boolean corrispettivi) {
        Long n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM U_IVA_CL WHERE " + CL_WHERE,
            clParams(soc, anno, mese, q, corrispettivi), Long.class);
        return n == null ? 0 : n;
    }

    public List<RegisterRow> sales(String soc, String anno, String mese, String q,
                                   boolean corrispettivi, SortKey sort, boolean asc,
                                   int page, int size) {
        String sql = "SELECT IVA_CHIAVE, IVA_MESE, IVA_DTFATT, IVA_NUMFAT, MOV_NUMPRO, "
            + "IVA_CODCLI, IVA_RAGSOC, IVA_FATNOT, IVA_CODIVA, IVA_ALIQUO, IVA_DESIVA, "
            + "IVA_IMPONI, IVA_IMPOST, IVA_TOTALE "
            + "FROM U_IVA_CL WHERE " + CL_WHERE
            + "ORDER BY " + sort.expr("IVA") + (asc ? " ASC" : " DESC") + ", IVA_CHIAVE ASC "
            + "OFFSET :off ROWS FETCH NEXT :lim ROWS ONLY";
        MapSqlParameterSource p = clParams(soc, anno, mese, q, corrispettivi)
            .addValue("off", page * size).addValue("lim", size);
        return jdbc.query(sql, p, (rs, i) -> new RegisterRow(
            rs.getLong("IVA_CHIAVE"),
            t(rs.getString("IVA_MESE")),
            t(rs.getString("IVA_DTFATT")),
            t(rs.getString("IVA_NUMFAT")),
            t(rs.getString("MOV_NUMPRO")),
            t(rs.getString("IVA_CODCLI")),
            t(rs.getString("IVA_RAGSOC")),
            t(rs.getString("IVA_FATNOT")),
            t(rs.getString("IVA_CODIVA")),
            rs.getBigDecimal("IVA_ALIQUO"),
            t(rs.getString("IVA_DESIVA")),
            null, null, null,
            rs.getBigDecimal("IVA_IMPONI"),
            rs.getBigDecimal("IVA_IMPOST"),
            rs.getBigDecimal("IVA_TOTALE")));
    }

    /** Totali per aliquota dell'insieme filtrato (non paginato). */
    public List<AliquotaTotal> salesTotalsByAliquota(String soc, String anno, String mese,
                                                     String q, boolean corrispettivi) {
        String sql = "SELECT IVA_CODIVA, MAX(IVA_DESIVA) AS DESIVA, MAX(IVA_ALIQUO) AS ALIQUO, "
            + "COUNT(*) AS NR, SUM(IVA_IMPONI) AS IMPONI, SUM(IVA_IMPOST) AS IMPOST "
            + "FROM U_IVA_CL WHERE " + CL_WHERE
            + "GROUP BY IVA_CODIVA ORDER BY IVA_CODIVA";
        return jdbc.query(sql, clParams(soc, anno, mese, q, corrispettivi),
            (rs, i) -> new AliquotaTotal(
                t(rs.getString("IVA_CODIVA")), t(rs.getString("DESIVA")),
                rs.getBigDecimal("ALIQUO"), rs.getLong("NR"),
                rs.getBigDecimal("IMPONI"), rs.getBigDecimal("IMPOST")));
    }

    private MapSqlParameterSource clParams(String soc, String anno, String mese,
                                           String q, boolean corrispettivi) {
        String qq = q == null ? "" : q.trim();
        return new MapSqlParameterSource()
            .addValue("soc", soc).addValue("anno", anno)
            .addValue("mese", mese == null ? "" : mese)
            .addValue("q", qq).addValue("qLike", "%" + qq + "%")
            .addValue("corrisp", corrispettivi ? 1 : 0);
    }

    // ─── Registro acquisti (U_IVF_CL) ────────────────────────────────────

    private static final String IVF_WHERE = """
        IVF_CODSOC = :soc AND IVF_ANNO = :anno
        AND (:mese = '' OR IVF_MESE = :mese)
        AND (:q = '' OR IVF_RAGSOC LIKE :qLike OR IVF_NUMFAT LIKE :qLike
             OR LTRIM(RTRIM(IVF_CODCLI)) LIKE :qLike)
        """;

    public long countPurchases(String soc, String anno, String mese, String q) {
        Long n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM U_IVF_CL WHERE " + IVF_WHERE,
            ivfParams(soc, anno, mese, q), Long.class);
        return n == null ? 0 : n;
    }

    public List<RegisterRow> purchases(String soc, String anno, String mese, String q,
                                       SortKey sort, boolean asc, int page, int size) {
        String sql = "SELECT IVF_CHIAVE, IVF_MESE, IVF_DTFATT, IVF_NUMFAT, IVF_NUMPRO, "
            + "IVF_CODCLI, IVF_RAGSOC, IVF_FATNOT, IVF_CODIVA, IVF_ALIQUO, IVF_DESIVA, "
            + "IVF_IND100, IVF_DEDUCIBILE, IVF_BDOG, IVF_IMPONI, IVF_IMPOST, IVF_TOTALE "
            + "FROM U_IVF_CL WHERE " + IVF_WHERE
            + "ORDER BY " + sort.expr("IVF") + (asc ? " ASC" : " DESC") + ", IVF_CHIAVE ASC "
            + "OFFSET :off ROWS FETCH NEXT :lim ROWS ONLY";
        MapSqlParameterSource p = ivfParams(soc, anno, mese, q)
            .addValue("off", page * size).addValue("lim", size);
        return jdbc.query(sql, p, (rs, i) -> new RegisterRow(
            rs.getLong("IVF_CHIAVE"),
            t(rs.getString("IVF_MESE")),
            t(rs.getString("IVF_DTFATT")),
            t(rs.getString("IVF_NUMFAT")),
            t(rs.getString("IVF_NUMPRO")),
            t(rs.getString("IVF_CODCLI")),
            t(rs.getString("IVF_RAGSOC")),
            t(rs.getString("IVF_FATNOT")),
            t(rs.getString("IVF_CODIVA")),
            rs.getBigDecimal("IVF_ALIQUO"),
            t(rs.getString("IVF_DESIVA")),
            t(rs.getString("IVF_IND100")),
            rs.getObject("IVF_DEDUCIBILE") == null ? null : rs.getBoolean("IVF_DEDUCIBILE"),
            rs.getObject("IVF_BDOG") == null ? null : rs.getBoolean("IVF_BDOG"),
            rs.getBigDecimal("IVF_IMPONI"),
            rs.getBigDecimal("IVF_IMPOST"),
            rs.getBigDecimal("IVF_TOTALE")));
    }

    public List<AliquotaTotal> purchaseTotalsByAliquota(String soc, String anno,
                                                        String mese, String q) {
        String sql = "SELECT IVF_CODIVA, MAX(IVF_DESIVA) AS DESIVA, MAX(IVF_ALIQUO) AS ALIQUO, "
            + "COUNT(*) AS NR, SUM(IVF_IMPONI) AS IMPONI, SUM(IVF_IMPOST) AS IMPOST "
            + "FROM U_IVF_CL WHERE " + IVF_WHERE
            + "GROUP BY IVF_CODIVA ORDER BY IVF_CODIVA";
        return jdbc.query(sql, ivfParams(soc, anno, mese, q),
            (rs, i) -> new AliquotaTotal(
                t(rs.getString("IVF_CODIVA")), t(rs.getString("DESIVA")),
                rs.getBigDecimal("ALIQUO"), rs.getLong("NR"),
                rs.getBigDecimal("IMPONI"), rs.getBigDecimal("IMPOST")));
    }

    private MapSqlParameterSource ivfParams(String soc, String anno, String mese, String q) {
        String qq = q == null ? "" : q.trim();
        return new MapSqlParameterSource()
            .addValue("soc", soc).addValue("anno", anno)
            .addValue("mese", mese == null ? "" : mese)
            .addValue("q", qq).addValue("qLike", "%" + qq + "%");
    }

    private static String t(String s) { return s == null ? "" : s.trim(); }
}
