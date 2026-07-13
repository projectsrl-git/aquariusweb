package com.aquarius.controller;

import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.service.ProgramGraphService;
import com.aquarius.service.ProgramGraphService.GraphObject;
import com.aquarius.service.ProgramGraphService.Link;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grafo dei legami tra programmi legacy (read-only, node-centric):
 * ogni oggetto e' una scheda con descrizione, "usato da" (genitori,
 * incluse le voci di menu), figli per tipo di legame e ponte
 * MIGRATED_TO verso il web. Ogni nodo e' cliccabile e ri-centra la
 * vista; il percorso di navigazione e' mantenuto in una breadcrumb.
 * I CSV sorgente sono scaricabili per essere modificati e ripassati
 * in chat (scope/priorita' di migrazione).
 */
@Controller
@RequestMapping("/utilita/legami")
@RequiredArgsConstructor
public class ProgramGraphController {

    /** File scaricabili (whitelist). */
    private static final Set<String> DOWNLOADS = Set.of(
        "program_objects.csv", "program_links.csv",
        "scx_migration_tracker.csv", "menu_coverage_audit.csv");

    private final ProgramGraphService graph;
    private final BreadcrumbService breadcrumbService;

    @GetMapping
    public String view(@RequestParam(value = "id", required = false) String id,
                       @RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "tipo", required = false) String tipo,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size,
                       @RequestParam(value = "trail", required = false) String trail,
                       @RequestParam(value = "orfani", required = false) String orfani,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        model.addAttribute("stats", graph.stats());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/utilita/legami", principal.getUsername()));

        // ── DETTAGLIO NODO (con grafo SVG del vicinato) ──
        if (id != null && !id.isBlank()) {
            GraphObject node = graph.get(id);
            if (node != null) {
                model.addAttribute("node", node);
                List<Object[]> parents = enrich(graph.parents(id), true);
                model.addAttribute("parents", parents);
                Map<String, List<Object[]>> childrenByType = new LinkedHashMap<>();
                for (Link l : graph.children(id)) {
                    childrenByType.computeIfAbsent(l.getType(), k -> new ArrayList<>())
                        .add(new Object[]{ l, graph.get(l.getChildId()) });
                }
                model.addAttribute("childrenByType", childrenByType);
                model.addAttribute("graphSvg", neighborhoodSvg(node, parents, childrenByType));

                List<GraphObject> trailNodes = new ArrayList<>();
                if (trail != null && !trail.isBlank()) {
                    for (String t : trail.split("\\|")) {
                        GraphObject g = graph.get(t);
                        if (g != null) trailNodes.add(g);
                        if (trailNodes.size() >= 10) break;
                    }
                }
                model.addAttribute("trailNodes", trailNodes);
                String newTrail = (trail == null || trail.isBlank()) ? id : trail + "|" + id;
                String[] parts = newTrail.split("\\|");
                if (parts.length > 10) {
                    newTrail = String.join("|",
                        java.util.Arrays.copyOfRange(parts, parts.length - 10, parts.length));
                }
                model.addAttribute("trailNext", newTrail);
                model.addAttribute("trailCurrent", trail == null ? "" : trail);
                return "utilita/legami";
            }
            // id presente ma oggetto non trovato → prosegue alla griglia con avviso
            model.addAttribute("notFound", id);
        }

        // ── GRIGLIA (default): tutti i dati subito, contatori-filtro, ricerca, paginazione ──
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("tipo", tipo);
        model.addAttribute("soloOrfani", orfani != null);
        List<GraphObject> filtered = graph.filter(tipo, q);
        if (orfani != null) {
            List<GraphObject> orf = new ArrayList<>();
            for (GraphObject o : filtered) {
                if (("FORM".equals(o.getType()) || "PRG".equals(o.getType()))
                    && graph.parentCount(o.getId()) == 0) orf.add(o);
            }
            filtered = orf;
        }
        int total = filtered.size();
        int sz = (size == null || size < 5) ? 20 : Math.min(size, 200);
        int totalPages = Math.max(1, (total + sz - 1) / sz);
        int pg = (page == null || page < 0) ? 0 : page;
        if (pg >= totalPages) pg = totalPages - 1;
        int from = Math.min(pg * sz, total);
        int to = Math.min(from + sz, total);
        List<Object[]> rows = new ArrayList<>();
        for (GraphObject o : filtered.subList(from, to)) {
            rows.add(new Object[]{ o, graph.parentCount(o.getId()), graph.childCount(o.getId()) });
        }
        model.addAttribute("rows", rows);
        model.addAttribute("total", total);
        model.addAttribute("page", pg);
        model.addAttribute("size", sz);
        model.addAttribute("totalPages", totalPages);
        return "utilita/legami";
    }

    /** SVG del vicinato (1 hop): centro + genitori a sinistra + figli a destra, nodi cliccabili. */
    private String neighborhoodSvg(GraphObject center, List<Object[]> parents,
                                   Map<String, List<Object[]>> childrenByType) {
        List<GraphObject> par = new ArrayList<>();
        for (Object[] p : parents) { if (p[1] != null) par.add((GraphObject) p[1]); if (par.size() >= 8) break; }
        List<GraphObject> chi = new ArrayList<>();
        for (List<Object[]> lst : childrenByType.values())
            for (Object[] c : lst) { if (c[1] != null) chi.add((GraphObject) c[1]); if (chi.size() >= 8) break; }
        int rows = Math.max(1, Math.max(par.size(), chi.size()));
        int rowH = 40, top = 30, W = 760, H = top + rows * rowH + 20;
        int cx = 300, cw = 160, cy = top + (rows * rowH) / 2 - 14;
        StringBuilder s = new StringBuilder();
        s.append("<svg viewBox=\"0 0 ").append(W).append(" ").append(H)
         .append("\" xmlns=\"http://www.w3.org/2000/svg\" style=\"width:100%;height:auto\">");
        // archi
        for (int i = 0; i < par.size(); i++) {
            int y = top + i * rowH + 14;
            s.append("<line x1=\"200\" y1=\"").append(y).append("\" x2=\"").append(cx)
             .append("\" y2=\"").append(cy + 14).append("\" stroke=\"#adb5bd\"/>");
        }
        for (int i = 0; i < chi.size(); i++) {
            int y = top + i * rowH + 14;
            s.append("<line x1=\"").append(cx + cw).append("\" y1=\"").append(cy + 14)
             .append("\" x2=\"560\" y2=\"").append(y).append("\" stroke=\"#adb5bd\"/>");
        }
        // genitori (sinistra)
        for (int i = 0; i < par.size(); i++) node(s, par.get(i), 20, top + i * rowH, 180, center.getId());
        // figli (destra)
        for (int i = 0; i < chi.size(); i++) node(s, chi.get(i), 560, top + i * rowH, 180, center.getId());
        // centro
        nodeBox(s, cx, cy, cw, center.getType(), center.getName(), true, null, null);
        s.append("</svg>");
        return s.toString();
    }

    private void node(StringBuilder s, GraphObject o, int x, int y, int w, String trail) {
        nodeBox(s, x, y, w, o.getType(), o.getName(), false,
            "?id=" + esc(o.getId()) + "&amp;trail=" + esc(trail), null);
    }

    private void nodeBox(StringBuilder s, int x, int y, int w, String type, String name,
                         boolean center, String href, String ignored) {
        String fill = center ? "#0d6efd" : "#f8f9fa";
        String stroke = center ? "#0a58ca" : "#ced4da";
        String tc = center ? "#ffffff" : "#212529";
        if (href != null) s.append("<a href=\"").append(href).append("\">");
        s.append("<rect x=\"").append(x).append("\" y=\"").append(y)
         .append("\" width=\"").append(w).append("\" height=\"28\" rx=\"5\" fill=\"").append(fill)
         .append("\" stroke=\"").append(stroke).append("\"/>");
        s.append("<text x=\"").append(x + 8).append("\" y=\"").append(y + 12)
         .append("\" font-size=\"8\" fill=\"").append(center ? "#cfe2ff" : "#6c757d").append("\">")
         .append(esc(type)).append("</text>");
        s.append("<text x=\"").append(x + 8).append("\" y=\"").append(y + 23)
         .append("\" font-size=\"11\" font-family=\"monospace\" fill=\"").append(tc).append("\">")
         .append(esc(clip(name, 24))).append("</text>");
        if (href != null) s.append("</a>");
    }

    private static String clip(String s, int n) { s = s == null ? "" : s; return s.length() > n ? s.substring(0, n - 1) + "\u2026" : s; }
    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Coppie (link, oggetto) per i genitori. */
    private List<Object[]> enrich(List<Link> links, boolean parentSide) {
        List<Object[]> out = new ArrayList<>();
        for (Link l : links) {
            out.add(new Object[]{ l, graph.get(parentSide ? l.getParentId() : l.getChildId()) });
        }
        return out;
    }

    @GetMapping("/download/{file}")
    public ResponseEntity<byte[]> download(@PathVariable String file) throws Exception {
        if (!DOWNLOADS.contains(file)) {
            return ResponseEntity.notFound().build();
        }
        ClassPathResource res = new ClassPathResource("migration/" + file);
        if (!res.exists()) return ResponseEntity.notFound().build();
        byte[] bytes = res.getInputStream().readAllBytes();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file)
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(bytes);
    }
}
