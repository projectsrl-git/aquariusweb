package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.entity.tenant.ProformaHead;
import com.aquarius.entity.tenant.ProformaRow;
import com.aquarius.repository.tenant.ProformaHeadRepository;
import com.aquarius.repository.tenant.ProformaRowRepository;
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
 * Fatture proforma — read-only consultation of U_FAP_TT / U_FAP_DD.
 * Web counterpart of the VFP form menu_FAP000 ("Fatture proforma").
 * Scoped to the current society + fiscal year (FiscalContext ≙ PUB_ANNO).
 */
@Controller
@RequestMapping("/proforma")
@RequiredArgsConstructor
@Slf4j
public class ProformaController {

    private static final Set<String> SORTABLE = Set.of(
        "invoiceNumber", "invoiceDate", "customerCode", "customerName",
        "documentType", "taxableAmount");

    private final ProformaHeadRepository proformaHeadRepository;
    private final ProformaRowRepository proformaRowRepository;
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
        ListParams lp = ListParams.of(page, size, sort, dir, SORTABLE, "invoiceDate", "desc");
        Page<ProformaHead> result = proformaHeadRepository.search(
            fiscalContext.getSocietyCode(), fiscalContext.getFiscalYear(),
            q, lp.toPageable());
        model.addAttribute("invoices", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/proforma", principal.getUsername()));
        return "proforma/list";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        ProformaHead head = proformaHeadRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Fattura proforma non trovata: " + id));
        List<ProformaRow> rows = (head.getAggancio() != null && !head.getAggancio().trim().isEmpty())
            ? proformaRowRepository.findByAggancio(head.getAggancio())
            : List.of();
        model.addAttribute("invoice", head);
        model.addAttribute("rows", rows);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/proforma", principal.getUsername()));
        crumbs.add(new Crumb("Proforma " +
            (head.getInvoiceNumber() == null ? id : head.getInvoiceNumber().trim()), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "proforma/detail";
    }
}
