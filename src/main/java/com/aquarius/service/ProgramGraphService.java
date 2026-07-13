package com.aquarius.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Grafo dei legami tra oggetti legacy (e ponti verso il web), caricato
 * da migration/program_objects.csv + program_links.csv (sessione 12).
 * Parser RFC-4180 riusato da CompanyParameterCatalogService. Il grafo
 * e' immutabile a runtime: caricato una volta, indicizzato per id con
 * reverse-index genitori/figli.
 */
@Service
@Slf4j
public class ProgramGraphService {

    private static final String OBJECTS = "migration/program_objects.csv";
    private static final String LINKS = "migration/program_links.csv";

    @Data @AllArgsConstructor
    public static class GraphObject {
        private String id;
        private String file;
        private String type;
        private String name;
        private String description;
        private String newLocation;
    }

    @Data @AllArgsConstructor
    public static class Link {
        private String parentId;
        private String childId;
        private String type;
        private String evidence;
    }

    @Getter private final Map<String, GraphObject> byId = new HashMap<>();
    @Getter private final Map<String, List<Link>> childrenOf = new HashMap<>();
    @Getter private final Map<String, List<Link>> parentsOf = new HashMap<>();
    private volatile boolean loaded = false;

    public synchronized void ensureLoaded() {
        if (loaded) return;
        try {
            for (String[] f : readCsv(OBJECTS, 6)) {
                byId.put(f[0], new GraphObject(f[0], f[1], f[2], f[3], f[4], f[5]));
            }
            for (String[] f : readCsv(LINKS, 4)) {
                Link l = new Link(f[0], f[1], f[2], f[3]);
                childrenOf.computeIfAbsent(l.getParentId(), k -> new ArrayList<>()).add(l);
                parentsOf.computeIfAbsent(l.getChildId(), k -> new ArrayList<>()).add(l);
            }
            loaded = true;
            log.info("Grafo programmi: {} oggetti, {} archi", byId.size(),
                childrenOf.values().stream().mapToInt(List::size).sum());
        } catch (Exception e) {
            log.error("Errore caricamento grafo programmi", e);
        }
    }

    public GraphObject get(String id) { ensureLoaded(); return byId.get(id); }

    public List<Link> children(String id) {
        ensureLoaded(); return childrenOf.getOrDefault(id, List.of());
    }

    public List<Link> parents(String id) {
        ensureLoaded(); return parentsOf.getOrDefault(id, List.of());
    }

    /** Ricerca per nome/descrizione/id (case-insensitive), max limit risultati. */
    public List<GraphObject> search(String q, int limit) {
        ensureLoaded();
        String qq = q.toLowerCase(Locale.ROOT);
        List<GraphObject> out = new ArrayList<>();
        for (GraphObject o : byId.values()) {
            if (o.getName().toLowerCase(Locale.ROOT).contains(qq)
                || o.getDescription().toLowerCase(Locale.ROOT).contains(qq)
                || o.getId().toLowerCase(Locale.ROOT).contains(qq)) {
                out.add(o);
                if (out.size() >= limit) break;
            }
        }
        out.sort((a, b) -> a.getId().compareTo(b.getId()));
        return out;
    }

    /** Oggetti FORM/PRG senza alcun genitore (orfani). */
    public List<GraphObject> orphans(int limit) {
        ensureLoaded();
        List<GraphObject> out = new ArrayList<>();
        for (GraphObject o : byId.values()) {
            if (("FORM".equals(o.getType()) || "PRG".equals(o.getType()))
                && parentsOf.getOrDefault(o.getId(), List.of()).isEmpty()) {
                out.add(o);
                if (out.size() >= limit) break;
            }
        }
        out.sort((a, b) -> a.getId().compareTo(b.getId()));
        return out;
    }

    /** Elenco filtrato per tipo (opzionale) e testo su qualunque campo. Ordinato. */
    public List<GraphObject> filter(String type, String q) {
        ensureLoaded();
        String qq = q == null ? "" : q.toLowerCase(Locale.ROOT).trim();
        List<GraphObject> out = new ArrayList<>();
        for (GraphObject o : byId.values()) {
            if (type != null && !type.isBlank() && !type.equals(o.getType())) continue;
            if (!qq.isEmpty()) {
                String hay = (nz(o.getName()) + " " + nz(o.getDescription()) + " "
                    + nz(o.getFile()) + " " + nz(o.getId()) + " " + nz(o.getNewLocation()))
                    .toLowerCase(Locale.ROOT);
                if (!hay.contains(qq)) continue;
            }
            out.add(o);
        }
        out.sort((a, b) -> a.getType().equals(b.getType())
            ? nz(a.getName()).compareToIgnoreCase(nz(b.getName()))
            : a.getType().compareTo(b.getType()));
        return out;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    public int parentCount(String id) { ensureLoaded(); return parentsOf.getOrDefault(id, List.of()).size(); }
    public int childCount(String id)  { ensureLoaded(); return childrenOf.getOrDefault(id, List.of()).size(); }

    public Map<String, Long> stats() {
        ensureLoaded();
        Map<String, Long> m = new java.util.TreeMap<>();
        byId.values().forEach(o -> m.merge(o.getType(), 1L, Long::sum));
        return m;
    }

    private List<String[]> readCsv(String resource, int cols) throws Exception {
        List<String[]> out = new ArrayList<>();
        ClassPathResource res = new ClassPathResource(resource);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String line; boolean header = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                if (header) { header = false; continue; }
                String[] f = CompanyParameterCatalogService.parseCsvLine(line);
                if (f.length >= cols) out.add(f);
            }
        }
        return out;
    }
}
