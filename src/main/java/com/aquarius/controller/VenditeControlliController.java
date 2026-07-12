package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.repository.tenant.VenditeControlliDao;
import com.aquarius.repository.tenant.VenditeControlliDao.DuplicateNumber;
import com.aquarius.repository.tenant.VenditeControlliDao.SequenceGap;
import com.aquarius.repository.tenant.VenditeControlliDao.UnpairedDdt;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controlli documenti vendite (read-only): DDT non/parzialmente
 * fatturati, sequenza numeri fattura (buchi/duplicati/non numerici),
 * sequenza protocolli del registro IVA vendite. Solo evidenza delle
 * anomalie (pattern pannello-anomalie): nessuna correzione dati.
 */
@Controller
@RequestMapping("/vendite/controlli")
@RequiredArgsConstructor
public class VenditeControlliController {

    private final VenditeControlliDao dao;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    @GetMapping
    public String view(Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();

        List<UnpairedDdt> ddt = dao.unpairedDdt(soc, anno);
        List<SequenceGap> gaps = dao.invoiceNumberGaps(soc, anno);
        List<DuplicateNumber> duplicates = dao.invoiceNumberDuplicates(soc, anno);
        List<String> nonNumeric = dao.invoiceNumberNonNumeric(soc, anno);
        List<SequenceGap> ivaGaps = dao.ivaProtocolGaps(soc, anno);

        long mancantiTot = gaps.stream().mapToLong(SequenceGap::getMancanti).sum();

        model.addAttribute("ddt", ddt);
        model.addAttribute("gaps", gaps);
        model.addAttribute("duplicates", duplicates);
        model.addAttribute("nonNumeric", nonNumeric);
        model.addAttribute("ivaGaps", ivaGaps);
        model.addAttribute("mancantiTot", mancantiTot);
        model.addAttribute("anno", anno);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/vendite/controlli", principal.getUsername()));
        return "vendite/controlli";
    }
}
