package com.aquarius.service;

import com.aquarius.context.FiscalContext;
import com.aquarius.entity.tenant.Account;
import com.aquarius.repository.tenant.AccountRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Costruisce la rappresentazione ad albero del piano dei conti partendo
 * dalla lista flat in {@code CONTI}, filtrata sull'anno contabile + società
 * della sessione corrente (vedi {@link FiscalContext}).
 *
 * <p>Equivalente moderno della logica VFP che usa la variabile globale
 * {@code PUB_ANNO}: ogni vista contabile parte da una WHERE
 * {@code CON_ANNO = :year AND CON_SOC = :society}.</p>
 *
 * <p>Per piani molto grandi (>5000 conti) considerare lazy loading via
 * {@link AccountRepository#findByParentCodeByYearAndSociety} con AJAX.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountTreeService {

    private final AccountRepository accountRepository;
    private final FiscalContext fiscalContext;

    /**
     * Costruisce l'albero del piano dei conti per l'anno contabile + società
     * correnti (dalla sessione utente).
     */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<AccountNode> buildTree() {
        if (!fiscalContext.isSet()) {
            log.warn("FiscalContext non settato — ritorno albero vuoto");
            return List.of();
        }
        List<Account> all = accountRepository.findAllForTreeByYearAndSociety(
            fiscalContext.getFiscalYear(),
            fiscalContext.getSocietyCode()
        );
        log.debug("Tree piano conti: anno={} società={} -> {} conti caricati",
                  fiscalContext.getFiscalYear(), fiscalContext.getSocietyCode(), all.size());
        return buildTreeFromList(all);
    }

    /**
     * Variante che parte da una lista già caricata (utile per test o quando
     * il chiamante vuole filtrare i conti prima della costruzione).
     *
     * <p>Strategia di ricostruzione della gerarchia, in ordine di priorità:</p>
     * <ol>
     *   <li>{@code CON_CODPADRE} se valorizzato e il nodo padre esiste in lista</li>
     *   <li>Pattern punto-separato del codice: {@code "01.01.001"} → padre
     *       {@code "01.01"} (tipico nei piani conti italiani)</li>
     *   <li>Altrimenti il conto è considerato root</li>
     * </ol>
     */
    public List<AccountNode> buildTreeFromList(List<Account> accounts) {
        // Indice per codice → nodo (per lookup veloce del padre)
        Map<String, AccountNode> byCode = new HashMap<>();
        List<AccountNode> roots = new ArrayList<>();

        // Pass 1: crea tutti i nodi e mette in indice.
        for (Account a : accounts) {
            if (a.getCode() == null || a.getCode().isBlank()) continue;
            String key = a.getCode().trim();
            byCode.computeIfAbsent(key, k -> new AccountNode(a, new ArrayList<>()));
        }

        // Log diagnostico: campione di codici reali
        if (log.isInfoEnabled() && !byCode.isEmpty()) {
            List<String> sample = byCode.keySet().stream().sorted().limit(10).toList();
            log.info("Campione codici (primi 10): {}", sample);
        }

        // Pass 2: aggancia ogni nodo al suo padre.
        int byParentCodeField = 0;
        int byDotPattern = 0;
        int byZeroStrip = 0;
        int asRoot = 0;
        int orphans = 0;

        for (AccountNode node : byCode.values()) {
            ParentResult res = resolveParentCode(node.getAccount(), byCode);

            if (res.parentCode == null) {
                roots.add(node);
                asRoot++;
            } else {
                AccountNode parent = byCode.get(res.parentCode);
                if (parent != null) {
                    parent.getChildren().add(node);
                    switch (res.strategy) {
                        case PARENT_FIELD -> byParentCodeField++;
                        case DOT_PATTERN  -> byDotPattern++;
                        case ZERO_STRIP   -> byZeroStrip++;
                    }
                } else {
                    roots.add(node);
                    orphans++;
                }
            }
        }

        // Pass 3: ordina per codice ogni livello
        Comparator<AccountNode> byCodeAsc =
            Comparator.comparing(n -> n.getAccount().getCode());
        roots.sort(byCodeAsc);
        for (AccountNode n : byCode.values()) {
            n.getChildren().sort(byCodeAsc);
        }

        log.info("Tree piano conti: {} totali → {} root ({} orfani), " +
                 "agganciati: {} via CON_CODPADRE + {} via dot-pattern + {} via zero-strip",
                 accounts.size(), roots.size(), orphans,
                 byParentCodeField, byDotPattern, byZeroStrip);
        return roots;
    }

    /**
     * Risolve il codice del padre del conto con strategia mista a 3 fallback.
     *
     * @return record con codice del padre + strategia usata, o NONE se root.
     */
    private ParentResult resolveParentCode(Account a, Map<String, AccountNode> byCode) {
        // (1) CON_CODPADRE se valorizzato e il padre esiste in lista
        String stored = a.getParentCode();
        if (stored != null && !stored.trim().isEmpty()) {
            String cleaned = stored.trim();
            if (byCode.containsKey(cleaned)) return new ParentResult(cleaned, Strategy.PARENT_FIELD);
        }

        String code = a.getCode();
        if (code == null) return new ParentResult(null, null);
        String trimmed = code.trim();

        // (2) Pattern dot-separated: "01.01.001" → "01.01"
        int lastDot = trimmed.lastIndexOf('.');
        if (lastDot > 0) {
            String candidate = trimmed.substring(0, lastDot);
            if (byCode.containsKey(candidate)) return new ParentResult(candidate, Strategy.DOT_PATTERN);
        }

        // (3) Trailing-zero strip: per codici a lunghezza fissa con padding zeri
        // Es: "01010001" → cerca "01010000" → trovato? è padre.
        //     "01010000" → cerca "01000000" → trovato? è padre.
        //     "01000000" → cerca "00000000" → quasi mai trovato → root
        String zsParent = findParentByZeroStrip(trimmed, byCode);
        if (zsParent != null) return new ParentResult(zsParent, Strategy.ZERO_STRIP);

        // (4) Root
        return new ParentResult(null, null);
    }

    /**
     * Per codici a lunghezza fissa con padding di zeri, trova il padre
     * "azzerando" da destra l'ultima cifra non-zero e cercando il risultato
     * nell'indice. Itera azzerando cifre fino a trovare un match o esaurire
     * le cifre.
     *
     * <p>Esempio (codice 8 char):</p>
     * <pre>
     *   "01010002" → prova "01010000" → trovato!  ritorna "01010000"
     *   "01010000" → prova "01000000" → trovato!  ritorna "01000000"
     *   "01000000" → prova "00000000" → non trovato → ritorna null
     * </pre>
     */
    private String findParentByZeroStrip(String code, Map<String, AccountNode> byCode) {
        if (code == null || code.isEmpty()) return null;
        char[] chars = code.toCharArray();
        // Posizioni delle cifre non-zero (da destra a sinistra)
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] != '0') {
                // Azzera questa cifra e cerca il risultato
                char[] candidate = chars.clone();
                candidate[i] = '0';
                // Azzera anche tutto ciò che sta a destra (per sicurezza)
                for (int j = i + 1; j < candidate.length; j++) candidate[j] = '0';
                String candStr = new String(candidate);
                if (!candStr.equals(code) && byCode.containsKey(candStr)) {
                    return candStr;
                }
            }
        }
        return null;
    }

    /** Strategia usata per risolvere il padre (per logging diagnostico). */
    private enum Strategy { PARENT_FIELD, DOT_PATTERN, ZERO_STRIP }

    /** Tuple parent-code + strategia. */
    private record ParentResult(String parentCode, Strategy strategy) {}

    /**
     * Conta totale nodi nell'albero (utile per UI).
     */
    public int countNodes(List<AccountNode> roots) {
        int n = 0;
        for (AccountNode r : roots) n += 1 + countNodes(r.getChildren());
        return n;
    }

    /**
     * Singolo nodo dell'albero. Contiene l'entity {@link Account} e la lista
     * dei figli (ricorsivamente).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountNode {
        private Account account;
        private List<AccountNode> children;

        public boolean hasChildren() {
            return children != null && !children.isEmpty();
        }

        /** ID DOM unico, per Bootstrap collapse. */
        public String domId() {
            return "acc-" + account.getCode().replace('.', '-').replace(' ', '-');
        }
    }
}
