package com.aquarius.repository.tenant;

import com.aquarius.dto.magazzino.OscillazionePrezzo;
import com.aquarius.dto.magazzino.StratoFifo;
import com.aquarius.dto.magazzino.ValorizzazioneRiga;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * DAO in SOLA LETTURA per la valorizzazione del magazzino a una data
 * (dashboard §"Valorizzazione Magazzino"). Legge le tabelle legacy
 * {@code U_MAG_MO} (movimenti) e {@code u_vva_ch} (cambi) — regola 1.3
 * plug&play: nessuna scrittura, nessuna modifica alle strutture VFP.
 *
 * <h3>Scelte tecniche chiave (dal documento di specifica)</h3>
 * <ul>
 *   <li><b>Base di calcolo</b>: la colonna data (documento o registrazione) è
 *       SOSTITUITA nel testo SQL dal backend a partire dall'enum
 *       {@link DateBase} — MAI un {@code CASE WHEN} runtime, che forzerebbe
 *       la coercizione di tipo tra i due rami (rischio Msg 241).
 *       È un identificatore controllato a 2 valori: nessun rischio injection.</li>
 *   <li><b>Date come stringhe</b>: {@code MOV_DTDOCU}/{@code MOV_DTREGI} sono
 *       varchar in formato {@code yyyy/MM/dd}. I parametri data vengono
 *       bindati nello STESSO formato stringa: confronto lessicografico
 *       corretto, SARGable, e niente conversioni implicite sull'intera
 *       colonna (che esploderebbero sui record sporchi).</li>
 *   <li><b>Cambio valuta</b>: as-of join ({@code OUTER APPLY TOP 1 ...
 *       WHERE VVA_DATA <= data_mov ORDER BY VVA_DATA DESC}) applicato a
 *       TUTTE le valute — EUR ha cambio 1 in u_vva_ch, nessuna eccezione.
 *       Conversione = sempre moltiplicazione.</li>
 *   <li><b>Tie-break FIFO</b>: a parità di data, {@code MOV_PREACQ DESC}
 *       (id_unique è un GUID, non usabile come progressivo temporale).</li>
 * </ul>
 */
@Repository
@Slf4j
public class WarehouseValuationDao {

    /** Base di calcolo: quale colonna data pilota giacenza, FIFO e cambi. */
    public enum DateBase {
        DOCU("MOV_DTDOCU"),
        REGI("MOV_DTREGI");

        private final String column;
        DateBase(String column) { this.column = column; }
        public String column() { return column; }

        public static DateBase from(String s) {
            return "REGI".equalsIgnoreCase(s == null ? "" : s.trim()) ? REGI : DOCU;
        }
    }

    private static final DateTimeFormatter LEGACY_DATE =
        DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final NamedParameterJdbcTemplate jdbc;

    public WarehouseValuationDao(@Qualifier("tenantDataSource") DataSource tenantDataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(tenantDataSource);
    }

    // ════════════════════════════════════════════════════════════════════
    //  §3.2 — Valorizzazione (intero magazzino o singolo articolo)
    // ════════════════════════════════════════════════════════════════════

