package com.aquarius.repository.tenant;

import com.aquarius.dto.magazzino.GiacenzaRiga;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

/**
 * DAO in SOLA LETTURA per le giacenze correnti ({@code U_MAG_GG}).
 * PITFALL CLAUDE.md: la tabella ha PIU' righe con segno per articolo →
 * SEMPRE pre-aggregata: {@code SUM(MAG_GIACEN) GROUP BY MAG_ANAART,
 * MAG_CODMAG} (l'aggregazione verbatim dei PRG legacy). Le righe
 * "fantasma" (totale zero, storico presente) sono ESPOSTE con
 * {@code rowCount}, non nascoste.
 *
 * SQL nativo (pattern WarehouseValuationDao/DocumentArchiveDao) perche'
 * il conteggio di gruppi con HAVING richiede una derived table, non
 * esprimibile in JPQL/Hibernate 5. Le colonne di ordinamento entrano nel
 * testo SQL SOLO dall'enum {@link SortKey} (identificatori controllati).
 */
@Repository
@Slf4j
public class StockBalanceDao {

    /** Colonne ordinabili: chiave UI → espressione SQL (whitelist). */
    public enum SortKey {
        ARTICOLO("MAG_ANAART"),
        MAGAZZINO("MAG_CODMAG"),
        GIACENZA("SUM(MAG_GIACEN)");

        private final String expr;
        SortKey(String expr) { this.expr = expr; }
        public String expr() { return expr; }

        public static SortKey from(String s) {
            if (s == null) return ARTICOLO;
            try { return SortKey.valueOf(s.trim().toUpperCase()); }
            catch (IllegalArgumentException e) { return ARTICOLO; }
        }
    }

    private final NamedParameterJdbcTemplate jdbc;

    public StockBalanceDao(@Qualifier("tenantDataSource") DataSource tenantDataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(tenantDataSource);
    }

    private static final String WHERE = """
        MAG_CODSOC = :soc
        AND (:q IS NULL OR :q = ''
             OR LOWER(MAG_ANAART) LIKE LOWER(:qLike)
             OR LOWER(MAG_DESART) LIKE LOWER(:qLike)
             OR LOWER(MAG_CODMAG) LIKE LOWER(:qLike))
        """;

    private static final String HAVING =
        "HAVING (:soloNonZero = 0 OR SUM(MAG_GIACEN) <> 0) ";

    private MapSqlParameterSource params(String soc, String q, boolean soloNonZero) {
        return new MapSqlParameterSource()
            .addValue("soc", soc)
            .addValue("q", q)
            .addValue("qLike", "%" + (q == null ? "" : q) + "%")
            .addValue("soloNonZero", soloNonZero ? 1 : 0);
    }

    public long count(String soc, String q, boolean soloNonZero) {
        String sql = "SELECT COUNT(*) FROM ("
            + "SELECT MAG_ANAART FROM U_MAG_GG WHERE " + WHERE
            + "GROUP BY MAG_ANAART, MAG_CODMAG " + HAVING
            + ") x";
        Long n = jdbc.queryForObject(sql, params(soc, q, soloNonZero), Long.class);
        return n == null ? 0 : n;
    }

    public List<GiacenzaRiga> aggregate(String soc, String q, boolean soloNonZero,
                                        SortKey sort, boolean asc, int page, int size) {
        String sql = "SELECT MAG_ANAART, MAX(MAG_DESART) AS DESART, MAG_CODMAG, "
            + "SUM(MAG_GIACEN) AS GIACEN, COUNT(*) AS NRIGHE "
            + "FROM U_MAG_GG WHERE " + WHERE
            + "GROUP BY MAG_ANAART, MAG_CODMAG " + HAVING
            + "ORDER BY " + sort.expr() + (asc ? " ASC" : " DESC") + ", MAG_ANAART ASC "
            + "OFFSET :off ROWS FETCH NEXT :lim ROWS ONLY";
        MapSqlParameterSource p = params(soc, q, soloNonZero)
            .addValue("off", page * size).addValue("lim", size);
        return jdbc.query(sql, p, (rs, i) -> new GiacenzaRiga(
            rs.getString("MAG_ANAART"),
            rs.getString("DESART"),
            rs.getString("MAG_CODMAG"),
            rs.getBigDecimal("GIACEN"),
            rs.getLong("NRIGHE")));
    }
}
