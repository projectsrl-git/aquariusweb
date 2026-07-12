package com.aquarius.controller;

import com.aquarius.entity.tenant.DepreciationQuota;
import com.aquarius.entity.tenant.FixedAsset;
import com.aquarius.entity.tenant.FixedAssetCategory;
import com.aquarius.entity.tenant.FixedAssetMovement;
import com.aquarius.repository.tenant.DepreciationQuotaRepository;
import com.aquarius.repository.tenant.FixedAssetCategoryRepository;
import com.aquarius.repository.tenant.FixedAssetMovementRepository;
import com.aquarius.repository.tenant.FixedAssetRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registro cespiti — consultazione READ-ONLY: anagrafica (u_amm_at),
 * categorie (u_amm_ca), quote di ammortamento per anno (U_QUO_AM) e
 * movimenti contabili collegati (u_amm_ad, con drill verso la prima
 * nota). Le gestioni (categorie, anagrafica, quote, trasferimenti in
 * coge) restano sul gestionale: logica fiscale riservata a Opus.
 * NOTA (verificata nei sorgenti): l'archivio cespiti NON ha la
 * dimensione societa' — archivio unico per installazione.
 */
@Controller
@RequestMapping("/cespiti")
@RequiredArgsConstructor
@Slf4j
public class FixedAssetController {

    private static final Set<String> SORTABLE = Set.of(
        "code", "description", "categoryCode", "inServiceDate",
        "historicalValue", "totalDepreciated", "residualValue");

    private final FixedAssetRepository assetRepository;
    private final FixedAssetCategoryRepository categoryRepository;
    private final DepreciationQuotaRepository quotaRepository;
    private final FixedAssetMovementRepository movementRepository;
    private final BreadcrumbService breadcrumbService;

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "cat", required = false) String cat,
                       @RequestParam(value = "stato", required = false) String stato,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "dir", required = false) String dir,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        String qq = q == null ? "" : q.trim();
        String cc = cat == null ? "" : cat.trim();
        String ss = stato == null ? "" : stato.trim().toLowerCase();
        if (!Set.of("", "attivi", "ceduti").contains(ss)) ss = "";
        // ordine legacy di default: categoria + codice
        ListParams lp = ListParams.of(page, size, sort, dir, SORTABLE, "code", "asc");
        Page<FixedAsset> result = assetRepository.search(qq, cc, ss, lp.toPageable());

        List<FixedAssetCategory> categorie = categoryRepository.findAllOrdered();
        Map<String, String> catDesc = categorie.stream().collect(Collectors.toMap(
            c -> c.getCode() == null ? "" : c.getCode().trim(),
            c -> c.getDescription1() == null ? "" : c.getDescription1().trim(),
            (a, b) -> a));

        model.addAttribute("cespiti", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("categorie", categorie);
        model.addAttribute("catDesc", catDesc);
        model.addAttribute("q", qq);
        model.addAttribute("cat", cc);
        model.addAttribute("stato", ss);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/cespiti", principal.getUsername()));
        return "cespiti/list";
    }

    @GetMapping("/categorie")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String categorie(Model model,
                            @AuthenticationPrincipal AquariusPrincipal principal) {
        List<FixedAssetCategory> categorie = categoryRepository.findAllOrdered();
        // riepilogo per categoria: [codcat, count, valsto, totamm, valres]
        Map<String, Object[]> summary = assetRepository.summaryByCategory().stream()
            .collect(Collectors.toMap(
                r -> r[0] == null ? "" : r[0].toString().trim(),
                r -> r, (a, b) -> a));
        model.addAttribute("categorie", categorie);
        model.addAttribute("summary", summary);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/cespiti/categorie", principal.getUsername()));
        return "cespiti/categorie";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        FixedAsset asset = assetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cespite non trovato: " + id));
        String code = asset.getCode() == null ? "" : asset.getCode().trim();
        List<DepreciationQuota> quote = code.isEmpty() ? List.of()
            : quotaRepository.findByAsset(asset.getCode());
        List<FixedAssetMovement> movimenti = code.isEmpty() ? List.of()
            : movementRepository.findByAsset(asset.getCode());

        BigDecimal totQuote = quote.stream()
            .map(x -> x.getTotalDepreciated() == null ? BigDecimal.ZERO : x.getTotalDepreciated())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cespite", asset);
        model.addAttribute("quote", quote);
        model.addAttribute("movimenti", movimenti);
        model.addAttribute("totQuote", totQuote);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/cespiti", principal.getUsername()));
        crumbs.add(new Crumb(asset.getDescription() == null ? code
            : asset.getDescription().trim(), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "cespiti/detail";
    }
}
