package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.entity.tenant.SupplierDdtHead;
import com.aquarius.entity.tenant.SupplierDdtRow;
import com.aquarius.repository.tenant.SupplierDdtHeadRepository;
import com.aquarius.repository.tenant.SupplierDdtRowRepository;
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
 * Documenti di carico da fornitore (entrata merce) — read-only consultation
 * of U_BFO_TT / U_BFO_DD, mirror of the sales DDT module. VFP: MENU_BFO000
 * ("Carico da fornitore"). ARCHIVE NOTE: the handoff mentioned U_BOF_TT,
 * but sources show U_BOF_* is the customer-facing "bollette fiscali" flow;
 * the supplier-inbound archive is U_BFO_* (used here, and already used by
 * the Ristampa dashboard types BFO/RDC). ORD_TIPO=9 rows are "resi da
 * clienti" and get a dedicated badge instead of being hidden.
 */
@Controller
@RequestMapping("/ddt-fornitore")
@RequiredArgsConstructor
@Slf4j
public class SupplierDdtController {

    private static final Set<String> SORTABLE = Set.of(
        "documentNumber", "documentDate", "supplierCode", "supplierName",
        "documentValue");

    private final SupplierDdtHeadRepository headRepository;
    private final SupplierDdtRowRepository rowRepository;
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
        ListParams lp = ListParams.of(page, size, sort, dir, SORTABLE, "documentDate", "desc");
        Page<SupplierDdtHead> result = headRepository.search(
            fiscalContext.getSocietyCode(), fiscalContext.getFiscalYear(), q, lp.toPageable());
        model.addAttribute("documents", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/ddt-fornitore", principal.getUsername()));
        return "ddt-fornitore/list";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        SupplierDdtHead head = headRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Documento non trovato: " + id));
        List<SupplierDdtRow> rows =
            (head.getAggancio() != null && !head.getAggancio().trim().isEmpty())
                ? rowRepository.findByAggancio(head.getAggancio())
                : List.of();
        model.addAttribute("doc", head);
        model.addAttribute("rows", rows);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/ddt-fornitore", principal.getUsername()));
        crumbs.add(new Crumb("Carico " +
            (head.getDocumentNumber() == null ? id : head.getDocumentNumber().trim()), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "ddt-fornitore/detail";
    }
}
