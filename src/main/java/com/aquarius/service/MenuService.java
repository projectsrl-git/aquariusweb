package com.aquarius.service;

import com.aquarius.dto.MenuNode;
import com.aquarius.entity.tenant.LegacyMenu;
import com.aquarius.repository.tenant.LegacyMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Costruisce l'albero del menu Aquarius leggendo {@code tbl_menu} legacy.
 *
 * Replica fedele dell'algoritmo VFP di {@code CREA_WINMAIN} in
 * {@code prg/WINMAIN_LIB.PRG}, che usa tre cursori basati sulla colonna
 * {@code LIVELLO_MENU}:
 *
 * <pre>
 *  LIVELLO_MENU=1  →  TOP-LEVEL (menubar)        es. "Clienti", "Fornitori"
 *  LIVELLO_MENU=2  →  popup primario             es. "Anagrafica" sotto Clienti
 *  LIVELLO_MENU=0  →  foglie di sub-submenu      es. "Elenco note anagrafiche"
 *                                                in MENU='gestioneclienti'
 * </pre>
 *
 * <h3>Chain di collegamento</h3>
 *
 * <ul>
 *   <li>L1 con (MENU='clienti', LABEL='Clienti', LIVELLO=1) → menubar pad</li>
 *   <li>L2 con (MENU='clienti', LABEL='Anagrafica', LIVELLO=2,
 *       COMANDO=&quot;do form form\submenu name M1 with 'GESTIONECLIENTI'&quot;) →
 *       voce sotto "Clienti" che apre il sub-submenu</li>
 *   <li>L0 con (MENU='gestioneclienti', LABEL='Aggiornamento', LIVELLO=0,
 *       COMANDO=&quot;do form form\menu_cli000 linked&quot;, UTENTI='.SER.AMB.') →
 *       foglia concreta sotto "Anagrafica"</li>
 * </ul>
 *
 * <h3>Algoritmo (replica di CREA_WINMAIN)</h3>
 *
 * <ol>
 *   <li>Carica le foglie L0 visibili all'operatore (filtro DB-side
 *       {@code UTENTI LIKE '%.code.%'})</li>
 *   <li>Carica tutti i record L1 (top-level)</li>
 *   <li>Carica tutti i record L2 (popup primari)</li>
 *   <li>Per ogni foglia L0 con MENU='X', cerca la voce L2 il cui COMANDO contiene
 *       "'X'" (case-insensitive): è il submenu padre → marca visibile</li>
 *   <li>Per ogni voce L2 di tipo "do form" diretto (cioè COMANDO che NON contiene
 *       "submenu"), marca visibile incondizionatamente (replica esatta VFP)</li>
 *   <li>Per ogni L2 visibile, marca visibile la L1 con lo stesso MENU</li>
 *   <li>Render: per ogni L1 visibile, costruisci l'albero coi figli L2 visibili,
 *       e per ogni L2 di tipo submenu allega le sue foglie L0</li>
 *   <li>Pulizia separator consecutivi/leading/trailing</li>
 * </ol>
 *
 * Cache server-side per {@code (tenant, operatorCode)}: dopo il primo build,
 * le richieste seguenti sono O(1) lookup in mappa.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final LegacyMenuRepository menuRepository;

    /**
     * Mappa form-VFP → URL della web app. Si arricchisce ad ogni slice di porting.
     * I form non in mappa hanno url=null → frontend li mostra come "(da migrare)".
     */
    private static final Map<String, String> FORM_TO_URL = Map.ofEntries(
        // Slice 3: Anagrafica clienti
        Map.entry("menu_cli000", "/clienti"),
        // Slice: Anagrafica fornitori
        Map.entry("menu_for000", "/fornitori"),
        // Slice 4: Piano dei conti
        Map.entry("menu_pdc000",          "/conti/tree"),
        Map.entry("menu_pdc000_treeview", "/conti/tree"),
        Map.entry("menu_pdcelen",         "/conti"),
        Map.entry("menu_pdcelcco",        "/conti"),
        // Slice: Contabilità (consultazione)
        Map.entry("menu_primanot",  "/contabilita/primanota"),
        Map.entry("menu_primanot2", "/contabilita/primanota"),
        Map.entry("viewpartitario", "/contabilita/partitari/clienti"),
        Map.entry("partitario_soprad", "/contabilita/partitari/clienti"),
        // Slice: Anagrafica articoli (consultazione)
        Map.entry("menu_art000", "/articoli"),
        // Slice: Ordini clienti (consultazione)
        Map.entry("menu_ord000", "/ordini"),
        // Slice: DDT / documenti di trasporto (consultazione)
        Map.entry("menu_bol000", "/ddt"),
        // Slice: Fatture di vendita + proforma (consultazione)
        Map.entry("menu_fat000", "/fatture"),
        Map.entry("menu_fap000", "/proforma"),
        // Slice: Ristampa documenti (cruscotto unificato + tracciabilita')
        Map.entry("menu_ristampa_doc", "/documenti"),
        // Slice: magazzino / distinta base / produzione standard (consultazione)
        Map.entry("menu_movimenti_mag", "/magazzino/movimenti"),
        Map.entry("menu_dis000", "/distinte"),
        Map.entry("std_programmazione", "/produzione"),
        // Slice: acquisti + anagrafiche minori (consultazione)
        Map.entry("menu_orf000", "/ordini-fornitore"),
        Map.entry("menu_bfo000", "/ddt-fornitore"),
        Map.entry("menu_age000", "/agenti"),
        Map.entry("menu_ban000", "/banche"),
        Map.entry("menu_car000", "/capi-area"),
        // Slice: viewer parametri aziendali (analisi MENU_AZI000)
        Map.entry("menu_azi000", "/parametri-aziendali")
        // NB: Bilancio CEE è esposto come voce SINTETICA sotto Contabilità
        // (vedi injectSyntheticEntries): il nome form legacy non è certo, quindi
        // non ci affidiamo a FORM_TO_URL per evitare voci mancanti o duplicate.
    );

    /**
     * Voci menu il cui COMANDO chiama una FUNZIONE ({@code =nomefunzione()})
     * invece di "do form": mappa funzione → URL web. Caso concreto:
     * {@code =determina_form_giacenze()} e' un dispatcher (APPLILIB) che per
     * le installazioni non-medicali apre MENU_GIACENZE → /magazzino/giacenze.
     */
    private static final Map<String, String> FUNCTION_TO_URL = Map.ofEntries(
        Map.entry("determina_form_giacenze", "/magazzino/giacenze")
    );

    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "(?i)^\\s*=\\s*([a-zA-Z0-9_]+)\\s*\\(\\s*\\)"
    );

    /**
     * Pattern del COMANDO di una voce L2 che apre un sub-submenu. Esempi:
     *   do form form\submenu LINKED name M1 with 'GESTIONECLIENTI'
     *   DO FORM FORM\SUBMENU NAME M2 WITH 'GESTIONEFORNITORI'
     * Cattura il nome del sub-submenu in group(1) (lowercased per consistenza).
     */
    private static final Pattern SUBMENU_PATTERN = Pattern.compile(
        "(?i)submenu.*?with\\s+['\"]([a-zA-Z0-9_]+)['\"]"
    );

    /**
     * Pattern del COMANDO di una foglia: estrae il nome del form.
     *   do form form\menu_cli000 linked
     *   DO FORM FORM\PRIMANOTA
     * Cattura il nome del form in group(1).
     */
    private static final Pattern FORM_PATTERN = Pattern.compile(
        "(?i)do\\s+form\\s+form\\\\([a-zA-Z0-9_]+)"
    );

    /**
     * Pattern per riconoscere chiamate a PARAGEST con la categoria parametrica:
     *   do form form\PARAGEST linked with "TOP","3","4","TIPO OPERAZIONE"
     *   DO FORM FORM\PARAGEST LINKED WITH "IVA","3","3","CODICI IVA"
     * Cattura il prefisso categoria in group(1).
     */
    private static final Pattern PARAGEST_PATTERN = Pattern.compile(
        "(?i)PARAGEST.*?with\\s+['\"]([A-Z0-9]+)['\"]"
    );

    /**
     * Costruisce l'albero del menu per l'operatore.
     * Cacheable per (tenant, operatorCode).
     */
    @Cacheable(
        value = "menuTrees",
        key = "T(com.aquarius.multitenancy.TenantContext).get() + ':' + #operatorCode"
    )
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<MenuNode> buildMenuTree(String operatorCode) {
        long t0 = System.currentTimeMillis();

        // Difesa: trim eventuali padding
        String code = operatorCode == null ? "" : operatorCode.trim();
        if (code.isEmpty()) {
            log.warn("buildMenuTree chiamato con operatorCode vuoto");
            return List.of();
        }

        // ─── STEP 1: carica i tre cursori ─────────────────────────────────
        List<LegacyMenu> leaves = menuRepository.findVisibleLeavesFor(code);
        List<LegacyMenu> topLevels = menuRepository.findAllTopLevel();
        List<LegacyMenu> secondLevels = menuRepository.findAllSecondLevel();
        long t1 = System.currentTimeMillis();

        log.info("Menu '{}': L0 visibili={}, L1={}, L2={} (DB {} ms)",
                 code, leaves.size(), topLevels.size(), secondLevels.size(), (t1 - t0));

        if (topLevels.isEmpty()) {
            log.warn("Menu '{}': NESSUN record con LIVELLO_MENU=1 (controlla tbl_menu)", code);
            return List.of();
        }

        // ─── STEP 2: raggruppa foglie L0 per MENU ─────────────────────────
        // Es: "gestioneclienti" → [Aggiornamento, Elenco note, ...]
        Map<String, List<LegacyMenu>> leavesByMenu = new HashMap<>();
        for (LegacyMenu m : leaves) {
            if (m.getMenu() == null) continue;
            leavesByMenu
                .computeIfAbsent(m.getMenu().trim().toLowerCase(Locale.ROOT), k -> new ArrayList<>())
                .add(m);
        }

        // ─── STEP 3: marca le voci L2 visibili (replica VFP) ──────────────
        // VFP: per ogni foglia L0, cerca la voce L2 il cui COMANDO contiene
        // il nome del MENU della foglia (tra apici). Quella L2 viene marcata
        // visibile e ha le foglie attaccate.
        Set<String> visibleL2Ids = new HashSet<>();
        Map<String, String> l2IdToSubmenuKey = new HashMap<>();  // L2.id → submenu pointed to

        // Pre-calcola gli ID dei MENU che hanno foglie visibili (case-insensitive)
        Set<String> menuKeysWithVisibleLeaves = new HashSet<>(leavesByMenu.keySet());

        for (LegacyMenu l2 : secondLevels) {
            String cmd = l2.getComando();
            if (cmd == null) continue;

            // Caso A: L2 è una foglia diretta (non un puntatore a submenu).
            // CREA_WINMAIN la marca sempre visibile (no filtro UTENTI a livello L2).
            if (!cmd.toUpperCase(Locale.ROOT).contains("SUBMENU")) {
                visibleL2Ids.add(l2.getId());
                continue;
            }

            // Caso B: L2 è un puntatore a sub-submenu. Estrai il nome del submenu
            // e verifica se ci sono foglie L0 visibili in quel submenu.
            Matcher mat = SUBMENU_PATTERN.matcher(cmd);
            if (mat.find()) {
                String submenuKey = mat.group(1).toLowerCase(Locale.ROOT);
                l2IdToSubmenuKey.put(l2.getId(), submenuKey);
                if (menuKeysWithVisibleLeaves.contains(submenuKey)) {
                    visibleL2Ids.add(l2.getId());
                }
            }
        }

        // ─── STEP 4: marca le voci L1 visibili ────────────────────────────
        // L1 visibile = esiste almeno una L2 visibile con stesso MENU
        Set<String> menusOfVisibleL2 = new HashSet<>();
        for (LegacyMenu l2 : secondLevels) {
            if (visibleL2Ids.contains(l2.getId()) && l2.getMenu() != null) {
                menusOfVisibleL2.add(l2.getMenu().trim().toLowerCase(Locale.ROOT));
            }
        }

        // ─── STEP 5: costruisci l'albero rendering ────────────────────────
        List<MenuNode> tree = new ArrayList<>();

        for (LegacyMenu l1 : topLevels) {
            String l1Menu = safeLower(l1.getMenu());
            boolean isVisible = menusOfVisibleL2.contains(l1Menu);

            List<MenuNode> children = new ArrayList<>();
            List<LegacyMenu> directL2 = filterByMenu(secondLevels, l1Menu);

            for (LegacyMenu l2 : directL2) {
                if (!visibleL2Ids.contains(l2.getId())) continue;
                if (l2.isSeparator()) {
                    children.add(MenuNode.builder().separator(true).build());
                    continue;
                }
                MenuNode l2Node = toNodeFromL2(l2, leavesByMenu, l2IdToSubmenuKey);
                if (l2Node != null) children.add(l2Node);
            }

            children = cleanSeparators(children);
            injectSyntheticEntries(l1Menu, children);

            MenuNode topNode = MenuNode.builder()
                .label(safeTrim(l1.getLabel()))
                .icon(guessIcon(l1Menu, l1.getIcona()))
                .children(children)
                .hasReachableLeaf((isVisible || hasSyntheticEntries(l1Menu)) && hasReachableLeaf(children))
                .build();

            int leavesCount = countLeaves(children);
            log.info("Menu '{}' L1 '{}' (menu={}): {} voci L2 nel popup, {} foglie raggiungibili",
                     code, topNode.getLabel(), l1Menu, children.size(), leavesCount);

            tree.add(topNode);
        }

        long t2 = System.currentTimeMillis();
        log.info("Menu '{}': albero costruito in {} ms ({} ms DB + {} ms build)",
                 code, (t2 - t0), (t1 - t0), (t2 - t1));

        return tree;
    }

    @org.springframework.cache.annotation.CacheEvict(value = "menuTrees", allEntries = true)
    public void evictAll() {
        log.info("Cache dei menu invalidata");
    }

    // ─── Helper privati ───────────────────────────────────────────────────

    /**
     * Inietta voci sintetiche all'inizio del popup di un top-level. Servono
     * per dare accesso diretto a funzionalità web che non hanno una voce nel
     * menu legacy VFP. Esempio: l'overview di TUTTI i parametri (340 categorie)
     * non è raggiungibile dalle voci L2 legacy (che lanciano PARAGEST con un
     * prefisso specifico), quindi aggiungiamo "Tutti i parametri" come prima
     * foglia sotto "Parametri".
     */
    private void injectSyntheticEntries(String l1Menu, List<MenuNode> children) {
        if (l1Menu == null) return;
        switch (l1Menu) {
            case "magazzino" -> {
                children.add(0, MenuNode.builder()
                    .label("Valorizzazione a data")
                    .icon("bi-cash-stack")
                    .url("/magazzino/valorizzazione")
                    .hasReachableLeaf(true)
                    .build());
                children.add(1, MenuNode.builder().separator(true).build());
            }
            case "parametri" -> {
                children.add(0, MenuNode.builder()
                    .label("Tutti i parametri")
                    .icon("bi-grid-3x3-gap")
                    .url("/parametri")
                    .hasReachableLeaf(true)
                    .build());
                children.add(1, MenuNode.builder()
                    .label("Parametri aziendali")
                    .icon("bi-sliders")
                    .url("/parametri-aziendali")
                    .hasReachableLeaf(true)
                    .build());
                children.add(2, MenuNode.builder().separator(true).build());
            }
            case "contabilit", "contabilita" -> {
                // Consultazione contabile (web). La "Primanota" NON è qui:
                // arriva dal container legacy "Prima nota" trasformato in voce
                // unica (vedi toNodeFromL2 / isPrimaNotaContainer). Qui solo le
                // voci che il menu legacy non espone come sottomenu dedicato.
                children.add(0, MenuNode.builder()
                    .label("Storico contabile")
                    .icon("bi-clock-history")
                    .url("/contabilita/storico")
                    .hasReachableLeaf(true)
                    .build());
                children.add(1, MenuNode.builder()
                    .label("Registri IVA")
                    .icon("bi-journal-check")
                    .url("/contabilita/registri-iva/vendite")
                    .hasReachableLeaf(true)
                    .build());
                children.add(2, MenuNode.builder()
                    .label("Bilancio")
                    .icon("bi-bar-chart-steps")
                    .url("/contabilita/bilancio")
                    .hasReachableLeaf(true)
                    .build());
                children.add(3, MenuNode.builder()
                    .label("Bilancio CEE")
                    .icon("bi-file-earmark-bar-graph")
                    .url("/contabilita/bilancio-cee")
                    .hasReachableLeaf(true)
                    .build());
                children.add(4, MenuNode.builder()
                    .label("Partitario clienti")
                    .icon("bi-person-lines-fill")
                    .url("/contabilita/partitari/clienti")
                    .hasReachableLeaf(true)
                    .build());
                children.add(5, MenuNode.builder()
                    .label("Partitario fornitori")
                    .icon("bi-person-lines-fill")
                    .url("/contabilita/partitari/fornitori")
                    .hasReachableLeaf(true)
                    .build());
                children.add(6, MenuNode.builder().separator(true).build());
                // Shortcut diretti al piano dei conti (web feature, non in VFP)
                children.add(7, MenuNode.builder()
                    .label("Piano dei conti (albero)")
                    .icon("bi-diagram-3")
                    .url("/conti/tree")
                    .hasReachableLeaf(true)
                    .build());
                children.add(8, MenuNode.builder()
                    .label("Piano dei conti (lista)")
                    .icon("bi-list-ul")
                    .url("/conti")
                    .hasReachableLeaf(true)
                    .build());
                children.add(9, MenuNode.builder().separator(true).build());
            }
            // Si possono aggiungere altre top-section sintetiche qui in futuro
        }
    }

    /** Riconosce il container L2 "Prima nota" (frammentato nel VFP). */
    private boolean isPrimaNotaContainer(LegacyMenu l2) {
        String label = safeLower(l2.getLabel());
        return label != null && label.contains("prima nota");
    }

    private boolean hasSyntheticEntries(String l1Menu) {
        return "parametri".equals(l1Menu)
            || "contabilit".equals(l1Menu) || "contabilita".equals(l1Menu)
            || "magazzino".equals(l1Menu);
    }

    private MenuNode toNodeFromL2(LegacyMenu l2,
                                  Map<String, List<LegacyMenu>> leavesByMenu,
                                  Map<String, String> l2IdToSubmenuKey) {
        String submenuKey = l2IdToSubmenuKey.get(l2.getId());

        if (submenuKey != null) {
            // L2 è un container: allega le foglie L0 del sub-submenu
            List<LegacyMenu> children = leavesByMenu.getOrDefault(submenuKey, List.of());

            // Caso speciale "Prima nota": nel VFP è frammentata in molte voci
            // (inserimento effettiva/previsionale, aggiornamento, ricerca,
            // annullo, duplica, blocco/sblocco...). Nel nuovo modello sono tutte
            // operazioni di un'unica CRUD sulla registrazione: sostituiamo le
            // foglie legacy con una sola voce web unificata.
            //
            // AUTORIZZAZIONE: `children` contiene SOLO le foglie che l'utente
            // può vedere (il filtro UTENTI è applicato a DB per foglia). Quindi
            // la voce unificata compare solo se l'utente era abilitato ad almeno
            // una voce di prima nota nel legacy; altrimenti il container prima
            // nota non produce alcuna voce. La granularità per-azione
            // (READ/CREATE/DELETE/...) sarà agganciata quando esisterà la CRUD
            // di scrittura; oggi la voce apre la sola consultazione.
            if (isPrimaNotaContainer(l2)) {
                boolean hasVisibleLeaf = children.stream().anyMatch(c -> !c.isSeparator());
                if (!hasVisibleLeaf) {
                    return null;   // utente non abilitato ad alcuna voce prima nota
                }
                List<MenuNode> unified = new ArrayList<>();
                unified.add(MenuNode.builder()
                    .label("Prima nota")
                    .icon("bi-journal-text")
                    .url("/contabilita/primanota")
                    .hasReachableLeaf(true)
                    .build());
                return MenuNode.builder()
                    .label(safeTrim(l2.getLabel()))
                    .icon(iconOrNull(l2.getIcona()))
                    .children(unified)
                    .hasReachableLeaf(true)
                    .build();
            }

            List<MenuNode> leafNodes = new ArrayList<>();
            for (LegacyMenu leaf : children) {
                if (leaf.isSeparator()) {
                    leafNodes.add(MenuNode.builder().separator(true).build());
                } else {
                    leafNodes.add(toLeafNode(leaf));
                }
            }
            leafNodes = cleanSeparators(leafNodes);
            return MenuNode.builder()
                .label(safeTrim(l2.getLabel()))
                .icon(iconOrNull(l2.getIcona()))
                .children(leafNodes)
                .hasReachableLeaf(hasReachableLeaf(leafNodes))
                .build();
        }

        // L2 è una foglia diretta (esegue un form senza submenu intermedio)
        return toLeafNode(l2);
    }

    private MenuNode toLeafNode(LegacyMenu m) {
        String formName = extractFormName(m.getComando());
        String url;
        // Caso speciale: PARAGEST è il form generico parametri; deriva URL
        // dal prefisso categoria nel COMANDO, es. "TOP" → /parametri/TOP
        if (formName != null && formName.equalsIgnoreCase("paragest")) {
            url = extractParagestUrl(m.getComando());
        } else if (formName == null) {
            // Comando non "do form": prova la mappa funzioni (=funzione())
            url = extractFunctionUrl(m.getComando());
        } else {
            url = FORM_TO_URL.get(formName.toLowerCase(Locale.ROOT));
        }
        return MenuNode.builder()
            .label(safeTrim(m.getLabel()))
            .icon(iconOrNull(m.getIcona()))
            .formName(formName)
            .url(url)
            .hasReachableLeaf(url != null)
            .build();
    }

    /**
     * Se il COMANDO chiama PARAGEST con un prefisso categoria,
     * ritorna l'URL della pagina web corrispondente. Es:
     *   "do form form\PARAGEST linked with 'TOP','3','4',..."  →  "/parametri/TOP"
     */
    private String extractParagestUrl(String comando) {
        if (comando == null) return null;
        Matcher m = PARAGEST_PATTERN.matcher(comando);
        if (m.find()) {
            return "/parametri/" + m.group(1).toUpperCase(Locale.ROOT);
        }
        return null;
    }

    /** URL per comandi menu del tipo {@code =nomefunzione()} (vedi FUNCTION_TO_URL). */
    private String extractFunctionUrl(String comando) {
        if (comando == null) return null;
        Matcher m = FUNCTION_PATTERN.matcher(comando.trim());
        return m.find() ? FUNCTION_TO_URL.get(m.group(1).toLowerCase(Locale.ROOT)) : null;
    }

    private String extractFormName(String comando) {
        if (comando == null) return null;
        Matcher mat = FORM_PATTERN.matcher(comando);
        return mat.find() ? mat.group(1).toLowerCase(Locale.ROOT) : null;
    }

    /** Filtra le L2 che appartengono a un dato MENU di L1 (case-insensitive). */
    private List<LegacyMenu> filterByMenu(List<LegacyMenu> items, String menuKey) {
        if (menuKey == null) return List.of();
        List<LegacyMenu> out = new ArrayList<>();
        for (LegacyMenu m : items) {
            if (menuKey.equalsIgnoreCase(safeTrim(m.getMenu()))) out.add(m);
        }
        return out;
    }

    /** Rimuove separator consecutivi, leading, trailing. */
    private List<MenuNode> cleanSeparators(List<MenuNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return nodes;
        List<MenuNode> out = new ArrayList<>(nodes.size());
        boolean prevWasSep = true;  // così salta i leading
        for (MenuNode n : nodes) {
            if (n.isSeparator()) {
                if (prevWasSep) continue;
                out.add(n);
                prevWasSep = true;
            } else {
                out.add(n);
                prevWasSep = false;
            }
        }
        // rimuovi trailing
        while (!out.isEmpty() && out.get(out.size() - 1).isSeparator()) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    private boolean hasReachableLeaf(List<MenuNode> nodes) {
        if (nodes == null) return false;
        for (MenuNode n : nodes) {
            if (n.isSeparator()) continue;
            if (n.getUrl() != null && !n.getUrl().isBlank()) return true;
            if (hasReachableLeaf(n.getChildren())) return true;
        }
        return false;
    }

    private int countLeaves(List<MenuNode> nodes) {
        if (nodes == null) return 0;
        int sum = 0;
        for (MenuNode n : nodes) {
            if (n.isSeparator()) continue;
            if (n.getChildren() == null || n.getChildren().isEmpty()) sum++;
            else sum += countLeaves(n.getChildren());
        }
        return sum;
    }

    /**
     * Sceglie un'icona Bootstrap Icons per i top-level: prima prova mapping
     * hardcoded sui MENU noti, altrimenti usa l'ICONA legacy (path BMP che
     * non sa renderizzare, fallback a icona generica).
     */
    private String guessIcon(String menuKey, String legacyIcona) {
        if (menuKey == null) return "bi-folder";
        return switch (menuKey) {
            case "clienti"      -> "bi-people";
            case "fornitori"    -> "bi-truck";
            case "consulenza"   -> "bi-briefcase";
            case "magazzino"    -> "bi-box-seam";
            case "produzione"   -> "bi-gear";
            case "contabilit", "contabilita" -> "bi-calculator";
            case "statistiche"  -> "bi-graph-up";
            case "web"          -> "bi-globe";
            case "parametri"    -> "bi-sliders";
            case "opzioni"      -> "bi-toggles";
            case "aiuto", "help" -> "bi-question-circle";
            default             -> "bi-folder";
        };
    }

    private String iconOrNull(String legacyIcona) {
        // Le icone legacy sono path a file BMP (es. "bmp\icone\agenda24x24.bmp"),
        // non renderizzabili sul web. Ritorna null → frontend usa bullet default.
        return null;
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
