package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.dto.parametri.CompanyParameterEntry;
import com.aquarius.repository.tenant.CompanyParameterValuesDao;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.service.CompanyParameterCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

/**
 * Viewer READ-ONLY dei parametri aziendali (MENU_AZI000 → U_AZI_*):
 * catalogo dell'analisi DEEP (metadati: scopo, funzionamento, evidenze) +
 * valori correnti della societa'. Nessuna modifica: la scrittura dei
 * parametri pilota il comportamento del gestionale ed e' riservata a slice
 * future. Pattern: come il viewer del migration tracker.
 */
@Controller
@RequestMapping("/parametri-aziendali")
@RequiredArgsConstructor
@Slf4j
public class CompanyParametersController {

    private final CompanyParameterCatalogService catalogService;
    private final CompanyParameterValuesDao valuesDao;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    @GetMapping
    public String view(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "gruppo", required = false) String gruppo,
                       @RequestParam(value = "conf", required = false) String conf,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        List<CompanyParameterEntry> all = catalogService.loadCatalog();
        Map<String, String> values = valuesDao.loadValues(fiscalContext.getSocietyCode());
        for (CompanyParameterEntry e : all) {
            String v = values.get(e.getColumn() == null ? "" : e.getColumn().toUpperCase());
            e.setValuePresent(v != null);
            e.setCurrentValue(v == null ? "" : v);
        }

        String qq = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String gg = gruppo == null ? "" : gruppo.trim();
        String cc = conf == null ? "" : conf.trim().toUpperCase(Locale.ROOT);
        List<CompanyParameterEntry> filtered = all.stream()
            .filter(e -> gg.isEmpty() || e.getTopGroup().equals(gg))
            .filter(e -> cc.isEmpty() || cc.equals(e.getConfidence()))
            .filter(e -> qq.isEmpty()
                || contains(e.getLabel(), qq) || contains(e.getPurpose(), qq)
                || contains(e.getTableColumn(), qq) || contains(e.getColumn(), qq)
                || contains(e.getHowItWorks(), qq) || contains(e.getGroup(), qq))
            .collect(Collectors.toList());

        // raggruppa per tab di primo livello, mantenendo l'ordine del form
        Map<String, List<CompanyParameterEntry>> grouped = new LinkedHashMap<>();
        for (CompanyParameterEntry e : filtered) {
            grouped.computeIfAbsent(e.getTopGroup(), k -> new java.util.ArrayList<>()).add(e);
        }

        List<String> allGroups = all.stream().map(CompanyParameterEntry::getTopGroup)
            .distinct().collect(Collectors.toList());

        long uncertain = all.stream().filter(CompanyParameterEntry::isUncertain).count();

        model.addAttribute("grouped", grouped);
        model.addAttribute("allGroups", allGroups);
        model.addAttribute("totale", all.size());
        model.addAttribute("mostrati", filtered.size());
        model.addAttribute("incerti", uncertain);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("gruppo", gg);
        model.addAttribute("conf", cc);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/parametri-aziendali", principal.getUsername()));
        return "parametri-aziendali/list";
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }
}
