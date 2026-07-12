package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.repository.tenant.VenditeStatsDao;
import com.aquarius.repository.tenant.VenditeStatsDao.Bucket;
import com.aquarius.repository.tenant.VenditeStatsDao.YearMonthCell;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.service.VenditeStatsExcelExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Statistiche fatturato vendite (read-only) — equivalente web di
 * menu_fatturato / "Analisi fatturato su piu' anni": per mese, per
 * cliente, per articolo, confronto pluriennale. Misura principale =
 * imponibile di testata; per articolo = valore riga (ORD_VALORE, netto
 * sconti). Le note di accredito non sono compensate.
 */
@Controller
@RequestMapping("/vendite/statistiche")
@RequiredArgsConstructor
@Slf4j
public class VenditeStatsController {

    private static final int TOP = 100;
    private static final int YEARS_BACK = 5;

    private final VenditeStatsDao dao;
    private final VenditeStatsExcelExporter excelExporter;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    @GetMapping
    public String view(@RequestParam(value = "meseDa", required = false) String meseDa,
                       @RequestParam(value = "meseA", required = false) String meseA,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        String da = normalizeMonth(meseDa);
        String a = normalizeMonth(meseA);

        List<Bucket> byMonth = dao.byMonth(soc, anno, da, a);
        List<Bucket> byCustomer = dao.byCustomer(soc, anno, da, a, TOP);
        List<Bucket> byArticle = dao.byArticle(soc, anno, da, a, TOP);
        List<Bucket> byYear = dao.byYear(soc);
        List<YearMonthCell> matrixCells = dao.yearMonthMatrix(soc, YEARS_BACK);

        // matrice anno -> (mese -> imponibile)
        Map<String, Map<String, BigDecimal>> matrix = new TreeMap<>();
        for (YearMonthCell c : matrixCells) {
            matrix.computeIfAbsent(c.getAnno(), k -> new LinkedHashMap<>())
                  .put(c.getMese(), c.getImponibile());
        }

        BigDecimal totImponibile = byMonth.stream()
            .map(b -> b.getImponibile() == null ? BigDecimal.ZERO : b.getImponibile())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totFatture = byMonth.stream().mapToLong(Bucket::getDocumenti).sum();

        model.addAttribute("byMonth", byMonth);
        model.addAttribute("byCustomer", byCustomer);
        model.addAttribute("byArticle", byArticle);
        model.addAttribute("byYear", byYear);
        model.addAttribute("matrix", matrix);
        model.addAttribute("totImponibile", totImponibile);
        model.addAttribute("totFatture", totFatture);
        model.addAttribute("top", TOP);
        model.addAttribute("meseDa", da);
        model.addAttribute("meseA", a);
        model.addAttribute("anno", anno);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/vendite/statistiche", principal.getUsername()));
        return "vendite/statistiche";
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(@RequestParam(value = "meseDa", required = false) String meseDa,
                                        @RequestParam(value = "meseA", required = false) String meseA)
            throws Exception {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        String da = normalizeMonth(meseDa);
        String a = normalizeMonth(meseA);
        byte[] bytes = excelExporter.export(anno,
            dao.byMonth(soc, anno, da, a),
            dao.byCustomer(soc, anno, da, a, TOP),
            dao.byArticle(soc, anno, da, a, TOP));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=statistiche-vendite-" + anno + ".xlsx")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    private static String normalizeMonth(String m) {
        if (m == null || m.isBlank()) return "";
        String s = m.trim();
        if (s.length() == 1) s = "0" + s;
        return s.matches("0[1-9]|1[0-2]") ? s : "";
    }
}
