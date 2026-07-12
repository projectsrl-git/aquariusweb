package com.aquarius.repository.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

/**
 * Statistiche fatturato vendite (read-only) sulle fatture esistenti:
 * testate U_FAT_TT (misura principale = ORD_IMPONIB, affiancata da
 * t_netto) e righe U_FAT_DD (per articolo: ORD_QTAORD e ORD_VALORE =
 * valore riga netto sconti; join testata-righe TAGGANCIO=DAGGANCIO come
 * in InvoiceRowRepository).
 *
 * Equivalente web di menu_fatturato / MENU_FATTURATO_attivo (filtri
 * legacy: data fattura dal/al, cliente, articolo, causale...). Le note
 * di accredito NON sono compensate ne' escluse: la colonna causale e i
 * conteggi sono esposti cosi' come sono (nessuna assunzione sui segni).
 * Il mese e' derivato da ORD_DATORD 'yyyy/MM/dd' (SUBSTRING 6,2).
 */
@Repository
public class VenditeStatsDao {

    private final NamedParameterJdbcTemplate jdbc;

    public VenditeStatsDao(@Qualifier("tenantDataSource") DataSource tenantDataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(tenantDataSource);
    }

    @Data @AllArgsConstructor
    public static class Bucket {
        private String key;        // mese / codice cliente / codice articolo / anno
        private String label;      // descrizione (ragione sociale, descrizione articolo...)
        private long documenti;    // n. fatture (o righe per articolo)
        private BigDecimal imponibile;
        private BigDecimal totale; // t_netto (per cliente/mese) o ORD_VALORE (articolo)
        private BigDecimal quantita; // solo per articolo
    }

    private static final String TT_WHERE = """
        ORD_CODSOC = :soc AND ORD_ANNO = :anno
        AND (:meseDa = '' OR SUBSTRING(ORD_DATORD, 6, 2) >= :meseDa)
        AND (:meseA  = '' OR SUBSTRING(ORD_DATORD, 6, 2) <= :meseA)
        """;

    /** Fatturato per mese (testate). */
    public List<Bucket> byMonth(String soc, String anno, String meseDa, String meseA) {
        String sql = "SELECT SUBSTRING(ORD_DATORD, 6, 2) AS K, COUNT(*) AS N, "
            + "SUM(ORD_IMPONIB) AS IMP, SUM(t_netto) AS TOT "
            + "FROM U_FAT_TT WHERE " + TT_WHERE
            + "GROUP BY SUBSTRING(ORD_DATORD, 6, 2) ORDER BY K";
        return jdbc.query(sql, params(soc, anno, meseDa, meseA), (rs, i) -> new Bucket(
            t(rs.getString("K")), "", rs.getLong("N"),
            rs.getBigDecimal("IMP"), rs.getBigDecimal("TOT"), null));
    }

    /** Fatturato per cliente (testate), ordinato per imponibile decrescente. */
    public List<Bucket> byCustomer(String soc, String anno, String meseDa, String meseA, int top) {
        String sql = "SELECT TOP (:top) LTRIM(RTRIM(ORD_CODCLI)) AS K, MAX(ORD_RAGSOC) AS L, "
            + "COUNT(*) AS N, SUM(ORD_IMPONIB) AS IMP, SUM(t_netto) AS TOT "
            + "FROM U_FAT_TT WHERE " + TT_WHERE
            + "GROUP BY LTRIM(RTRIM(ORD_CODCLI)) ORDER BY SUM(ORD_IMPONIB) DESC";
        return jdbc.query(sql, params(soc, anno, meseDa, meseA).addValue("top", top),
            (rs, i) -> new Bucket(
                t(rs.getString("K")), t(rs.getString("L")), rs.getLong("N"),
                rs.getBigDecimal("IMP"), rs.getBigDecimal("TOT"), null));
    }