    private static final String SQL_VALORIZZA = """
        WITH giacenze AS (
            SELECT m.MOV_ANAART,
                   SUM(CASE m.MOV_SEGNO WHEN '+' THEN m.MOV_QTAMOV ELSE -m.MOV_QTAMOV END) AS giacenza
            FROM U_MAG_MO m
            WHERE m.MOV_CODMAG = :codmag
              AND m.%DATA% <= :dataRif
              AND (:articolo IS NULL OR m.MOV_ANAART = :articolo)
            GROUP BY m.MOV_ANAART
        ),
        carichi AS (
            SELECT m.MOV_ANAART, m.MOV_QTAMOV,
                   m.MOV_PREACQ * ISNULL(cb.cambio, 1) AS prezzo_conv,
                   CASE WHEN cb.cambio IS NULL AND m.MOV_VALUTA IS NOT NULL AND m.MOV_VALUTA <> ''
                        THEN 1 ELSE 0 END AS cambio_mancante,
                   SUM(m.MOV_QTAMOV) OVER (
                       PARTITION BY m.MOV_ANAART
                       ORDER BY m.%DATA% DESC, m.MOV_PREACQ DESC
                       ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                   ) AS qta_cum_prec
            FROM U_MAG_MO m
            OUTER APPLY (
                SELECT TOP 1 v.VVA_CHANGE AS cambio
                FROM u_vva_ch v
                WHERE v.VVA_CODVAL = m.MOV_VALUTA
                  AND v.VVA_DATA <= m.%DATA%
                ORDER BY v.VVA_DATA DESC
            ) cb
            WHERE m.MOV_SEGNO = '+'
              AND m.MOV_CODMAG = :codmag
              AND m.%DATA% <= :dataRif
              AND (:articolo IS NULL OR m.MOV_ANAART = :articolo)
        ),
        strati AS (
            SELECT c.MOV_ANAART, c.prezzo_conv, c.cambio_mancante, g.giacenza,
                   CASE
                       WHEN g.giacenza - ISNULL(c.qta_cum_prec, 0) <= 0            THEN 0
                       WHEN g.giacenza - ISNULL(c.qta_cum_prec, 0) >= c.MOV_QTAMOV THEN c.MOV_QTAMOV
                       ELSE g.giacenza - ISNULL(c.qta_cum_prec, 0)
                   END AS qta_residua
            FROM carichi c
            JOIN giacenze g ON g.MOV_ANAART = c.MOV_ANAART
        ),
        strati_agg AS (
            SELECT MOV_ANAART,
                   SUM(qta_residua)               AS qta_valorizzata,
                   SUM(qta_residua * prezzo_conv) AS valore_giacenza,
                   MAX(cambio_mancante)           AS flag_cambio_mancante
            FROM strati WHERE qta_residua > 0 GROUP BY MOV_ANAART
        )
        SELECT
            g.MOV_ANAART                                    AS codice_articolo,
            g.giacenza                                      AS giacenza_alla_data,
            ISNULL(sa.qta_valorizzata, 0)                   AS qta_valorizzata,
            ISNULL(sa.valore_giacenza, 0)                   AS valore_giacenza,
            CAST(sa.valore_giacenza
                 / NULLIF(sa.qta_valorizzata, 0) AS decimal(18,4)) AS prezzo_medio_carico,
            ISNULL(sa.flag_cambio_mancante, 0)              AS flag_cambio_mancante,
            CASE
                WHEN g.giacenza < 0                                      THEN 'GIACENZA_NEGATIVA'
                WHEN g.giacenza > 0 AND ISNULL(sa.qta_valorizzata,0) = 0 THEN 'NON_VALORIZZATA'
                WHEN g.giacenza > ISNULL(sa.qta_valorizzata, 0)          THEN 'VALORIZZAZIONE_PARZIALE'
                WHEN ISNULL(sa.flag_cambio_mancante, 0) = 1              THEN 'CAMBIO_MANCANTE'
                ELSE 'OK'
            END                                             AS stato_valorizzazione
        FROM giacenze g
        LEFT JOIN strati_agg sa ON sa.MOV_ANAART = g.MOV_ANAART
        WHERE g.giacenza <> 0
        ORDER BY codice_articolo
        """;

    public List<ValorizzazioneRiga> valorizza(LocalDate dataRif, String codMag,
                                              String articolo, DateBase base) {
        String sql = SQL_VALORIZZA.replace("%DATA%", base.column());
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("codmag", codMag)
            .addValue("dataRif", LEGACY_DATE.format(dataRif))
            .addValue("articolo", blankToNull(articolo), Types.VARCHAR);
        long t0 = System.currentTimeMillis();
        List<ValorizzazioneRiga> out = jdbc.query(sql, p, (rs, i) -> new ValorizzazioneRiga(
            trim(rs.getString("codice_articolo")),
            rs.getBigDecimal("giacenza_alla_data"),
            rs.getBigDecimal("qta_valorizzata"),
            rs.getBigDecimal("valore_giacenza"),
            rs.getBigDecimal("prezzo_medio_carico"),
            rs.getInt("flag_cambio_mancante") == 1,
            rs.getString("stato_valorizzazione")
        ));
        log.info("Valorizzazione {} @{} base={} art={} → {} righe in {}ms",
                 codMag, dataRif, base, articolo, out.size(), System.currentTimeMillis() - t0);
        return out;
    }

    // ════════════════════════════════════════════════════════════════════
    //  §3.3 — Drill-down strati FIFO di un articolo
    // ════════════════════════════════════════════════════════════════════

