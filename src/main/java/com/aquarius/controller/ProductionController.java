package com.aquarius.controller;

import com.aquarius.entity.tenant.ParameterItem;
import com.aquarius.entity.tenant.ProductionProgram;
import com.aquarius.repository.tenant.ParameterRepository;
import com.aquarius.repository.tenant.ProductionComponentRepository;
import com.aquarius.repository.tenant.ProductionOrderRefRepository;
import com.aquarius.repository.tenant.ProductionProgramRepository;
import com.aquarius.repository.tenant.ProductionProgressRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Produzione STANDARD — consultazione read-only dei programmi di produzione
 * (PRODUZIONE con PARENT='' AND TIPO='STD', filtro verificato in
 * STD_PROGRAMMAZIONE) con ordini collegati (PROD_ORDINI), componenti
 * (PROD_LEGAMI) e avanzamenti (PROD_AVANZA), tutti agganciati per IDPRG.
 * SOLO la produzione standard: le altre varianti (generica, molle, tessuti,
 * lavorazioni PRODBOBI/PRODPEDANA/PRODSPAL/PRODMACC) sono escluse per
 * mandato. Registrazione avanzamenti, impegni/disimpegni e movimentazioni
 * restano sul gestionale.
 */
@Controller
@RequestMapping("/produzione")
@RequiredArgsConstructor
@Slf4j
public class ProductionController {

    private static final Set<String> SORTABLE = Set.of(
        "programNumber", "programDate", "articleCode", "quantity",
        "plannedStart", "plannedEnd");

    private final ProductionProgramRepository programRepository;
    private final ProductionOrderRefRepository orderRefRepository;
    private final ProductionComponentRepository componentRepository;
    private final ProductionProgressRepository progressRepository;
    private final ParameterRepository parameterRepository;
    private final BreadcrumbService breadcrumbService;

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
            "programDate", "desc");
        Page<ProductionProgram> result = programRepository.searchStandard(q, lp.toPageable());
        model.addAttribute("programmi", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/produzione", principal.getUsername()));
        return "produzione/list";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        ProductionProgram program = programRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Programma non trovato: " + id));
        String idprg = program.getProgramId();
        model.addAttribute("programma", program);
        model.addAttribute("ordini",
            idprg == null ? List.of() : orderRefRepository.findByProgram(idprg));
        model.addAttribute("componenti",
            idprg == null ? List.of() : componentRepository.findByProgram(idprg));
        model.addAttribute("avanzamenti",
            idprg == null ? List.of() : progressRepository.findByProgram(idprg));
        model.addAttribute("gruppiProduzione", prdDescriptions());
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/produzione", principal.getUsername()));
        crumbs.add(new Crumb("Programma " +
            (program.getProgramNumber() == null ? id : program.getProgramNumber().trim()), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "produzione/detail";
    }

    /** Decodifica gruppi di produzione: PARA 'PRD'+codice → DESCRI (legacy seek_para). */
    protected Map<String, String> prdDescriptions() {
        Map<String, String> map = new LinkedHashMap<>();
        for (ParameterItem p : parameterRepository.findByPrefix("PRD")) {
            String code = p.getCodice() == null ? "" : p.getCodice().trim();
            if (code.length() > 3) {
                map.put(code.substring(3),
                    p.getDescri() == null ? "" : p.getDescri().trim());
            }
        }
        return map;
    }
}
