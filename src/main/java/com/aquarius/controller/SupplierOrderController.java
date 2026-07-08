package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.entity.tenant.SupplierOrderHead;
import com.aquarius.entity.tenant.SupplierOrderRow;
import com.aquarius.repository.tenant.SupplierOrderHeadRepository;
import com.aquarius.repository.tenant.SupplierOrderRowRepository;
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
 * Ordini a fornitore — read-only consultation of U_ORF_TT / U_ORF_DD,
 * mirror of the sales module (/ordini). VFP: MENU_ORF000
 * ("Aggiornamento ordini di acquisto"). The same legacy form also handles
 * "proposte ordini" (launch flag) without a visible discriminator column:
 * this module lists the whole archive (NEEDS_DOMAIN in the tracker).
 */
@Controller
@RequestMapping("/ordini-fornitore")
@RequiredArgsConstructor
@Slf4j
public class SupplierOrderController {

    private static final Set<String> SORTABLE = Set.of(
        "orderNumber", "orderDate", "supplierCode", "supplierName",
        "deliveryDate", "taxableAmount");

    private final SupplierOrderHeadRepository headRepository;
    private final SupplierOrderRowRepository rowRepository;
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
        ListParams lp = ListParams.of(page, size, sort, dir, SORTABLE, "orderDate", "desc");
        Page<SupplierOrderHead> result = headRepository.search(
            fiscalContext.getSocietyCode(), fiscalContext.getFiscalYear(), q, lp.toPageable());
        model.addAttribute("orders", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/ordini-fornitore", principal.getUsername()));
        return "ordini-fornitore/list";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        SupplierOrderHead head = headRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Ordine fornitore non trovato: " + id));
        List<SupplierOrderRow> rows =
            (head.getAggancio() != null && !head.getAggancio().trim().isEmpty())
                ? rowRepository.findByAggancio(head.getAggancio())
                : List.of();
        model.addAttribute("order", head);
        model.addAttribute("rows", rows);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/ordini-fornitore", principal.getUsername()));
        crumbs.add(new Crumb("Ordine " +
            (head.getOrderNumber() == null ? id : head.getOrderNumber().trim()), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "ordini-fornitore/detail";
    }
}