    private static final String SQL_STRATI = """
        WITH giacenza AS (
            SELECT SUM(CASE m.MOV_SEGNO WHEN '+' THEN m.MOV_QTAMOV ELSE -m.MOV_QTAMOV END) AS giacenza
            FROM U_MAG_MO m
            WHERE m.MOV_CODMAG = :codmag AND m.MOV_ANAART = :articolo
              AND m.%DATA% <= :dataRif
        ),
        carichi AS (
            SELECT m.%DATA% AS data_carico,
                   m.MOV_QTAMOV, m.MOV_PREACQ, m.MOV_VALUTA,
                   cb.cambio,
                   m.MOV_PREACQ * ISNULL(cb.cambio, 1) AS prezzo_conv,
                   SUM(m.MOV_QTAMOV) OVER (
                       ORDER BY m.%DATA% DESC, m.MOV_PREACQ DESC
                       ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING) AS qta_cum_prec
            FROM U_MAG_MO m
            OUTER APPLY (SELECT TOP 1 v.VVA_CHANGE AS cambio FROM u_vva_ch v
                         WHERE v.VVA_CODVAL = m.MOV_VALUTA
                           AND v.VVA_DATA <= m.%DATA%
                         ORDER BY v.VVA_DATA DESC) cb
            WHERE m.MOV_SEGNO = '+' AND m.MOV_CODMAG = :codmag AND m.MOV_ANAART = :articolo
              AND m.%DATA% <= :dataRif
        )
        SELECT
            c.data_carico,
            c.MOV_QTAMOV       AS qta_carico,
            c.MOV_VALUTA       AS valuta,
            c.MOV_PREACQ       AS prezzo_orig,
            c.cambio           AS cambio_applicato,
            c.prezzo_conv      AS prezzo_conv,
            CASE
                WHEN g.giacenza - ISNULL(c.qta_cum_prec, 0) <= 0            THEN 0
                WHEN g.giacenza - ISNULL(c.qta_cum_prec, 0) >= c.MOV_QTAMOV THEN c.MOV_QTAMOV
                ELSE g.giacenza - ISNULL(c.qta_cum_prec, 0)
            END                AS qta_residua_in_giacenza
        FROM carichi c CROSS JOIN giacenza g
        ORDER BY c.data_carico DESC
        """;

    public List<StratoFifo> strati(LocalDate dataRif, String codMag,
                                   String articolo, DateBase base) {
        String sql = SQL_STRATI.replace("%DATA%", base.column());
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("codmag", codMag)
            .addValue("dataRif", LEGACY_DATE.format(dataRif))
            .addValue("articolo", articolo);
        return jdbc.query(sql, p, (rs, i) -> new StratoFifo(
            trim(rs.getString("data_carico")),
            rs.getBigDecimal("qta_carico"),
            trim(rs.getString("valuta")),
            rs.getBigDecimal("prezzo_orig"),
            rs.getBigDecimal("cambio_applicato"),
            rs.getBigDecimal("prezzo_conv"),
            rs.getBigDecimal("qta_residua_in_giacenza")
        ));
    }

    // ════════════════════════════════════════════════════════════════════
    //  §3.4 — Oscillazione prezzi 6 mesi + proiezione (+6 mesi)
    // ════════════════════════════════════════════════════════════════════
    //  NB: usa CAST(colonna AS date) per la regressione → richiede date
    //  bonificate (vedi §8 del documento). I filtri di finestra restano
    //  su stringa per SARGability.

