package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.entity.tenant.BomHead;
import com.aquarius.entity.tenant.BomRow;
import com.aquarius.repository.tenant.BomHeadRepository;
import com.aquarius.repository.tenant.BomRowRepository;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Distinta base — consultazione read-only di U_DIS_TT / U_DIS_DD.
 * Il dettaglio mostra i componenti di PRIMO livello; per i componenti
 * marcati sotto-distinta (DIS_ESPLOD='X') c'e' il link "apri
 * sotto-distinta" (navigazione un livello alla volta). L'esplosione
 * ricorsiva multi-livello e il ricalcolo costi/prezzi NON vivono qui
 * (calcolo, non consultazione — riservati a slice future).
 */
@Controller
@RequestMapping("/distinte")
@RequiredArgsConstructor
@Slf4j
public class BomController {

    private static final Set<String> SORTABLE = Set.of(
        "parentArticleCode", "description", "unit", "cost", "price", "customerName");

    private final BomHeadRepository bomHeadRepository;
    private final BomRowRepository bomRowRepository;
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
        ListParams lp = ListParams.of(page, size, sort, dir, SORTABLE,
            "parentArticleCode", "asc");
        Page<BomHead> result = bomHeadRepository.search(
            fiscalContext.getSocietyCode(), q, lp.toPageable());
        model.addAttribute("distinte", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/distinte", principal.getUsername()));
        return "distinte/list";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        BomHead head = bomHeadRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Distinta non trovata: " + id));
        List<BomRow> rows =
            (head.getAggancio() != null && !head.getAggancio().trim().isEmpty())
                ? bomRowRepository.findByAggancio(head.getAggancio())
                : List.of();
        model.addAttribute("distinta", head);
        model.addAttribute("rows", rows);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/distinte", principal.getUsername()));
        crumbs.add(new Crumb(head.getParentArticleCode() == null
            ? id : head.getParentArticleCode().trim(), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "distinte/detail";
    }

    /**
     * Navigazione sotto-distinta: dal codice articolo componente alla sua
     * distinta (legacy: dit_gruppo = codice). Se non esiste, torna alla
     * lista filtrata sul codice.
     */
    @GetMapping("/articolo/{code}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String byArticle(@PathVariable String code, RedirectAttributes ra) {
        List<BomHead> found = bomHeadRepository.findByParentArticle(
            fiscalContext.getSocietyCode(), code == null ? "" : code.trim());
        if (found.size() == 1) {
            return "redirect:/distinte/" + found.get(0).getId();
        }
        ra.addAttribute("q", code == null ? "" : code.trim());
        return "redirect:/distinte";
    }
}
