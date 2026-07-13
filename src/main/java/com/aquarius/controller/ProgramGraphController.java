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
                       @RequestParam(value = "trail", required = false) String trail,
                       @RequestParam(value = "orfani", required = false) String orfani,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        model.addAttribute("stats", graph.stats());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/utilita/legami", principal.getUsername()));

        if (q != null && !q.isBlank()) {
            model.addAttribute("q", q.trim());
            model.addAttribute("results", graph.search(q.trim(), 80));
            return "utilita/legami";
        }
        if (orfani != null) {
            model.addAttribute("orfani", graph.orphans(300));
            return "utilita/legami";
        }
        if (id != null && !id.isBlank()) {
            GraphObject node = graph.get(id);
            model.addAttribute("node", node);
            if (node != null) {
                model.addAttribute("parents", enrich(graph.parents(id), true));
                // figli raggruppati per tipo di legame
                Map<String, List<Object[]>> childrenByType = new LinkedHashMap<>();
                for (Link l : graph.children(id)) {
                    childrenByType.computeIfAbsent(l.getType(), k -> new ArrayList<>())
                        .add(new Object[]{ l, graph.get(l.getChildId()) });
                }
                model.addAttribute("childrenByType", childrenByType);
                // breadcrumb di navigazione: ids separati da '|'
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
                // cap alla coda di 10 elementi
                String[] parts = newTrail.split("\\|");
                if (parts.length > 10) {
                    newTrail = String.join("|",
                        java.util.Arrays.copyOfRange(parts, parts.length - 10, parts.length));
                }
                model.addAttribute("trailNext", newTrail);
                model.addAttribute("trailCurrent", trail == null ? "" : trail);
            }
            return "utilita/legami";
        }
        return "utilita/legami";
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
