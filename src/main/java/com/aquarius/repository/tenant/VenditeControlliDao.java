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
 * Controlli read-only sui documenti di vendita. Criteri DICHIARATI:
 *
 * 1) DDT non/parzialmente fatturati — meccanismo verificato in sessione
 *    1 (ristampa fatture differite): la fatturazione TIMBRA le RIGHE
 *    del DDT con MOV_NUMFAT/MOV_DATFAT (U_BOL_DD). Un DDT e' "non
 *    fatturato" se nessuna riga ha MOV_NUMFAT valorizzato, "parziale"
 *    se solo alcune. NB: alcune causali possono legittimamente non
 *    essere fatturabili (resi, omaggi, conto lavorazione): la causale
 *    e' esposta in colonna e il filtro resta all'utente (NEEDS_DOMAIN
 *    nel tracker per l'elenco causali da escludere).
 *
 * 2) Sequenza numeri fattura — buchi e duplicati su
 *    TRY_CONVERT(INT, ORD_NUMORD) per anno (U_FAT_TT), tutte le serie
 *    insieme (se Impresind usa serie separate per TIPORD la verifica
 *    va raffinata: NEEDS_DOMAIN). I numeri non numerici sono esposti
 *    come anomalia di qualita' dati, non scartati in silenzio.
 *
 * 3) Sequenza protocolli registro IVA vendite — buchi su IVA_CHIAVE
 *    (U_IVA_CL, protocollo del bollato, righe non corrispettivi) per
 *    anno: e' il "controllo sequenza protocolli" lato registri gia'
 *    migrati (il PRG legacy PRNPROT lavora invece su MOV_CONT:
 *    documentato nel tracker).
 */
@Repository
public class VenditeControlliDao {

    private final NamedParameterJdbcTemplate jdbc;

    public VenditeControlliDao(@Qualifier("tenantDataSource") DataSource tenantDataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(tenantDataSource);
    }

    // ─── 1) DDT non / parzialmente fatturati ─────────────────────────────

    @Data @AllArgsConstructor
    public static class UnpairedDdt {
        private String numero;
        private String data;
        private String codCli;
        private String ragSoc;
        private String causale;
        private BigDecimal imponibile;
        private long righe;
        private long righeFatturate;
        public String getStato() {
            return righeFatturate == 0 ? "NON_FATTURATO" : "PARZIALE";
        }
    }

    public List<UnpairedDdt> unpairedDdt(String soc, String anno) {
        String sql = """
            SELECT t.ORD_NUMDDT, t.ORD_DATDDT, t.ORD_CODCLI, t.ORD_RAGSOC,
                   t.ORD_CAUS, t.ORD_IMPONIB,
                   COUNT(d.DAGGANCIO) AS RIGHE,
                   SUM(CASE WHEN LTRIM(RTRIM(COALESCE(d.MOV_NUMFAT, ''))) <> ''
                            THEN 1 ELSE 0 END) AS FATTURATE
            FROM U_BOL_TT t
            LEFT JOIN U_BOL_DD d ON t.TAGGANCIO = d.DAGGANCIO
            WHERE t.ORD_CODSOC = :soc AND t.ORD_ANNO = :anno
            GROUP BY t.ORD_NUMDDT, t.ORD_DATDDT, t.ORD_CODCLI, t.ORD_RAGSOC,
                     t.ORD_CAUS, t.ORD_IMPONIB
            HAVING SUM(CASE WHEN LTRIM(RTRIM(COALESCE(d.MOV_NUMFAT, ''))) <> ''
                            THEN 1 ELSE 0 END) < COUNT(d.DAGGANCIO)
            ORDER BY t.ORD_DATDDT, t.ORD_NUMDDT
            """;
        return jdbc.query(sql, socAnno(soc, anno), (rs, i) -> new UnpairedDdt(
            t(rs.getString("ORD_NUMDDT")), t(rs.getString("ORD_DATDDT")),
            t(rs.getString("ORD_CODCLI")), t(rs.getString("ORD_RAGSOC")),
            t(rs.getString("ORD_CAUS")), rs.getBigDecimal("ORD_IMPONIB"),
            rs.getLong("RIGHE"), rs.getLong("FATTURATE")));
    }

    // ─── 2) Sequenza numeri fattura ──────────────────────────────────────

    @Data @AllArgsConstructor
    public static class SequenceGap {
        private long dopoNumero;   // ultimo numero presente prima del buco
        private long primaNumero;  // primo numero presente dopo il buco
        public long getMancanti() { return primaNumero - dopoNumero - 1; }
    }

