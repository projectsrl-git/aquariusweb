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
 * DAO in SOLA LETTURA della STRUTTURA del bilancio CEE (niente valori
 * calcolati: il motore e' riservato a Opus — vedi resources/cee/README.md
 * per l'algoritmo decodificato da ceecont.PRG).
 *
 * Regole applicate (verificate nei sorgenti):
 * - ordine del prospetto = BIL_CODRIG (varchar numerico, confronto con
 *   padding a 10 come il legacy PADL);
 * - TIPO_DATO: I=Commenti, V=Dettagli di riga, T=Totali (validazione
 *   verbatim di menu_cee000);
 * - mappatura conti: U_INT_TT con riga dare (INT_CODRIG) e riga avere
 *   alternativa per saldo negativo (INT_CODRIA);
 * - totali: edge list U_COR_TT (COR_RIGA → COR_CONFLU con COR_SEGNO).
 *
 * Pannello anomalie = i tre controlli di ceecont documentati nel README:
 * conti non mappati (esclusi silenziosamente dal calcolo), mappature verso
 * voci inesistenti (conto saltato con avviso), confluenze totali rotte
 * (calcolo interrotto).
 */
@Repository
@Slf4j
public class CeeStructureDao {

    private final NamedParameterJdbcTemplate jdbc;

    public CeeStructureDao(@Qualifier("tenantDataSource") DataSource tenantDataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(tenantDataSource);
    }

    // ─── DTO ─────────────────────────────────────────────────────────────

    @Data @AllArgsConstructor
    public static class CeeRow {
        private String codRiga;      // BIL_CODRIG (trim)
        private String tipo;         // I | V | T
        private String descrizione;  // DESCRIZION
    }

    @Data @AllArgsConstructor
    public static class ContoMappato {
        private String conto;        // INT_CONTO
        private String descrizione;  // CONTI.CON_DESCR (o INT_DESCRI se assente)
        private String rigaDare;     // INT_CODRIG
        private String rigaAvere;    // INT_CODRIA (puo' essere vuota)
    }

    @Data @AllArgsConstructor
    public static class ConfluenzaTotale {
        private String riga;         // COR_RIGA (componente)
        private String descrizioneRiga;
        private String segno;        // '+' somma, altro sottrae
        private String totale;       // COR_CONFLU (destinazione)
        private String descrizioneTotale;
    }

    @Data @AllArgsConstructor
    public static class ContoNonMappato {
        private String conto;
        private String descrizione;
        private BigDecimal saldo;    // CON_IMP_D - CON_IMP_A (solo indicativo)
    }

    // ─── Struttura ───────────────────────────────────────────────────────

    /** Voci del prospetto, nell'ordine di stampa legacy (bil_codrig). */
    public List<CeeRow> structure(String soc) {
        return jdbc.query("""
            SELECT BIL_CODRIG, TIPO_DATO, DESCRIZION
            FROM BILNEW
            WHERE BIL_CODSOC = :soc
            ORDER BY BIL_CODRIG
            """,
            new MapSqlParameterSource("soc", soc),
            (rs, i) -> new CeeRow(
                t(rs.getString("BIL_CODRIG")),
                t(rs.getString("TIPO_DATO")),
                t(rs.getString("DESCRIZION"))));
    }

    /**
     * Mappature conto→voce della societa', con descrizione conto presa da
     * CONTI (anno corrente) e fallback sulla denormalizzata INT_DESCRI.
     */
    public List<ContoMappato> mappings(String soc, String anno) {
        return jdbc.query("""
            SELECT i.INT_CONTO, i.INT_CODRIG, i.INT_CODRIA,
                   COALESCE(c.CON_DESCR, i.INT_DESCRI, '') AS DESCR
            FROM U_INT_TT i
            LEFT JOIN CONTI c ON c.CON_SOC = i.INT_CODSOC
                 AND c.CON_ANNO = :anno AND c.CON_CONTO = i.INT_CONTO
            WHERE i.INT_CODSOC = :soc
            ORDER BY i.INT_CONTO
            """,
            new MapSqlParameterSource().addValue("soc", soc).addValue("anno", anno),
            (rs, i) -> new ContoMappato(
                t(rs.getString("INT_CONTO")),
                t(rs.getString("DESCR")),
                t(rs.getString("INT_CODRIG")),
                t(rs.getString("INT_CODRIA"))));
    }

    /** Confluenze riga→totale (la "gerarchia" dei totali), in ordine legacy. */
    public List<ConfluenzaTotale> totalEdges(String soc) {
        return jdbc.query("""
            SELECT COR_RIGA, COR_DESRIG, COR_SEGNO, COR_CONFLU, COR_DESCON
            FROM U_COR_TT
            WHERE COR_CODSOC = :soc
            ORDER BY COR_RIGA
            """,
            new MapSqlParameterSource("soc", soc),
            (rs, i) -> new ConfluenzaTotale(
                t(rs.getString("COR_RIGA")),
                t(rs.getString("COR_DESRIG")),
                t(rs.getString("COR_SEGNO")),
                t(rs.getString("COR_CONFLU")),
                t(rs.getString("COR_DESCON"))));
    }

    // ─── Anomalie (i tre controlli di ceecont) ───────────────────────────

    /** 1) Conti dell'esercizio SENZA mappatura CEE: esclusi silenziosamente dal calcolo. */
    public List<ContoNonMappato> unmappedAccounts(String soc, String anno, int limit) {
        return jdbc.query("""
            SELECT TOP (:lim) c.CON_CONTO, c.CON_DESCR,
                   (c.CON_IMP_D - c.CON_IMP_A) AS SALDO
            FROM CONTI c
            LEFT JOIN U_INT_TT i ON i.INT_CODSOC = c.CON_SOC
                 AND i.INT_CONTO = c.CON_CONTO
            WHERE c.CON_SOC = :soc AND c.CON_ANNO = :anno
              AND i.INT_CONTO IS NULL
            ORDER BY c.CON_CONTO
            """,
            new MapSqlParameterSource().addValue("soc", soc)
                .addValue("anno", anno).addValue("lim", limit),
            (rs, i) -> new ContoNonMappato(
                t(rs.getString("CON_CONTO")),
                t(rs.getString("CON_DESCR")),
                rs.getBigDecimal("SALDO")));
    }

    public long unmappedAccountsCount(String soc, String anno) {
        Long n = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM CONTI c
            LEFT JOIN U_INT_TT i ON i.INT_CODSOC = c.CON_SOC
                 AND i.INT_CONTO = c.CON_CONTO
            WHERE c.CON_SOC = :soc AND c.CON_ANNO = :anno
              AND i.INT_CONTO IS NULL
            """,
            new MapSqlParameterSource().addValue("soc", soc).addValue("anno", anno),
            Long.class);
        return n == null ? 0 : n;
    }

    /** 2) Mappature verso voci inesistenti in BILNEW (il conto verrebbe saltato). */
    public List<ContoMappato> brokenMappings(String soc) {
        return jdbc.query("""
            SELECT i.INT_CONTO, COALESCE(i.INT_DESCRI,'') AS DESCR,
                   i.INT_CODRIG, i.INT_CODRIA
            FROM U_INT_TT i
            LEFT JOIN BILNEW bd ON bd.BIL_CODSOC = i.INT_CODSOC
                 AND bd.BIL_CODRIG = RIGHT(REPLICATE(' ', 10) + LTRIM(RTRIM(i.INT_CODRIG)), 10)
            LEFT JOIN BILNEW ba ON ba.BIL_CODSOC = i.INT_CODSOC
                 AND ba.BIL_CODRIG = RIGHT(REPLICATE(' ', 10) + LTRIM(RTRIM(i.INT_CODRIA)), 10)
            WHERE i.INT_CODSOC = :soc
              AND (bd.id_unique IS NULL
                   OR (LTRIM(RTRIM(COALESCE(i.INT_CODRIA,''))) <> '' AND ba.id_unique IS NULL))
            ORDER BY i.INT_CONTO
            """,
            new MapSqlParameterSource("soc", soc),
            (rs, i) -> new ContoMappato(
                t(rs.getString("INT_CONTO")),
                t(rs.getString("DESCR")),
                t(rs.getString("INT_CODRIG")),
                t(rs.getString("INT_CODRIA"))));
    }

    /** 3) Confluenze totali verso voci inesistenti: BLOCCANTI per il calcolo legacy. */
    public List<ConfluenzaTotale> brokenTotalEdges(String soc) {
        return jdbc.query("""
            SELECT t.COR_RIGA, t.COR_DESRIG, t.COR_SEGNO, t.COR_CONFLU, t.COR_DESCON
            FROM U_COR_TT t
            LEFT JOIN BILNEW b ON b.BIL_CODSOC = t.COR_CODSOC
                 AND b.BIL_CODRIG = RIGHT(REPLICATE(' ', 10) + LTRIM(RTRIM(t.COR_CONFLU)), 10)
            WHERE t.COR_CODSOC = :soc AND b.id_unique IS NULL
            ORDER BY t.COR_RIGA
            """,
            new MapSqlParameterSource("soc", soc),
            (rs, i) -> new ConfluenzaTotale(
                t(rs.getString("COR_RIGA")),
                t(rs.getString("COR_DESRIG")),
                t(rs.getString("COR_SEGNO")),
                t(rs.getString("COR_CONFLU")),
                t(rs.getString("COR_DESCON"))));
    }

    private static String t(String s) { return s == null ? "" : s.trim(); }
}
