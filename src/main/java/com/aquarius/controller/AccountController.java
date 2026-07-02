package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.dto.FormTab;
import com.aquarius.entity.tenant.Account;
import com.aquarius.repository.tenant.AccountRepository;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.AccountTreeService;
import com.aquarius.service.AccountTreeService.AccountNode;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.service.BreadcrumbService.Crumb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/conti")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountRepository repository;
    private final AccountTreeService treeService;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    private static final int PAGE_SIZE = 50;

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        Pageable pg = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        Page<Account> result = repository.searchByYearAndSociety(
            fiscalContext.getFiscalYear(),
            fiscalContext.getSocietyCode(),
            q, pg);
        model.addAttribute("accounts", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/conti", principal.getUsername()));
        return "conti/list";
    }

    /**
     * Vista ad albero del piano dei conti.
     */
    @GetMapping("/tree")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String tree(Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        List<AccountNode> roots = treeService.buildTree();
        model.addAttribute("roots", roots);
        model.addAttribute("totalCount", treeService.countNodes(roots));

        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/conti", principal.getUsername()));
        crumbs.add(new Crumb("Vista ad albero", null));
        model.addAttribute("breadcrumbs", crumbs);
        return "conti/tree";
    }

    /**
     * Dettaglio read-only di un singolo conto.
     */
    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        Account account = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Conto non trovato: " + id));
        model.addAttribute("account", account);

        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/conti", principal.getUsername()));
        crumbs.add(new Crumb(account.getCode(), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "conti/detail";
    }

    /**
     * Form di modifica del conto — 2 tab (Informazioni principali + Altri dati).
     */
    @GetMapping("/{id}/edit")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String edit(@PathVariable String id, Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        Account account = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Conto non trovato: " + id));
        model.addAttribute("account", account);
        model.addAttribute("formTabs", buildAccountFormTabs());

        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/conti", principal.getUsername()));
        crumbs.add(new Crumb(account.getCode(), "/conti/" + id));
        crumbs.add(new Crumb("Modifica", null));
        model.addAttribute("breadcrumbs", crumbs);
        return "conti/edit";
    }

    /** I 2 tab del form anagrafica conto, replica VFP. */
    private List<FormTab> buildAccountFormTabs() {
        return List.of(
            FormTab.builder().id("tab-info").label("Informazioni principali")
                   .icon("bi-card-text").active(true).build(),
            FormTab.builder().id("tab-altri").label("Altri dati")
                   .icon("bi-info-circle").build(),
            FormTab.builder().id("tab-saldi").label("Saldi")
                   .icon("bi-bar-chart").placeholder(true).build(),
            FormTab.builder().id("tab-mov").label("Movimenti")
                   .icon("bi-arrow-left-right").placeholder(true).build()
        );
    }

    /**
     * Salvataggio modifiche al conto. Solo i campi del form (descrizione,
     * tipo, posizione bilancio, flag, centri di costo, CEE, RAP/INPS,
     * ammortamenti). NON modificabili da web: code, society, year, gerarchia.
     */
    @PostMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager")
    public String save(@PathVariable String id,
                       @ModelAttribute("account") Account formData,
                       RedirectAttributes ra) {
        Account dbEntity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Conto non trovato: " + id));

        copyEditableFields(formData, dbEntity);

        try {
            repository.save(dbEntity);
            log.info("Conto {}/{} aggiornato da web (id={})",
                     dbEntity.getSocietyCode(), dbEntity.getCode(), id);
            ra.addFlashAttribute("flashSuccess",
                "Modifiche al conto " + dbEntity.getCode() + " salvate.");
        } catch (Exception e) {
            log.error("Errore salvando conto id={}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("flashError",
                "Errore durante il salvataggio: " + e.getMessage());
            return "redirect:/conti/" + id + "/edit";
        }
        return "redirect:/conti/" + id;
    }

    private void copyEditableFields(Account src, Account dst) {
        // Anagrafica base
        dst.setDescription(src.getDescription());
        // Classificazione
        dst.setAccountType(src.getAccountType());
        dst.setAccountSubType(src.getAccountSubType());
        dst.setBalancePosition(src.getBalancePosition());
        dst.setHasVat(src.getHasVat());
        dst.setVatNumber(src.getVatNumber());
        dst.setCurrency(src.getCurrency());
        dst.setEnabled(src.getEnabled());
        // Centri di costo
        dst.setCostCenterEnabled(src.getCostCenterEnabled());
        dst.setCostCenter0(src.getCostCenter0());
        dst.setCostCenter1(src.getCostCenter1());
        dst.setCostCenter2(src.getCostCenter2());
        dst.setCostCenter3(src.getCostCenter3());
        dst.setCostCenter4(src.getCostCenter4());
        dst.setCostCenterPercent1(src.getCostCenterPercent1());
        dst.setCostCenterPercent2(src.getCostCenterPercent2());
        dst.setCostCenterPercent3(src.getCostCenterPercent3());
        dst.setCostCenterPercent4(src.getCostCenterPercent4());
        dst.setCostCenterPercent5(src.getCostCenterPercent5());
        // Bilancio CEE
        dst.setCeeDareCode(src.getCeeDareCode());
        dst.setCeeAvereCode(src.getCeeAvereCode());
        dst.setSole24DareCode(src.getSole24DareCode());
        dst.setSole24AvereCode(src.getSole24AvereCode());
        dst.setAquariusGroup(src.getAquariusGroup());
        // R.A.P. / INPS
        dst.setIsAdvanceAccount(src.getIsAdvanceAccount());
        dst.setIsWelfareAccount(src.getIsWelfareAccount());
        dst.setIsStampDutyAccount(src.getIsStampDutyAccount());
        dst.setIsRapSubject(src.getIsRapSubject());
        dst.setIsRapAccount(src.getIsRapAccount());
        dst.setIsInpsSubject(src.getIsInpsSubject());
        dst.setIsInpsAccount(src.getIsInpsAccount());
        // Ammortamenti / Cespiti
        dst.setAmortizationCategory(src.getAmortizationCategory());
        dst.setAssetRegistryCode(src.getAssetRegistryCode());
        dst.setGeneralCategory(src.getGeneralCategory());
        // Altri
        dst.setNoOrderNumberCheck(src.getNoOrderNumberCheck());
    }
}
