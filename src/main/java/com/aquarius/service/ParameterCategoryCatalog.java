package com.aquarius.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Catalogo statico delle ~340 categorie di parametri Aquarius (TOP, IVA, CPA,
 * AGE, CVE, ...). Caricato all'avvio da {@code resources/data/param-categories.csv}
 * (generato dal codice VFP analizzando le invocazioni di PARAGEST nei form
 * MENU_*).
 *
 * <p>Pattern d'uso:</p>
 * <pre>
 *   Optional&lt;Category&gt; c = catalog.byPrefix("TOP");
 *   List&lt;Category&gt; tutte = catalog.all();
 *   Map&lt;String, List&lt;Category&gt;&gt; perArea = catalog.byArea();
 * </pre>
 *
 * <p>Per modificare la categorizzazione "area" o aggiungere categorie, basta
 * editare il CSV — niente codice da ricompilare.</p>
 */
@Component
@Slf4j
public class ParameterCategoryCatalog {

    @Value("classpath:data/param-categories.csv")
    private Resource csvResource;

    private List<Category> all = Collections.emptyList();
    private Map<String, Category> byPrefix = Collections.emptyMap();

    @PostConstruct
    void load() {
        List<Category> loaded = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(csvResource.getInputStream(), StandardCharsets.UTF_8))) {
            String header = r.readLine();  // skip
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = parseCsv(line);
                if (parts.length < 5) continue;
                loaded.add(new Category(
                    parts[0],                            // prefix
                    Integer.parseInt(parts[1].trim()),   // filtLen
                    Integer.parseInt(parts[2].trim()),   // codLen
                    parts[3],                            // description
                    parts[4]                             // area
                ));
            }
        } catch (Exception e) {
            log.error("Impossibile caricare param-categories.csv: {}", e.getMessage(), e);
        }
        loaded.sort((a, b) -> a.prefix.compareTo(b.prefix));
        this.all = loaded;
        Map<String, Category> idx = new HashMap<>();
        for (Category c : loaded) idx.put(c.prefix.toUpperCase(), c);
        this.byPrefix = idx;
        log.info("Catalogo parametri caricato: {} categorie", loaded.size());
    }

    /**
     * Lista immutabile di tutte le categorie, ordinate per prefisso.
     */
    public List<Category> all() {
        return Collections.unmodifiableList(all);
    }

    /**
     * Cerca per prefisso (case-insensitive).
     */
    public Optional<Category> byPrefix(String prefix) {
        if (prefix == null) return Optional.empty();
        return Optional.ofNullable(byPrefix.get(prefix.trim().toUpperCase()));
    }

    /**
     * Categorie raggruppate per area, in ordine alfabetico dentro ogni area.
     * Aree ordinate: Contabilità, Anagrafiche, Commerciale, Magazzino,
     * Produzione, Assistenza, Risorse, Generali, Altro.
     */
    public Map<String, List<Category>> byArea() {
        // Ordine areale preferito (le altre vanno dopo in ordine alfabetico)
        List<String> preferred = List.of(
            "Contabilità", "Anagrafiche", "Commerciale",
            "Magazzino / Logistica", "Produzione", "Assistenza / Tecnici",
            "Risorse umane", "Generali", "Altro"
        );
        Map<String, List<Category>> raw = new HashMap<>();
        for (Category c : all) {
            raw.computeIfAbsent(c.area, k -> new ArrayList<>()).add(c);
        }
        Map<String, List<Category>> sorted = new LinkedHashMap<>();
        for (String key : preferred) {
            if (raw.containsKey(key)) sorted.put(key, raw.remove(key));
        }
        new TreeMap<>(raw).forEach(sorted::put);
        return sorted;
    }

    /**
     * Estrae il "valore" del codice rimuovendo il prefisso. Es. CODICE='TOP0001'
     * + prefisso='TOP' → '0001'. Per categorie con filtLen lungo (es. NUMGAC),
     * il filtro è la prima parte del codice.
     */
    public String extractValue(String fullCodice, Category cat) {
        if (fullCodice == null || cat == null) return "";
        if (fullCodice.length() <= cat.filtLen) return "";
        return fullCodice.substring(cat.filtLen);
    }

    /**
     * Compone il CODICE completo dato il prefisso e il valore.
     */
    public String composeCodice(Category cat, String value) {
        return cat.prefix + (value == null ? "" : value);
    }

    /** Parsing CSV minimal-aware delle virgole dentro virgolette. */
    private static String[] parseCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Category {
        /** Prefisso del CODICE in PARA (es. "TOP", "IVA", "NUMGAC"). */
        private String prefix;
        /** Lunghezza del prefisso (di solito = prefix.length()). */
        private int filtLen;
        /** Lunghezza del codice value (la parte dopo il prefisso). */
        private int codLen;
        /** Descrizione human-readable (mostrata in UI). */
        private String description;
        /** Area funzionale (per raggruppamento UI). */
        private String area;
    }
}
