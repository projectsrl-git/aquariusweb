package com.aquarius.controller;

import com.aquarius.dto.MigrationRow;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.service.MigrationTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.aquarius.security.AquariusPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Viewer del migration tracker (.scx ↔ web) — utilità web-only.
 * Legge il CSV dal classpath e lo mostra con filtri client-side.
 */
@Controller
@RequestMapping("/utilita/migrazione")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationTrackerService trackerService;
    private final BreadcrumbService breadcrumbService;

    @GetMapping
    public String view(Model model, @AuthenticationPrincipal AquariusPrincipal principal) {
        List<MigrationRow> rows = trackerService.loadRows();

        // Conteggi per stato (per il riassunto in testa)
        Map<String, Long> byStatus = rows.stream()
            .collect(Collectors.groupingBy(
                r -> r.getStatus().isEmpty() ? "—" : r.getStatus(),
                LinkedHashMap::new, Collectors.counting()));

        model.addAttribute("rows", rows);
        model.addAttribute("total", rows.size());
        model.addAttribute("byStatus", byStatus);
        model.addAttribute("forms", new TreeSet<>(rows.stream()
            .map(MigrationRow::getFormFile).filter(s -> !s.isEmpty()).collect(Collectors.toSet())));
        model.addAttribute("reasons", new TreeSet<>(rows.stream()
            .map(MigrationRow::getReasonCode).filter(s -> !s.isEmpty()).collect(Collectors.toSet())));
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/utilita/migrazione", principal.getUsername()));
        return "utilita/migrazione";
    }
}