    private static final String SQL_OSCILLAZIONE = """
        WITH base AS (
            SELECT
                m.MOV_ANAART,
                CAST(m.%DATA% AS date) AS data_mov,
                m.MOV_PREACQ * ISNULL(cb.cambio, 1) AS prezzo_conv
            FROM U_MAG_MO m
            OUTER APPLY (
                SELECT TOP 1 v.VVA_CHANGE AS cambio FROM u_vva_ch v
                WHERE v.VVA_CODVAL = m.MOV_VALUTA
                  AND v.VVA_DATA <= m.%DATA%
                ORDER BY v.VVA_DATA DESC
            ) cb
            WHERE m.MOV_SEGNO = '+'
              AND m.MOV_CODMAG = :codmag
              AND (:articolo IS NULL OR m.MOV_ANAART = :articolo)
              AND m.%DATA% >= :dataStart
              AND m.%DATA% <= :dataRif
        ),
        reg AS (
            SELECT
                MOV_ANAART,
                COUNT(*)           AS n_carichi,
                MIN(prezzo_conv)   AS prezzo_min,
                MAX(prezzo_conv)   AS prezzo_max,
                AVG(prezzo_conv)   AS prezzo_medio,
                STDEV(prezzo_conv) AS dev_std,
                SUM(CAST(DATEDIFF(day, :dataStartD, data_mov) AS float))               AS Sx,
                SUM(CAST(prezzo_conv AS float))                                        AS Sy,
                SUM(CAST(DATEDIFF(day, :dataStartD, data_mov) AS float)
                    * DATEDIFF(day, :dataStartD, data_mov))                            AS Sxx,
                SUM(CAST(DATEDIFF(day, :dataStartD, data_mov) AS float) * prezzo_conv) AS Sxy
            FROM base
            GROUP BY MOV_ANAART
        ),
        calc AS (
            SELECT r.*,
                CASE WHEN n_carichi >= 2 AND (n_carichi*Sxx - Sx*Sx) <> 0
                     THEN (n_carichi*Sxy - Sx*Sy) / (n_carichi*Sxx - Sx*Sx) END AS slope
            FROM reg r
        )
        SELECT
            MOV_ANAART                          AS codice_articolo,
            n_carichi,
            CAST(prezzo_min   AS decimal(18,4)) AS prezzo_min_6m,
            CAST(prezzo_max   AS decimal(18,4)) AS prezzo_max_6m,
            CAST(prezzo_medio AS decimal(18,4)) AS prezzo_medio_6m,
            CAST(dev_std      AS decimal(18,4)) AS dev_std_6m,
            CAST(CASE WHEN prezzo_medio > 0 THEN dev_std/prezzo_medio END AS decimal(18,4)) AS coeff_variazione,
            CAST(slope * 30.4375 AS decimal(18,4)) AS trend_mensile,
            CAST(CASE
                    WHEN slope IS NOT NULL
                    THEN ((Sy - slope*Sx)/n_carichi) + slope * :xFuturo
                    ELSE prezzo_medio
                 END AS decimal(18,4))          AS prezzo_proiettato_6m
        FROM calc
        ORDER BY codice_articolo
        """;

    public List<OscillazionePrezzo> oscillazionePrezzi(LocalDate dataRif, String codMag,
                                                       String articolo, DateBase base) {
        LocalDate dataStart = dataRif.minusMonths(6);
        long xFuturo = ChronoUnit.DAYS.between(dataStart, dataRif.plusMonths(6));

        String sql = SQL_OSCILLAZIONE.replace("%DATA%", base.column());
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("codmag", codMag)
            .addValue("articolo", blankToNull(articolo), Types.VARCHAR)
            .addValue("dataStart", LEGACY_DATE.format(dataStart))
            .addValue("dataRif", LEGACY_DATE.format(dataRif))
            .addValue("dataStartD", java.sql.Date.valueOf(dataStart))
            .addValue("xFuturo", (int) xFuturo);
        return jdbc.query(sql, p, (rs, i) -> new OscillazionePrezzo(
            trim(rs.getString("codice_articolo")),
            rs.getInt("n_carichi"),
            rs.getBigDecimal("prezzo_min_6m"),
            rs.getBigDecimal("prezzo_max_6m"),
            rs.getBigDecimal("prezzo_medio_6m"),
            rs.getBigDecimal("dev_std_6m"),
            rs.getBigDecimal("coeff_variazione"),
            rs.getBigDecimal("trend_mensile"),
            rs.getBigDecimal("prezzo_proiettato_6m")
        ));
    }

    // ════════════════════════════════════════════════════════════════════
    //  Lookup di supporto
    // ════════════════════════════════════════════════════════════════════

    public List<String> elencoMagazzini() {
        return jdbc.query(
            "SELECT DISTINCT MOV_CODMAG FROM U_MAG_MO WHERE MOV_CODMAG IS NOT NULL ORDER BY 1",
            (rs, i) -> trim(rs.getString(1)));
    }

    /** Autocomplete articolo: primi 20 codici che iniziano per il testo dato. */
    public List<String> cercaArticoli(String codMag, String prefix) {
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("codmag", codMag)
            .addValue("q", (prefix == null ? "" : prefix.trim()) + "%");
        return jdbc.query("""
            SELECT DISTINCT TOP 20 MOV_ANAART FROM U_MAG_MO
            WHERE MOV_CODMAG = :codmag AND MOV_ANAART LIKE :q
            ORDER BY MOV_ANAART
            """, p, (rs, i) -> trim(rs.getString(1)));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