    /** Buchi nella numerazione fatture (numeri interi distinti, per anno). */
    public List<SequenceGap> invoiceNumberGaps(String soc, String anno) {
        String sql = """
            WITH NUMS AS (
                SELECT DISTINCT TRY_CONVERT(INT, LTRIM(RTRIM(ORD_NUMORD))) AS N
                FROM U_FAT_TT
                WHERE ORD_CODSOC = :soc AND ORD_ANNO = :anno
                  AND TRY_CONVERT(INT, LTRIM(RTRIM(ORD_NUMORD))) IS NOT NULL
            ), SEQ AS (
                SELECT N, LEAD(N) OVER (ORDER BY N) AS NEXT_N FROM NUMS
            )
            SELECT N, NEXT_N FROM SEQ WHERE NEXT_N > N + 1 ORDER BY N
            """;
        return jdbc.query(sql, socAnno(soc, anno),
            (rs, i) -> new SequenceGap(rs.getLong("N"), rs.getLong("NEXT_N")));
    }

    @Data @AllArgsConstructor
    public static class DuplicateNumber {
        private String numero;
        private long occorrenze;
        private String tipi;   // serie/TIPORD coinvolti (per giudicare se e' un falso positivo)
    }

    /** Numeri fattura duplicati nell'anno (con le serie coinvolte). */
    public List<DuplicateNumber> invoiceNumberDuplicates(String soc, String anno) {
        String sql = """
            SELECT LTRIM(RTRIM(ORD_NUMORD)) AS NUM, COUNT(*) AS N,
                   STUFF((
                       SELECT DISTINCT ',' + LTRIM(RTRIM(x.ORD_TIPORD))
                       FROM U_FAT_TT x
                       WHERE x.ORD_CODSOC = :soc AND x.ORD_ANNO = :anno
                         AND LTRIM(RTRIM(x.ORD_NUMORD)) = LTRIM(RTRIM(f.ORD_NUMORD))
                       FOR XML PATH('')), 1, 1, '') AS TIPI
            FROM U_FAT_TT f
            WHERE f.ORD_CODSOC = :soc AND f.ORD_ANNO = :anno
            GROUP BY LTRIM(RTRIM(f.ORD_NUMORD))
            HAVING COUNT(*) > 1
            ORDER BY LTRIM(RTRIM(f.ORD_NUMORD))
            """;
        return jdbc.query(sql, socAnno(soc, anno), (rs, i) -> new DuplicateNumber(
            t(rs.getString("NUM")), rs.getLong("N"), t(rs.getString("TIPI"))));
    }

    /** Numeri fattura non numerici (qualita' dati: esposti, non scartati). */
    public List<String> invoiceNumberNonNumeric(String soc, String anno) {
        String sql = """
            SELECT DISTINCT LTRIM(RTRIM(ORD_NUMORD)) AS NUM
            FROM U_FAT_TT
            WHERE ORD_CODSOC = :soc AND ORD_ANNO = :anno
              AND TRY_CONVERT(INT, LTRIM(RTRIM(ORD_NUMORD))) IS NULL
            ORDER BY NUM
            """;
        return jdbc.query(sql, socAnno(soc, anno), (rs, i) -> t(rs.getString("NUM")));
    }

    // ─── 3) Sequenza protocolli registro IVA vendite ─────────────────────

    /** Buchi nella numerazione protocollo del bollato vendite (IVA_CHIAVE). */
    public List<SequenceGap> ivaProtocolGaps(String soc, String anno) {
        String sql = """
            WITH NUMS AS (
                SELECT DISTINCT CAST(IVA_CHIAVE AS BIGINT) AS N
                FROM U_IVA_CL
                WHERE IVA_CODSOC = :soc AND IVA_ANNO = :anno
                  AND IVA_FATNOT <> 'C'
            ), SEQ AS (
                SELECT N, LEAD(N) OVER (ORDER BY N) AS NEXT_N FROM NUMS
            )
            SELECT N, NEXT_N FROM SEQ WHERE NEXT_N > N + 1 ORDER BY N
            """;
        return jdbc.query(sql, socAnno(soc, anno),
            (rs, i) -> new SequenceGap(rs.getLong("N"), rs.getLong("NEXT_N")));
    }

    private MapSqlParameterSource socAnno(String soc, String anno) {
        return new MapSqlParameterSource().addValue("soc", soc).addValue("anno", anno);
    }

    private static String t(String s) { return s == null ? "" : s.trim(); }
}
