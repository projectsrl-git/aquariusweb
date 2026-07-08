package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.entity.tenant.OrderHead;
import com.aquarius.entity.tenant.OrderRow;
import com.aquarius.repository.tenant.OrderHeadRepository;
import com.aquarius.repository.tenant.OrderRowRepository;
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
 * Ordini clienti — read-only consultation of U_ORD_TT / U_ORD_DD.
 * Web counterpart of the VFP form MENU_ORD000 ("Ordini clienti").
 * Scoped to the current society + fiscal year (FiscalContext ≙ PUB_ANNO),
 * as the legacy client does.
 */
@Controller
@RequestMapping("/ordini")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private static final Set<String> SORTABLE = Set.of(
        "orderNumber", "orderDate", "customerCode", "customerName",
        "deliveryDate", "taxableAmount");

    private final OrderHeadRepository orderHeadRepository;
    private final OrderRowRepository orderRowRepository;
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
        Page<OrderHead> result = orderHeadRepository.search(
            fiscalContext.getSocietyCode(), fiscalContext.getFiscalYear(),
            q, lp.toPageable());
        model.addAttribute("orders", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/ordini", principal.getUsername()));
        return "ordini/list";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        OrderHead head = orderHeadRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Ordine non trovato: " + id));
        // Primary link: TAGGANCIO/DAGGANCIO hook (modern legacy SQL).
        // Fallback for old records with empty hook: triple key (ristampelib).
        List<OrderRow> rows = (head.getAggancio() != null && !head.getAggancio().trim().isEmpty())
            ? orderRowRepository.findByAggancio(head.getAggancio())
            : List.of();
        if (rows.isEmpty()) {
            rows = orderRowRepository.findRows(
                head.getOrderDate(), head.getOrderNumber(), head.getCustomerCode());
        }
        model.addAttribute("order", head);
        model.addAttribute("rows", rows);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/ordini", principal.getUsername()));
        crumbs.add(new Crumb("Ordine " +
            (head.getOrderNumber() == null ? id : head.getOrderNumber().trim()), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "ordini/detail";
    }
}