    /**
     * Fatturato per articolo (righe: quantita' ORD_QTAORD e valore riga
     * ORD_VALORE netto sconti), ordinato per valore decrescente.
     */
    public List<Bucket> byArticle(String soc, String anno, String meseDa, String meseA, int top) {
        String sql = "SELECT TOP (:top) LTRIM(RTRIM(d.ORD_CODART)) AS K, MAX(d.ORD_DESART) AS L, "
            + "COUNT(*) AS N, SUM(d.ORD_QTAORD) AS QTA, SUM(d.ORD_VALORE) AS VAL "
            + "FROM U_FAT_DD d JOIN U_FAT_TT t ON t.TAGGANCIO = d.DAGGANCIO "
            + "WHERE t.ORD_CODSOC = :soc AND t.ORD_ANNO = :anno "
            + "AND (:meseDa = '' OR SUBSTRING(t.ORD_DATORD, 6, 2) >= :meseDa) "
            + "AND (:meseA  = '' OR SUBSTRING(t.ORD_DATORD, 6, 2) <= :meseA) "
            + "AND LTRIM(RTRIM(d.ORD_CODART)) <> '' "
            + "GROUP BY LTRIM(RTRIM(d.ORD_CODART)) ORDER BY SUM(d.ORD_VALORE) DESC";
        return jdbc.query(sql, params(soc, anno, meseDa, meseA).addValue("top", top),
            (rs, i) -> new Bucket(
                t(rs.getString("K")), t(rs.getString("L")), rs.getLong("N"),
                null, rs.getBigDecimal("VAL"), rs.getBigDecimal("QTA")));
    }

    /** Confronto pluriennale: totali per anno (tutti gli anni presenti). */
    public List<Bucket> byYear(String soc) {
        String sql = "SELECT ORD_ANNO AS K, COUNT(*) AS N, "
            + "SUM(ORD_IMPONIB) AS IMP, SUM(t_netto) AS TOT "
            + "FROM U_FAT_TT WHERE ORD_CODSOC = :soc "
            + "GROUP BY ORD_ANNO ORDER BY ORD_ANNO";
        return jdbc.query(sql, new MapSqlParameterSource().addValue("soc", soc),
            (rs, i) -> new Bucket(
                t(rs.getString("K")), "", rs.getLong("N"),
                rs.getBigDecimal("IMP"), rs.getBigDecimal("TOT"), null));
    }

    /** Matrice anno×mese per il confronto pluriennale (ultimi N anni). */
    public List<YearMonthCell> yearMonthMatrix(String soc, int lastYears) {
        String sql = "SELECT ORD_ANNO AS ANNO, SUBSTRING(ORD_DATORD, 6, 2) AS MESE, "
            + "SUM(ORD_IMPONIB) AS IMP "
            + "FROM U_FAT_TT WHERE ORD_CODSOC = :soc "
            + "AND TRY_CONVERT(INT, ORD_ANNO) >= "
            + "(SELECT MAX(TRY_CONVERT(INT, ORD_ANNO)) FROM U_FAT_TT WHERE ORD_CODSOC = :soc) - :back "
            + "GROUP BY ORD_ANNO, SUBSTRING(ORD_DATORD, 6, 2) ORDER BY ANNO, MESE";
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("soc", soc).addValue("back", lastYears - 1);
        return jdbc.query(sql, p, (rs, i) -> new YearMonthCell(
            t(rs.getString("ANNO")), t(rs.getString("MESE")), rs.getBigDecimal("IMP")));
    }

    @Data @AllArgsConstructor
    public static class YearMonthCell {
        private String anno;
        private String mese;
        private BigDecimal imponibile;
    }

    private MapSqlParameterSource params(String soc, String anno, String meseDa, String meseA) {
        return new MapSqlParameterSource()
            .addValue("soc", soc).addValue("anno", anno)
            .addValue("meseDa", meseDa == null ? "" : meseDa)
            .addValue("meseA", meseA == null ? "" : meseA);
    }

    private static String t(String s) { return s == null ? "" : s.trim(); }
}
