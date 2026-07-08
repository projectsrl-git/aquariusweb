package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.dto.magazzino.GiacenzaRiga;
import com.aquarius.entity.tenant.ParameterItem;
import com.aquarius.entity.tenant.WarehouseMovement;
import com.aquarius.repository.tenant.ParameterRepository;
import com.aquarius.repository.tenant.StockBalanceDao;
import com.aquarius.repository.tenant.StockBalanceDao.SortKey;
import com.aquarius.repository.tenant.WarehouseMovementRepository;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.web.ListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Magazzino — consultazione read-only di movimenti (U_MAG_MO) e giacenze
 * correnti (U_MAG_GG). Controparti VFP: menu_movimenti_mag e menu_giacenze.
 * La valorizzazione e la ricostruzione storica NON vivono qui (FIFO gia'
 * esistente in /magazzino/valorizzazione; il resto e' riservato a Opus).
 */
@Controller
@RequestMapping("/magazzino")
@RequiredArgsConstructor
@Slf4j
public class WarehouseController {

    private static final java.util.Set<String> MOV_SORTABLE = java.util.Set.of(
        "registrationDate", "documentDate", "documentNumber", "articleCode",
        "quantity", "top", "warehouseCode");

    private final WarehouseMovementRepository movementRepository;
    private final StockBalanceDao stockBalanceDao;
    private final ParameterRepository parameterRepository;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    // ─── Movimenti ───────────────────────────────────────────────────────────

    @GetMapping("/movimenti")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String movimenti(@RequestParam(value = "q", required = false) String q,
                            @RequestParam(value = "page", required = false) Integer page,
                            @RequestParam(value = "size", required = false) Integer size,
                            @RequestParam(value = "sort", required = false) String sort,
                            @RequestParam(value = "dir", required = false) String dir,
                            Model model,
                            @AuthenticationPrincipal AquariusPrincipal principal) {
        ListParams lp = ListParams.of(page, size, sort, dir, MOV_SORTABLE,
            "registrationDate", "desc");
        Page<WarehouseMovement> result = movementRepository.search(
            fiscalContext.getSocietyCode(), fiscalContext.getFiscalYear(),
            q, lp.toPageable());
        model.addAttribute("movimenti", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("topDescriptions", topDescriptions());
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/magazzino/movimenti", principal.getUsername()));
        return "magazzino/movimenti";
    }

    // ─── Giacenze ────────────────────────────────────────────────────────────

    @GetMapping("/giacenze")
    public String giacenze(@RequestParam(value = "q", required = false) String q,
                           @RequestParam(value = "tutte", required = false, defaultValue = "false") boolean tutte,
                           @RequestParam(value = "page", required = false) Integer page,
                           @RequestParam(value = "size", required = false) Integer size,
                           @RequestParam(value = "sort", required = false) String sort,
                           @RequestParam(value = "dir", required = false) String dir,
                           Model model,
                           @AuthenticationPrincipal AquariusPrincipal principal) {
        int pageEff = page == null || page < 0 ? 0 : page;
        int sizeEff = size == null || !ListParams.PAGE_SIZE_OPTIONS.contains(size) ? 20 : size;
        SortKey sortKey = SortKey.from(sort);
        boolean asc = !"desc".equalsIgnoreCase(dir); // default asc su articolo
        boolean soloNonZero = !tutte;

        String soc = fiscalContext.getSocietyCode();
        String qEff = q == null ? "" : q.trim();
        long total = stockBalanceDao.count(soc, qEff, soloNonZero);
        List<GiacenzaRiga> content = stockBalanceDao.aggregate(
            soc, qEff, soloNonZero, sortKey, asc, pageEff, sizeEff);
        Page<GiacenzaRiga> result =
            new PageImpl<>(content, PageRequest.of(pageEff, sizeEff), total);

        model.addAttribute("giacenze", result);
        model.addAttribute("q", qEff);
        model.addAttribute("tutte", tutte);
        model.addAttribute("size", sizeEff);
        model.addAttribute("sort", sortKey.name());
        model.addAttribute("dir", asc ? "asc" : "desc");
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/magazzino/giacenze", principal.getUsername()));
        return "magazzino/giacenze";
    }

    /** Decodifica causali movimento: PARA 'TOP'+codice → DESCRI (regola TOP). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    protected Map<String, String> topDescriptions() {
        Map<String, String> map = new LinkedHashMap<>();
        for (ParameterItem p : parameterRepository.findByPrefix("TOP")) {
            String code = p.getCodice() == null ? "" : p.getCodice().trim();
            if (code.length() > 3) {
                map.put(code.substring(3),
                    p.getDescri() == null ? "" : p.getDescri().trim());
            }
        }
        return map;
    }
}
