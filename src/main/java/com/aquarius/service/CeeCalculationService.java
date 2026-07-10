package com.aquarius.service;

import com.aquarius.repository.tenant.CeeStructureDao;
import com.aquarius.repository.tenant.CeeStructureDao.CeeValueRow;
import com.aquarius.repository.tenant.CeeStructureDao.ConfluenzaTotale;
import com.aquarius.repository.tenant.CeeStructureDao.ContoMappato;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motore di calcolo del Bilancio CEE — replica fedele di {@code ceecont.PRG}
 * ("CALCOLO VALORI BILANCIO CEE"), sulla base dell'analisi in
 * resources/cee/README.md. Scrive i risultati in BILNEW.CORRENTE (tabella
 * volatile, azzerata a ogni ricalcolo). Il calcolo è in memoria e scritto in
 * blocco alla fine: in caso di errore bloccante (riga di confluenza mancante) NON
 * viene scritto nulla ("aggiornamento annullato", come il legacy).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CeeCalculationService {

    private static final long ECONOMIC_THRESHOLD = 21600;   // VAL(codrig) >= 21600 → economico

    private final CeeStructureDao dao;

    /** Esito del ricalcolo. */
    public static class Result {
        public boolean ok;
        public String error;                       // valorizzato se ok=false (abort)
        public int righeAggiornate;
        public final List<String> warnings = new ArrayList<>();
    }

    /** Nodo di lavoro (una riga BILNEW). */
    private static class Node {
        final String codeStored;   // BIL_CODRIG come letto (trim) → per la UPDATE
        final long codeNum;        // valore numerico (per soglia economico), Long.MIN se n.d.
        BigDecimal corrente = BigDecimal.ZERO;
        Node(String codeStored, long codeNum) { this.codeStored = codeStored; this.codeNum = codeNum; }
    }

    public Result recalcola(String soc, String anno, boolean previsionali) {
        Result res = new Result();

        // BILNEW: nodi per codice normalizzato (numerico) → confluenze/lookup robusti
        Map<String, Node> byKey = new LinkedHashMap<>();
        for (CeeValueRow r : dao.values(soc)) {
            String stored = r.getCodRiga() == null ? "" : r.getCodRiga().trim();
            long num = parseNum(stored);
            byKey.put(normKey(stored), new Node(stored, num));
        }
        if (byKey.isEmpty()) {
            res.error = "Struttura del bilancio CEE (BILNEW) non definita per questa società.";
            return res;
        }

        Map<String, BigDecimal> saldi = dao.accountBalances(soc, anno, previsionali);

        // FASE 1+2 — assegnazione saldi conti alle voci
        for (ContoMappato m : dao.mappings(soc, anno)) {
            String conto = m.getConto() == null ? "" : m.getConto().trim();
            BigDecimal saldo = saldi.get(conto);
            if (saldo == null) {                       // conto non presente in CONTI
                res.warnings.add("Conto " + conto + " non presente negli archivi contabili: saltato.");
                continue;
            }
            if (saldo.signum() == 0) continue;         // nessun contributo
            String rigaDare  = trim(m.getRigaDare());
            String rigaAvere = trim(m.getRigaAvere());
            // Se il saldo è negativo e la riga avere è definita, confluisce lì (es. "banche in passivo")
            String dest = (saldo.signum() < 0 && !rigaAvere.isEmpty()) ? rigaAvere : rigaDare;
            Node n = byKey.get(normKey(dest));
            if (n == null) {
                res.warnings.add("Conto " + conto + ": riga di destinazione '" + dest
                    + "' non presente in BILNEW — confluenza non assegnata, conto saltato.");
                continue;
            }
            n.corrente = n.corrente.add(saldo);
        }

        // FASE 2b — valore assoluto sulle voci economiche (>= 21600)
        for (Node n : byKey.values()) {
            if (n.codeNum >= ECONOMIC_THRESHOLD) n.corrente = n.corrente.abs();
        }

        // FASE 3 — totali dalle confluenze (U_COR_TT), nell'ordine legacy (COR_RIGA)
        for (ConfluenzaTotale e : dao.totalEdges(soc)) {
            Node src = byKey.get(normKey(trim(e.getRiga())));
            BigDecimal v = src != null ? src.corrente : BigDecimal.ZERO;
            BigDecimal delta = "+".equals(trim(e.getSegno())) ? v : v.negate();
            Node dst = byKey.get(normKey(trim(e.getTotale())));
            if (dst == null) {
                // Errore bloccante come nel legacy: nulla viene scritto.
                res.error = "Riga di confluenza '" + trim(e.getTotale())
                    + "' non trovata in BILNEW: aggiornamento annullato.";
                return res;
            }
            dst.corrente = dst.corrente.add(delta);
        }

        // Scrittura in blocco (BILNEW volatile): imposta CORRENTE per ogni riga.
        Map<String, BigDecimal> toWrite = new LinkedHashMap<>();
        for (Node n : byKey.values()) toWrite.put(n.codeStored, n.corrente);
        dao.updateCorrente(soc, toWrite);

        res.ok = true;
        res.righeAggiornate = toWrite.size();
        log.info("Ricalcolo CEE soc={} anno={}: {} righe, {} warning",
                 soc, anno, res.righeAggiornate, res.warnings.size());
        return res;
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    /** Chiave normalizzata: se numerica → senza zeri iniziali; altrimenti trim. */
    private static String normKey(String s) {
        String t = trim(s);
        long n = parseNum(t);
        return n != Long.MIN_VALUE ? Long.toString(n) : t;
    }

    private static long parseNum(String t) {
        try { return Long.parseLong(t.trim()); }
        catch (Exception e) { return Long.MIN_VALUE; }
    }
}
