package com.aquarius.repository.tenant;

import com.aquarius.dto.documenti.DocumentType;
import com.aquarius.dto.documenti.DocumentoCollegato;
import com.aquarius.dto.documenti.DocumentoRiga;
import com.aquarius.dto.documenti.DocumentoTestata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO in SOLA LETTURA per il cruscotto "Ristampa documenti" — porting della
 * selezione archivio dinamica ({@code &_V_ARCH_TT}) del form VFP
 * {@code MENU_RISTAMPA_DOC}. Un solo code-path per tutti gli archivi del
 * catalogo {@link DocumentType}: le tabelle condividono i nomi colonna ORD_*.
 *
 * <h3>Scelte chiave (regole CLAUDE.md + precedenti nel repo)</h3>
 * <ul>
 *   <li><b>Identificatori controllati</b>: i nomi tabella entrano nel testo
 *       SQL SOLO da {@link DocumentType} (stesso argomento di sicurezza di
 *       {@code WarehouseValuationDao.DateBase}); l'input utente viaggia
 *       esclusivamente come parametro bindato.</li>
 *   <li><b>Descrizione causale</b>: join {@code PARA.CODICE = 'TOP' + ORD_CAUS}
 *       → {@code DESCRI} — identico alla query legacy del form e coerente con
 *       la regola gia' consolidata in primanota (tipo operazione da PARA).</li>
 *   <li><b>Date come stringhe</b> {@code yyyy/MM/dd}: confronto lessicografico
 *       bindato nello stesso formato (nessuna conversione di colonna).</li>
 *   <li><b>Paginazione</b>: {@code OFFSET/FETCH} (SQL Server 2012) + count
 *       separato con lo stesso WHERE.</li>
 *   <li><b>Tracciabilita'</b>: ordine ↔ DDT ↔ fattura ricostruita dai
 *       riferimenti denormalizzati legacy sulle righe DDT
 *       ({@code ORS_NUMORD/ORS_DATORD} e {@code ORS_NUMORC/ORS_DATORC} per
 *       l'ordine — il legacy porta ENTRAMBE le coppie, vengono interrogate
 *       entrambe — e {@code MOV_NUMFAT/MOV_DATFAT} per la fattura).</li>
 * </ul>
 */
@Repository
@Slf4j
public class DocumentArchiveDao {

    /** Colonne ordinabili: chiave UI → colonna SQL (whitelist, mai input utente). */
    public enum SortKey {
        NUMERO("ORD_NUMORD"),
        DATA("ORD_DATORD"),
        CODICE("ORD_CODCLI"),
        RAGSOC("ORD_RAGSOC"),
        CAUSALE("ORD_CAUS"),
        IMPONIBILE("ORD_IMPONIB");

        private final String column;
        SortKey(String column) { this.column = column; }
        public String column() { return column; }

        public static SortKey from(String s) {
            if (s == null) return NUMERO;
            try { return SortKey.valueOf(s.trim().toUpperCase()); }
            catch (IllegalArgumentException e) { return NUMERO; }
        }
    }

    private final NamedParameterJdbcTemplate jdbc;

    public DocumentArchiveDao(@Qualifier("tenantDataSource") DataSource tenantDataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(tenantDataSource);
    }

    // ════════════════════════════════════════════════════════════════════
    //  Ricerca unificata (lista del cruscotto)
    // ════════════════════════════════════════════════════════════════════

    /** Filtri della ricerca (tutti opzionali tranne soc/anno; stringhe gia' trimmate). */
    public record Filtri(String soc, String anno,
                         String numDa, String numA,
                         String dataDa, String dataA,
                         String soggetto,      // codice O ragione sociale (LIKE)
                         String riferimento,   // LIKE
                         String articolo) {}   // esatto, via subquery su DD (come il legacy)

