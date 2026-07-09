package com.aquarius.controller;

import com.aquarius.repository.tenant.CeeStructureDao;
import com.aquarius.repository.tenant.CeeStructureDao.CeeValueRow;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.context.FiscalContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bilancio CEE — VISTA VALORI (prospetto IV direttiva CEE).
 * Read-only: legge da BILNEW i valori (CORRENTE/PRECEDENTE) così come li ha
 * lasciati l'ultimo "Calcolo bilancio cee" eseguito sul gestionale. Il web NON
 * ricalcola (plug&play). La struttura/mappatura è nel viewer dedicato
 * (/contabilita/bilancio-cee-struttura).
 */
@Controller
@RequestMapping("/contabilita/bilancio-cee")
@RequiredArgsConstructor
public class CeeBalanceController {

    private final CeeStructureDao dao;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String view(Model model, @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();

        List<CeeValueRow> rows = dao.values(soc);

        // Ci sono valori? (se il gestionale non ha mai lanciato il calcolo, tutto 0/null)
        boolean hasValues = rows.stream().anyMatch(r ->
            (r.getCorrente() != null && r.getCorrente().signum() != 0) ||
            (r.getPrecedente() != null && r.getPrecedente().signum() != 0));

        model.addAttribute("rows", rows);
        model.addAttribute("hasValues", hasValues);
        model.addAttribute("anno", anno);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/bilancio-cee", principal.getUsername()));
        return "contabilita/bilancio-cee";
    }
}
