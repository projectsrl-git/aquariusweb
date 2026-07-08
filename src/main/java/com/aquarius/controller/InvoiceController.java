package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.entity.tenant.InvoiceHead;
import com.aquarius.entity.tenant.InvoiceRow;
import com.aquarius.repository.tenant.InvoiceHeadRepository;
import com.aquarius.repository.tenant.InvoiceRowRepository;
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
 * Fatture di vendita — read-only consultation of U_FAT_TT / U_FAT_DD.
 * Web counterpart of the VFP form MENU_FAT000 ("Fatture di vendita").
 * Scoped to the current society + fiscal year (FiscalContext ≙ PUB_ANNO).
 */
@Controller
@RequestMapping("/fatture")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private static final Set<String> SORTABLE = Set.of(
        "invoiceNumber", "invoiceDate", "customerCode", "customerName",
        "documentType", "taxableAmount");

    private final InvoiceHeadRepository invoiceHeadRepository;
    private final InvoiceRowRepository invoiceRowRepository;
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
        Page<InvoiceHead> result = invoiceHeadRepository.search(
            fiscalContext.getSocietyCode(), fiscalContext.getFiscalYear(),
            q, lp.toPageable());
        model.addAttribute("invoices", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/fatture", principal.getUsername()));
        return "fatture/list";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        InvoiceHead head = invoiceHeadRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Fattura non trovata: " + id));
        List<InvoiceRow> rows = (head.getAggancio() != null && !head.getAggancio().trim().isEmpty())
            ? invoiceRowRepository.findByAggancio(head.getAggancio())
            : List.of();
        model.addAttribute("invoice", head);
        model.addAttribute("rows", rows);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/fatture", principal.getUsername()));
        crumbs.add(new Crumb("Fattura " +
            (head.getInvoiceNumber() == null ? id : head.getInvoiceNumber().trim()), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "fatture/detail";
    }
}
