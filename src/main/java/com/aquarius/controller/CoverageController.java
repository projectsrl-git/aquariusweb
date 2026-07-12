package com.aquarius.controller;

import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.service.CoverageAuditService;
import com.aquarius.service.CoverageAuditService.Entry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Matrice di copertura della migrazione (read-only): legge l'audit CSV e
 * la mostra per area/sottomenu con filtri. Il riepilogo testuale con le
 * priorita' e' in resources/migration/COVERAGE_REPORT.md.
 */
@Controller
@RequestMapping("/utilita/copertura")
@RequiredArgsConstructor
public class CoverageController {

    private final CoverageAuditService auditService;
    private final BreadcrumbService breadcrumbService;

    @GetMapping
    public String view(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "area", required = false) String area,
                       @RequestParam(value = "stato", required = false) String stato,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        List<Entry> all = auditService.load();
        String qq = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String aa = area == null ? "" : area.trim();
        String ss = stato == null ? "" : stato.trim().toUpperCase(Locale.ROOT);

        List<Entry> filtered = all.stream()
            .filter(e -> aa.isEmpty() || e.getArea().equals(aa))
            .filter(e -> ss.isEmpty() || e.getCopertura().equals(ss))
            .filter(e -> qq.isEmpty()
                || e.getVoce().toLowerCase(Locale.ROOT).contains(qq)
                || e.getPercorso().toLowerCase(Locale.ROOT).contains(qq)
                || e.getEvidenza().toLowerCase(Locale.ROOT).contains(qq)
                || e.getNote().toLowerCase(Locale.ROOT).contains(qq))
            .toList();

        // matrice area -> conteggi per stato (su TUTTO l'audit, non filtrato)
        Map<String, Map<String, Long>> matrix = new TreeMap<>();
        for (Entry e : all) {
            matrix.computeIfAbsent(e.getArea(), k -> new LinkedHashMap<>())
                  .merge(e.getCopertura(), 1L, Long::sum);
        }

        List<String> aree = all.stream().map(Entry::getArea).distinct().sorted().toList();

        model.addAttribute("entries", filtered);
        model.addAttribute("matrix", matrix);
        model.addAttribute("aree", aree);
        model.addAttribute("totale", all.size());
        model.addAttribute("mostrati", filtered.size());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("area", aa);
        model.addAttribute("stato", ss);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/utilita/copertura", principal.getUsername()));
        return "utilita/copertura";
    }
}
