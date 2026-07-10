package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.repository.tenant.IvaRegisterDao;
import com.aquarius.repository.tenant.IvaRegisterDao.AliquotaTotal;
import com.aquarius.repository.tenant.IvaRegisterDao.RegisterRow;
import com.aquarius.repository.tenant.IvaRegisterDao.SortKey;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Consultazione READ-ONLY dei registri IVA ("bollati"): vendite,
 * acquisti e corrispettivi, con filtro periodo e totali per aliquota
 * dell'insieme filtrato. Equivalente web della "Stampa bollati IVA";
 * il CARICAMENTO dei bollati (BOLLATI.PRG, da prima nota) e gli
 * adempimenti restano sul gestionale.
 *
 * NB (verificato in BOLLATI.PRG): le righe sono rigenerate per
 * societa'/anno/mese a ogni caricamento legacy; questa vista mostra i
 * registri come caricati l'ultima volta per ciascun mese.
 */
@Controller
@RequestMapping("/contabilita/registri-iva")
@RequiredArgsConstructor
@Slf4j
public class IvaRegisterController {

    private static final Set<String> REGISTERS = Set.of("vendite", "acquisti", "corrispettivi");
    private static final List<Integer> PAGE_SIZES = List.of(20, 50, 100, 200);

    private final IvaRegisterDao dao;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    @GetMapping
    public String index() {
        return "redirect:/contabilita/registri-iva/vendite";
    }

    @GetMapping("/{registro}")
    public String view(@PathVariable String registro,
                       @RequestParam(value = "mese", required = false) String mese,
                       @RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "dir", required = false) String dir,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        if (!REGISTERS.contains(registro)) {
            return "redirect:/contabilita/registri-iva/vendite";
        }
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        String mm = normalizeMonth(mese);
        String qq = q == null ? "" : q.trim();
        int pg = page == null || page < 0 ? 0 : page;
        int sz = size == null || !PAGE_SIZES.contains(size) ? 50 : size;
        SortKey sk = SortKey.of(sort, SortKey.PROTOCOLLO);
        boolean asc = !"desc".equalsIgnoreCase(dir);

        long total;
        List<RegisterRow> rows;
        List<AliquotaTotal> totals;
        switch (registro) {
            case "acquisti" -> {
                total = dao.countPurchases(soc, anno, mm, qq);
                rows = dao.purchases(soc, anno, mm, qq, sk, asc, pg, sz);
                totals = dao.purchaseTotalsByAliquota(soc, anno, mm, qq);
            }
            case "corrispettivi" -> {
                total = dao.countSales(soc, anno, mm, qq, true);
                rows = dao.sales(soc, anno, mm, qq, true, sk, asc, pg, sz);
                totals = dao.salesTotalsByAliquota(soc, anno, mm, qq, true);
            }
            default -> {
                total = dao.countSales(soc, anno, mm, qq, false);
                rows = dao.sales(soc, anno, mm, qq, false, sk, asc, pg, sz);
                totals = dao.salesTotalsByAliquota(soc, anno, mm, qq, false);
            }
        }

        BigDecimal totImponibile = totals.stream()
            .map(t -> t.getImponibile() == null ? BigDecimal.ZERO : t.getImponibile())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totImposta = totals.stream()
            .map(t -> t.getImposta() == null ? BigDecimal.ZERO : t.getImposta())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int lastPage = total == 0 ? 0 : (int) ((total - 1) / sz);

        model.addAttribute("registro", registro);
        model.addAttribute("rows", rows);
        model.addAttribute("totals", totals);
        model.addAttribute("totImponibile", totImponibile);
        model.addAttribute("totImposta", totImposta);
        model.addAttribute("total", total);
        model.addAttribute("mese", mm);
        model.addAttribute("q", qq);
        model.addAttribute("page", pg);
        model.addAttribute("size", sz);
        model.addAttribute("sizes", PAGE_SIZES);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("sort", sk.name().toLowerCase());
        model.addAttribute("dir", asc ? "asc" : "desc");
        model.addAttribute("anno", anno);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/registri-iva/" + registro,
                principal.getUsername()));
        return "contabilita/registro-iva";
    }

    /** Accetta '1'..'12' o '01'..'12'; vuoto = intero anno. */
    private static String normalizeMonth(String m) {
        if (m == null || m.isBlank()) return "";
        String s = m.trim();
        if (s.length() == 1) s = "0" + s;
        return s.matches("0[1-9]|1[0-2]") ? s : "";
    }
}