    private String buildWhere(DocumentType tipo, Filtri f, MapSqlParameterSource p) {
        StringBuilder w = new StringBuilder("tt.ORD_CODSOC = :soc AND tt.ORD_ANNO = :anno");
        p.addValue("soc", f.soc()).addValue("anno", f.anno());
        if (notEmpty(f.numDa()))  { w.append(" AND tt.ORD_NUMORD >= :numDa");  p.addValue("numDa", f.numDa()); }
        if (notEmpty(f.numA()))   { w.append(" AND tt.ORD_NUMORD <= :numA");   p.addValue("numA", f.numA()); }
        if (notEmpty(f.dataDa())) { w.append(" AND tt.ORD_DATORD >= :dataDa"); p.addValue("dataDa", f.dataDa()); }
        if (notEmpty(f.dataA()))  { w.append(" AND tt.ORD_DATORD <= :dataA");  p.addValue("dataA", f.dataA()); }
        if (notEmpty(f.soggetto())) {
            w.append(" AND (tt.ORD_CODCLI LIKE :sogg OR LOWER(tt.ORD_RAGSOC) LIKE LOWER(:sogg))");
            p.addValue("sogg", "%" + f.soggetto() + "%");
        }
        if (notEmpty(f.riferimento())) {
            w.append(" AND tt.ORD_RIFERI LIKE :rif");
            p.addValue("rif", "%" + f.riferimento() + "%");
        }
        if (notEmpty(f.articolo())) {
            // come il legacy: taggancio IN (select daggancio from DD where ord_codart = ...)
            w.append(" AND tt.TAGGANCIO IN (SELECT dd.DAGGANCIO FROM ")
             .append(tipo.ddTable())
             .append(" dd WHERE dd.ORD_CODART = :art)");
            p.addValue("art", f.articolo());
        }
        if (tipo.extraWhere() != null) {
            w.append(" AND tt.").append(tipo.extraWhere());
        }
        return w.toString();
    }

