package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.service.FiscalYearService;
import com.aquarius.service.FiscalYearService.FiscalYear;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Pagina di scelta dell'anno contabile dopo il login.
 *
 * <p>Replica web del campo "Anno contabile" del form VFP {@code pass.scx}.
 * Mostra dropdown con tutti gli anni in PARA (prefisso ANN), default sull'anno
 * corrente. Una volta scelto, l'anno viene salvato nel {@link FiscalContext}
 * della sessione e l'utente può navigare nell'app.</p>
 */
@Controller
@RequestMapping("/select-year")
@RequiredArgsConstructor
@Slf4j
public class FiscalYearController {

    private final FiscalYearService fiscalYearService;
    private final FiscalContext fiscalContext;

    @GetMapping
    public String show(Model model) {
        List<FiscalYear> years = fiscalYearService.listAvailable();
        model.addAttribute("years", years);

        // Pre-seleziona l'anno corrente in sessione, oppure default = anno corrente
        String preselected = fiscalContext.getFiscalYear();
        if (preselected == null) {
            preselected = fiscalYearService.defaultYear().map(FiscalYear::getYear).orElse(null);
        }
        model.addAttribute("preselected", preselected);
        model.addAttribute("noYears", years.isEmpty());
        return "fiscal/select-year";
    }

    @PostMapping
    public String choose(@RequestParam("year") String year,
                         @RequestParam(value = "redirect", required = false) String redirect,
                         RedirectAttributes ra) {
        Optional<FiscalYear> yOpt = fiscalYearService.findByYear(year);
        if (yOpt.isEmpty()) {
            ra.addFlashAttribute("flashError", "Anno '" + year + "' non valido.");
            return "redirect:/select-year";
        }
        FiscalYear y = yOpt.get();
        fiscalContext.setFiscalYear(y.getYear());
        fiscalContext.setFiscalYearDescription(y.getDescription());
        log.info("FiscalContext aggiornato: anno={}", y.getYear());

        String target = (redirect != null && !redirect.isBlank() && redirect.startsWith("/"))
            ? redirect : "/dashboard";
        return "redirect:" + target;
    }
}
