package com.aquarius.service;

import com.aquarius.context.FiscalContext;
import com.aquarius.repository.tenant.AccountRepository;
import com.aquarius.repository.tenant.AccountRepository.TreeRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Costruisce la rappresentazione dell'albero del piano dei conti replicando
 * la semantica ESATTA del VFP (form MENU_PDC000_TREEVIEW.scx).
 *
 * <h3>Come funziona la gerarchia in Aquarius (verificato nel sorgente VFP)</h3>
 * <p>Il piano dei conti è a codice a lunghezza fissa con segmenti posizionali.
 * Le posizioni di taglio sono configurate in {@code U_AZI_AN} (anagrafica
 * azienda) e caricate in APPLILIB come variabili globali:</p>
 * <pre>
 *   PUB_MASTRO = AZI_MASTRO     &amp;&amp; lunghezza segmento mastro
 *   IF PUB_MASTRO &lt;&gt; 0
 *      PUB_MASTRO = PUB_MASTRO + 1   &amp;&amp; → posizione di taglio
 *   ELSE
 *      PUB_MASTRO = 4                &amp;&amp; default: mastro a 3 char
 *   ENDIF
 *   (idem PUB_SOTTOG, default 6 → sottogruppo a 5 char)
 * </pre>
 * <p>Il VFP classifica poi con {@code SUBSTR(CON_CONTO, 1, PUB_MASTRO-1)}.
 * Esempio con mastro=3 e sottogruppo=5:</p>
 * <pre>
 *   "0010000000000"  → mastro       (zeri dopo posizione 3)
 *   "0010100000000"  → sottogruppo  (zeri dopo posizione 5)
 *   "0010100000042"  → conto        (foglia movimentabile)
 * </pre>
 *
 * <h3>Strategie di aggancio del padre (in ordine)</h3>
 * <ol>
 *   <li><b>Posizionale</b> (la regola VFP): conto → sottogruppo con stesso
 *       prefisso a 5 char; sottogruppo → mastro con stesso prefisso a 3 char</li>
 *   <li>{@code CON_CODPADRE} esplicito (se valorizzato e coerente)</li>
 *   <li>Pattern dot-separated ("01.01.001" → "01.01")</li>
 *   <li>Zero-strip euristico (ultima risorsa)</li>
 * </ol>
 *
 * <p>Output: payload flat (nodi + indice padre) pensato per il rendering
 * client-side del componente <code>aq-tree.js</code> — niente fragment
 * Thymeleaf ricorsivi, niente HTML da 7000 nodi generato server-side.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountTreeService {

    private final AccountRepository accountRepository;
    private final FiscalContext fiscalContext;

    /** Default VFP quando AZI_MASTRO / AZI_SOTTOG sono 0 o assenti. */
    static final int DEFAULT_MASTRO_LEN = 3;
    static final int DEFAULT_SOTTOG_LEN = 5;

    // ════════════════════════════════════════════════════════════════════
    //  API principale
    // ════════════════════════════════════════════════════════════════════

    /**
     * Costruisce il payload completo dell'albero per anno+società correnti.
     */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public TreePayload buildTreePayload() {
        if (!fiscalContext.isSet()) {
            log.warn("FiscalContext non settato — payload vuoto");
            return TreePayload.empty();
        }
        String year = fiscalContext.getFiscalYear();
        String society = fiscalContext.getSocietyCode();

        long t0 = System.currentTimeMillis();
        List<TreeRow> rows = accountRepository.findTreeRowsByYearAndSociety(year, society);
        long tQuery = System.currentTimeMillis() - t0;

        PdcStructure structure = loadStructure(society);
        return assemble(rows, structure, year, society, tQuery);
    }

    /**
     * Legge AZI_MASTRO / AZI_SOTTOG da U_AZI_AN applicando i default VFP.
     */
    PdcStructure loadStructure(String society) {
        try {
            List<Object[]> raw = accountRepository.findPdcStructureRaw(society);
            if (!raw.isEmpty() && raw.get(0) != null) {
                Object[] r = raw.get(0);
                int mastro = toInt(r.length > 0 ? r[0] : null);
                int sottog = toInt(r.length > 1 ? r[1] : null);
                if (mastro > 0 && sottog > mastro) {
                    return new PdcStructure(mastro, sottog, "U_AZI_AN");
                }
                if (mastro > 0 || sottog > 0) {
                    log.warn("U_AZI_AN soc={}: valori anomali AZI_MASTRO={} AZI_SOTTOG={} — uso default VFP",
                             society, mastro, sottog);
                }
            }
        } catch (Exception e) {
            log.warn("Lettura U_AZI_AN fallita ({}): uso default VFP 3/5", e.getMessage());
        }
        return new PdcStructure(DEFAULT_MASTRO_LEN, DEFAULT_SOTTOG_LEN, "default VFP");
    }

    private static int toInt(Object o) {
        return (o instanceof Number n) ? n.intValue() : 0;
    }

    // ════════════════════════════════════════════════════════════════════
    //  Assemblaggio albero
    // ════════════════════════════════════════════════════════════════════

    TreePayload assemble(List<TreeRow> rows, PdcStructure s,
                         String year, String society, long queryMillis) {
        long t0 = System.currentTimeMillis();
        int n = rows.size();

        String[] codes = new String[n];
        int[] levels = new int[n];
        int[] parents = new int[n];

        // Pass 1: normalizza codici, classifica il livello posizionale,
        //         indicizza mastri e sottogruppi per prefisso fisso.
        Map<String, Integer> mastriByPrefix = new HashMap<>();
        Map<String, Integer> sottogByPrefix = new HashMap<>();
        Map<String, Integer> byFullCode = new HashMap<>();

        for (int i = 0; i < n; i++) {
            String code = safe(rows.get(i).getCode());
            codes[i] = code;
            byFullCode.putIfAbsent(code, i);

            int prefixLen = nonZeroPrefixLen(code);
            int level = (prefixLen <= s.mastroLen()) ? 1
                       : (prefixLen <= s.sottogLen()) ? 2 : 3;
            levels[i] = level;

            if (level == 1) mastriByPrefix.putIfAbsent(cut(code, s.mastroLen()), i);
            if (level == 2) sottogByPrefix.putIfAbsent(cut(code, s.sottogLen()), i);
        }

        // Pass 2: risolvi il padre di ogni nodo (cascata di strategie).
        Map<String, Integer> strategies = new LinkedHashMap<>();
        strategies.put("posizionale", 0);
        strategies.put("conCodPadre", 0);
        strategies.put("dotPattern", 0);
        strategies.put("zeroStrip", 0);
        int rootsCount = 0;

        for (int i = 0; i < n; i++) {
            int p = resolveParent(i, rows.get(i), codes, levels, s,
                                  mastriByPrefix, sottogByPrefix, byFullCode, strategies);
            parents[i] = p;
            if (p < 0) rootsCount++;
        }

        // Pass 3: serializza in payload compatto [id, code, descr, tipo, posbil, level, parentIdx]
        List<Object[]> nodes = new ArrayList<>(n);
        int mastri = 0, sottogruppi = 0, conti = 0;
        for (int i = 0; i < n; i++) {
            TreeRow r = rows.get(i);
            switch (levels[i]) { case 1 -> mastri++; case 2 -> sottogruppi++; default -> conti++; }
            nodes.add(new Object[]{
                r.getId(), codes[i], safe(r.getDescription()),
                safe(r.getAccountType()), safe(r.getBalancePosition()),
                levels[i], parents[i]
            });
        }

        Map<String, Integer> levelCounts = new LinkedHashMap<>();
        levelCounts.put("mastri", mastri);
        levelCounts.put("sottogruppi", sottogruppi);
        levelCounts.put("conti", conti);

        long tBuild = System.currentTimeMillis() - t0;
        log.info("PDC tree: {} nodi (m={} s={} c={}), {} root | struttura {}+{} da {} | " +
                 "strategie {} | query {}ms, build {}ms",
                 n, mastri, sottogruppi, conti, rootsCount,
                 s.mastroLen(), s.sottogLen(), s.source(), strategies, queryMillis, tBuild);

        TreeMeta meta = new TreeMeta(year, society, n, s, levelCounts, strategies,
                                     rootsCount, queryMillis, tBuild);
        return new TreePayload(meta, nodes);
    }

    /**
     * Cascata di risoluzione del padre. Ritorna l'indice del padre o -1 (root).
     */
    private int resolveParent(int idx, TreeRow row, String[] codes, int[] levels,
                              PdcStructure s,
                              Map<String, Integer> mastriByPrefix,
                              Map<String, Integer> sottogByPrefix,
                              Map<String, Integer> byFullCode,
                              Map<String, Integer> strategies) {
        String code = codes[idx];
        int level = levels[idx];

        // (1) POSIZIONALE — la regola VFP: SUBSTR a lunghezza fissa
        if (level == 3) {
            Integer p = sottogByPrefix.get(cut(code, s.sottogLen()));
            if (p == null) p = mastriByPrefix.get(cut(code, s.mastroLen()));  // sottogruppo mancante
            if (p != null && p != idx) { bump(strategies, "posizionale"); return p; }
        } else if (level == 2) {
            Integer p = mastriByPrefix.get(cut(code, s.mastroLen()));
            if (p != null && p != idx) { bump(strategies, "posizionale"); return p; }
        } else {
            return -1;  // i mastri sono root per definizione
        }

        // (2) CON_CODPADRE esplicito
        String stored = safe(row.getParentCode());
        if (!stored.isEmpty()) {
            Integer p = byFullCode.get(stored);
            if (p != null && p != idx && nonZeroPrefixLen(codes[p]) < nonZeroPrefixLen(code)) {
                bump(strategies, "conCodPadre");
                return p;
            }
        }

        // (3) Dot-pattern
        int lastDot = code.lastIndexOf('.');
        if (lastDot > 0) {
            Integer p = byFullCode.get(code.substring(0, lastDot));
            if (p != null && p != idx) { bump(strategies, "dotPattern"); return p; }
        }

        // (4) Zero-strip euristico
        char[] chars = code.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] != '0') {
                char[] cand = chars.clone();
                for (int j = i; j < cand.length; j++) cand[j] = '0';
                String candidate = new String(cand);
                Integer p = byFullCode.get(candidate);
                if (p != null && p != idx) { bump(strategies, "zeroStrip"); return p; }
            }
        }

        return -1;
    }

    // ════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════

    /** Posizione dopo l'ultimo carattere non-zero. "0010000" → 3, "001042" → 6. */
    static int nonZeroPrefixLen(String code) {
        for (int i = code.length() - 1; i >= 0; i--) {
            if (code.charAt(i) != '0') return i + 1;
        }
        return 0;
    }

    /** Primi {@code len} caratteri, zero-padded a destra se il codice è più corto. */
    static String cut(String code, int len) {
        if (code.length() >= len) return code.substring(0, len);
        StringBuilder sb = new StringBuilder(len).append(code);
        while (sb.length() < len) sb.append('0');
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static void bump(Map<String, Integer> m, String k) {
        m.merge(k, 1, Integer::sum);
    }

    // ════════════════════════════════════════════════════════════════════
    //  DTO
    // ════════════════════════════════════════════════════════════════════

    /** Struttura segmenti del piano dei conti (da U_AZI_AN o default VFP). */
    public record PdcStructure(int mastroLen, int sottogLen, String source) {}

    public record TreeMeta(String year, String society, int total,
                           PdcStructure structure,
                           Map<String, Integer> levels,
                           Map<String, Integer> strategies,
                           int roots, long queryMillis, long buildMillis) {}

    /**
     * Payload per aq-tree.js. Ogni nodo è un array compatto:
     * {@code [id, code, descr, accountType, balancePosition, level, parentIdx]}
     * con parentIdx = indice del padre nell'array nodes, o -1 se root.
     */
    public record TreePayload(TreeMeta meta, List<Object[]> nodes) {
        static TreePayload empty() {
            return new TreePayload(
                new TreeMeta("", "", 0, new PdcStructure(0, 0, "n/a"),
                             Map.of(), Map.of(), 0, 0, 0),
                List.of());
        }
    }
}
