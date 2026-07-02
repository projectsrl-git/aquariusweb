package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.service.AppVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Year;

/**
 * Inietta variabili globali nel Model di ogni view Thymeleaf, così template
 * come layout.html possono accedere a {@code ${appVersion}}, {@code ${currentFiscalYear}}
 * ecc. senza che ogni controller debba popolare il valore esplicitamente.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final AppVersionService appVersionService;
    private final FiscalContext fiscalContext;

    @ModelAttribute("appVersion")
    public String appVersion() {
        return appVersionService.getCurrentVersion();
    }

    @ModelAttribute("appBuildTime")
    public String appBuildTime() {
        return appVersionService.getBuildTime();
    }

    @ModelAttribute("currentYear")
    public int currentYear() {
        return Year.now().getValue();
    }

    /** Anno contabile selezionato (es. "2026"), o null se non scelto. */
    @ModelAttribute("currentFiscalYear")
    public String currentFiscalYear() {
        return fiscalContext.isSet() ? fiscalContext.getFiscalYear() : null;
    }

    @ModelAttribute("currentFiscalYearDescription")
    public String currentFiscalYearDescription() {
        return fiscalContext.isSet() ? fiscalContext.getFiscalYearDescription() : null;
    }
}