    public long count(DocumentType tipo, Filtri f) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT COUNT(*) FROM " + tipo.ttTable() + " tt WHERE " + buildWhere(tipo, f, p);
        Long n = jdbc.queryForObject(sql, p, Long.class);
        return n == null ? 0 : n;
    }

    public List<DocumentoTestata> search(DocumentType tipo, Filtri f,
                                         SortKey sort, boolean asc,
                                         int page, int size) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = buildWhere(tipo, f, p);
        String sql =
            "SELECT tt.id_unique, tt.TAGGANCIO, tt.ORD_ANNO, tt.ORD_NUMORD, tt.ORD_DATORD, " +
            "       tt.ORD_CODCLI, tt.ORD_RAGSOC, tt.ORD_CAUS, " +
            "       COALESCE(pt.DESCRI, '') AS CAUS_DESCRI, " +
            "       tt.ORD_RIFERI, tt.ORD_AGE, COALESCE(pa.DESCRI, '') AS AGE_DESCRI, " +
            "       tt.ORD_IMPONIB, tt.ORD_IMPOSTA, tt.ORD_VALUTA " +
            "FROM " + tipo.ttTable() + " tt " +
            "LEFT OUTER JOIN PARA pt ON pt.CODICE = 'TOP' + tt.ORD_CAUS " +
            "LEFT OUTER JOIN PARA pa ON pa.CODICE = 'AGE' + tt.ORD_AGE " +
            "WHERE " + where + " " +
            "ORDER BY tt." + sort.column() + (asc ? " ASC" : " DESC") +
            // tie-breaker su ORD_NUMORD solo se non è già la colonna d'ordine
            // (altrimenti SQL Server: "colonna specificata più volte nell'ORDER BY")
            ("ORD_NUMORD".equals(sort.column()) ? " " : ", tt.ORD_NUMORD " + (asc ? "ASC" : "DESC") + " ") +
            "OFFSET :off ROWS FETCH NEXT :lim ROWS ONLY";
        p.addValue("off", page * size).addValue("lim", size);
        return jdbc.query(sql, p, testataMapper(tipo));
    }

    /** Testata singola per il dettaglio generico. */
    public DocumentoTestata findHead(DocumentType tipo, String id) {
        MapSqlParameterSource p = new MapSqlParameterSource("id", id);
        String sql =
            "SELECT tt.id_unique, tt.TAGGANCIO, tt.ORD_ANNO, tt.ORD_NUMORD, tt.ORD_DATORD, " +
            "       tt.ORD_CODCLI, tt.ORD_RAGSOC, tt.ORD_CAUS, " +
            "       COALESCE(pt.DESCRI, '') AS CAUS_DESCRI, " +
            "       tt.ORD_RIFERI, tt.ORD_AGE, COALESCE(pa.DESCRI, '') AS AGE_DESCRI, " +
            "       tt.ORD_IMPONIB, tt.ORD_IMPOSTA, tt.ORD_VALUTA " +
            "FROM " + tipo.ttTable() + " tt " +
            "LEFT OUTER JOIN PARA pt ON pt.CODICE = 'TOP' + tt.ORD_CAUS " +
            "LEFT OUTER JOIN PARA pa ON pa.CODICE = 'AGE' + tt.ORD_AGE " +
            "WHERE tt.id_unique = :id";
        List<DocumentoTestata> l = jdbc.query(sql, p, testataMapper(tipo));
        return l.isEmpty() ? null : l.get(0);
    }

    /** Righe di un documento via chiave di aggancio (TT.TAGGANCIO = DD.DAGGANCIO). */
    public List<DocumentoRiga> findRows(DocumentType tipo, String aggancio) {
        MapSqlParameterSource p = new MapSqlParameterSource("agg", aggancio);
        String sql =
            "SELECT dd.ORD_SEQUEN, dd.ORD_CODART, dd.ORD_MAGA, dd.ORD_DESART, dd.ORD_DES2, " +
            "       dd.ORD_QTAORD, dd.ORD_PRZNET, dd.ORD_VALORE, dd.ORD_IVA " +
            "FROM " + tipo.ddTable() + " dd " +
            "WHERE dd.DAGGANCIO = :agg " +
            "ORDER BY dd.ORD_SEQUEN ASC";
        return jdbc.query(sql, p, (rs, i) -> {
            DocumentoRiga r = new DocumentoRiga();
            r.setSequenza(rs.getBigDecimal("ORD_SEQUEN"));
            r.setCodiceArticolo(rs.getString("ORD_CODART"));
            r.setEstensione(rs.getString("ORD_MAGA"));
            r.setDescrizione(rs.getString("ORD_DESART"));
            r.setDescrizione2(rs.getString("ORD_DES2"));
            r.setQuantita(rs.getBigDecimal("ORD_QTAORD"));
            r.setPrezzoNetto(rs.getBigDecimal("ORD_PRZNET"));
            r.setValoreRiga(rs.getBigDecimal("ORD_VALORE"));
            r.setCodiceIva(rs.getString("ORD_IVA"));
            return r;
        });
    }

    // ════════════════════════════════════════════════════════════════════
    //  Tracciabilita' ordine ↔ DDT ↔ fattura
    //  Verificata SOLO per la catena vendite (ORD/BOL/FAT): i riferimenti
    //  vivono sulle righe DDT (U_BOL_DD). Per gli altri tipi non ci sono
    //  colonne di legame verificate nei sorgenti → nessuna inferenza.
    // ════════════════════════════════════════════════════════════════════

    /** DDT e fatture collegati a un ORDINE (numero+data). */
    public List<DocumentoCollegato> linkedForOrder(String numOrd, String datOrd) {
        List<DocumentoCollegato> out = new ArrayList<>();
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("n", numOrd).addValue("d", datOrd);
        // DDT: sulle righe bolla il riferimento all'ordine CLIENTE e' la coppia
        // ORS_NUMORC/ORS_DATORC ("ORdine Cliente" — evidenza: match legacy con
        // U_ORD_T2.ORD_NUMORD); ORS_NUMORD/ORS_DATORD sono l'auto-riferimento
        // alla testata bolla e NON vanno usati qui.
        out.addAll(jdbc.query(
            "SELECT DISTINCT tt.id_unique, tt.ORD_NUMORD, tt.ORD_DATORD " +
            "FROM U_BOL_DD dd JOIN U_BOL_TT tt ON tt.TAGGANCIO = dd.DAGGANCIO " +
            "WHERE dd.ORS_NUMORC = :n AND dd.ORS_DATORC = :d",
            p, (rs, i) -> new DocumentoCollegato(DocumentType.BOL,
                rs.getString("ORD_NUMORD"), rs.getString("ORD_DATORD"),
                rs.getString("id_unique"))));
        // Fatture: dai riferimenti fattura sulle stesse righe bolla
        out.addAll(jdbc.query(
            "SELECT DISTINCT dd.MOV_NUMFAT, dd.MOV_DATFAT " +
            "FROM U_BOL_DD dd " +
            "WHERE dd.ORS_NUMORC = :n AND dd.ORS_DATORC = :d " +
            "  AND dd.MOV_NUMFAT IS NOT NULL AND LTRIM(RTRIM(dd.MOV_NUMFAT)) <> ''",
            p, (rs, i) -> new DocumentoCollegato(DocumentType.FAT,
                rs.getString("MOV_NUMFAT"), rs.getString("MOV_DATFAT"), null)));
        resolveInvoiceIds(out);
        return dedupe(out);
    }

    /** Ordini e fatture collegati a un DDT (via le sue righe). */
    public List<DocumentoCollegato> linkedForDdt(String aggancio) {
        List<DocumentoCollegato> out = new ArrayList<>();
        MapSqlParameterSource p = new MapSqlParameterSource("agg", aggancio);
        out.addAll(jdbc.query(
            "SELECT DISTINCT dd.ORS_NUMORC AS N, dd.ORS_DATORC AS D FROM U_BOL_DD dd " +
            "WHERE dd.DAGGANCIO = :agg AND dd.ORS_NUMORC IS NOT NULL AND LTRIM(RTRIM(dd.ORS_NUMORC)) <> ''",
            p, (rs, i) -> new DocumentoCollegato(DocumentType.ORD,
                rs.getString("N"), rs.getString("D"), null)));
        out.addAll(jdbc.query(
            "SELECT DISTINCT dd.MOV_NUMFAT, dd.MOV_DATFAT FROM U_BOL_DD dd " +
            "WHERE dd.DAGGANCIO = :agg AND dd.MOV_NUMFAT IS NOT NULL AND LTRIM(RTRIM(dd.MOV_NUMFAT)) <> ''",
            p, (rs, i) -> new DocumentoCollegato(DocumentType.FAT,
                rs.getString("MOV_NUMFAT"), rs.getString("MOV_DATFAT"), null)));
        resolveOrderIds(out);
        resolveInvoiceIds(out);
        return dedupe(out);
    }

    /** DDT e ordini collegati a una FATTURA (numero+data, via righe DDT). */
    public List<DocumentoCollegato> linkedForInvoice(String numFat, String datFat) {
        List<DocumentoCollegato> out = new ArrayList<>();
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("n", numFat).addValue("d", datFat);
        out.addAll(jdbc.query(
            "SELECT DISTINCT tt.id_unique, tt.ORD_NUMORD, tt.ORD_DATORD " +
            "FROM U_BOL_DD dd JOIN U_BOL_TT tt ON tt.TAGGANCIO = dd.DAGGANCIO " +
            "WHERE dd.MOV_NUMFAT = :n AND dd.MOV_DATFAT = :d",
            p, (rs, i) -> new DocumentoCollegato(DocumentType.BOL,
                rs.getString("ORD_NUMORD"), rs.getString("ORD_DATORD"),
                rs.getString("id_unique"))));
        out.addAll(jdbc.query(
            "SELECT DISTINCT dd.ORS_NUMORC AS N, dd.ORS_DATORC AS D FROM U_BOL_DD dd " +
            "WHERE dd.MOV_NUMFAT = :n AND dd.MOV_DATFAT = :d " +
            "  AND dd.ORS_NUMORC IS NOT NULL AND LTRIM(RTRIM(dd.ORS_NUMORC)) <> ''",
            p, (rs, i) -> new DocumentoCollegato(DocumentType.ORD,
                rs.getString("N"), rs.getString("D"), null)));
        resolveOrderIds(out);
        return dedupe(out);
    }

    private void resolveOrderIds(List<DocumentoCollegato> docs) {
        for (DocumentoCollegato d : docs) {
            if (d.getTipo() == DocumentType.ORD && d.getId() == null && d.getNumero() != null) {
                List<String> ids = jdbc.query(
                    "SELECT TOP 1 id_unique FROM U_ORD_TT WHERE ORD_NUMORD = :n AND ORD_DATORD = :d",
                    new MapSqlParameterSource().addValue("n", d.getNumero()).addValue("d", d.getData()),
                    (rs, i) -> rs.getString(1));
                if (!ids.isEmpty()) d.setId(ids.get(0));
            }
        }
    }

    private void resolveInvoiceIds(List<DocumentoCollegato> docs) {
        for (DocumentoCollegato d : docs) {
            if (d.getTipo() == DocumentType.FAT && d.getId() == null && d.getNumero() != null) {
                List<String> ids = jdbc.query(
                    "SELECT TOP 1 id_unique FROM U_FAT_TT WHERE ORD_NUMORD = :n AND ORD_DATORD = :d",
                    new MapSqlParameterSource().addValue("n", d.getNumero()).addValue("d", d.getData()),
                    (rs, i) -> rs.getString(1));
                if (!ids.isEmpty()) d.setId(ids.get(0));
            }
        }
    }

    /** Deduplica per tipo+numero+data preservando l'ordine (id risolto se disponibile). */
    private List<DocumentoCollegato> dedupe(List<DocumentoCollegato> docs) {
        Map<String, DocumentoCollegato> seen = new LinkedHashMap<>();
        for (DocumentoCollegato d : docs) {
            String key = d.getTipo() + "|" +
                (d.getNumero() == null ? "" : d.getNumero().trim()) + "|" +
                (d.getData() == null ? "" : d.getData().trim());
            DocumentoCollegato prev = seen.get(key);
            if (prev == null || (prev.getId() == null && d.getId() != null)) {
                seen.put(key, d);
            }
        }
        return new ArrayList<>(seen.values());
    }

    private RowMapper<DocumentoTestata> testataMapper(DocumentType tipo) {
        return (rs, i) -> {
            DocumentoTestata t = new DocumentoTestata();
            t.setTipo(tipo);
            t.setId(rs.getString("id_unique"));
            t.setAggancio(rs.getString("TAGGANCIO"));
            t.setAnno(rs.getString("ORD_ANNO"));
            t.setNumero(rs.getString("ORD_NUMORD"));
            t.setData(rs.getString("ORD_DATORD"));
            t.setCodiceSoggetto(rs.getString("ORD_CODCLI"));
            t.setRagioneSociale(rs.getString("ORD_RAGSOC"));
            t.setCausale(rs.getString("ORD_CAUS"));
            t.setCausaleDescrizione(rs.getString("CAUS_DESCRI"));
            t.setRiferimento(rs.getString("ORD_RIFERI"));
            t.setAgente(rs.getString("ORD_AGE"));
            t.setAgenteDescrizione(rs.getString("AGE_DESCRI"));
            t.setImponibile(rs.getBigDecimal("ORD_IMPONIB"));
            t.setImposta(rs.getBigDecimal("ORD_IMPOSTA"));
            t.setValuta(rs.getString("ORD_VALUTA"));
            return t;
        };
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
