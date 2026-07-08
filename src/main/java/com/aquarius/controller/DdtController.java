package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.entity.tenant.DdtHead;
import com.aquarius.entity.tenant.DdtRow;
import com.aquarius.repository.tenant.DdtHeadRepository;
import com.aquarius.repository.tenant.DdtRowRepository;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.service.BreadcrumbService.Crumb;
import com.aquarius.web.ListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * DDT / documenti di trasporto — read-only consultation of U_BOL_TT / U_BOL_DD.
 * Web counterpart of the VFP form menu_BOL000 ("Documenti di trasporto").
 * Scoped to the current society + fiscal year (FiscalContext ≙ PUB_ANNO).
 */
@Controller
@RequestMapping("/ddt")
@RequiredArgsConstructor
@Slf4j
public class DdtController {

    private static final Set<String> SORTABLE = Set.of(
        "ddtNumber", "ddtDate", "customerCode", "customerName",
        "causale", "documentValue");

    private final DdtHeadRepository ddtHeadRepository;
    private final DdtRowRepository ddtRowRepository;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "dir", required = false) String dir,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        ListParams lp = ListParams.of(page, size, sort, dir, SORTABLE, "ddtDate", "desc");
        Page<DdtHead> result = ddtHeadRepository.search(
            fiscalContext.getSocietyCode(), fiscalContext.getFiscalYear(),
            q, lp.toPageable());
        model.addAttribute("ddts", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/ddt", principal.getUsername()));
        return "ddt/list";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        DdtHead head = ddtHeadRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("DDT non trovato: " + id));
        List<DdtRow> rows = (head.getAggancio() != null && !head.getAggancio().trim().isEmpty())
            ? ddtRowRepository.findByAggancio(head.getAggancio())
            : List.of();
        model.addAttribute("ddt", head);
        model.addAttribute("rows", rows);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/ddt", principal.getUsername()));
        crumbs.add(new Crumb("DDT " +
            (head.getDdtNumber() == null ? id : head.getDdtNumber().trim()), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "ddt/detail";
    }
}
