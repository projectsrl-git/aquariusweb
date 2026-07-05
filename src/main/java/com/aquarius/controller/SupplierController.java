package com.aquarius.controller;

import com.aquarius.dto.FormTab;
import com.aquarius.entity.tenant.Supplier;
import com.aquarius.repository.tenant.SupplierRepository;
import com.aquarius.security.AquariusPrincipal;
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

/**
 * Anagrafica fornitori - replica web del form VFP MENU_FOR000.
 * Stesso framework dei clienti (form-shell + FormTab + whitelist di copia).
 * 7 tab attivi con campi verificati su U_FOR_AN.
 */
@Controller
@RequestMapping("/fornitori")
@RequiredArgsConstructor
@Slf4j
public class SupplierController {

    private final SupplierRepository supplierRepository;
    private final BreadcrumbService breadcrumbService;

    private static final int PAGE_SIZE = 50;

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        Pageable pageable = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        Page<Supplier> result = supplierRepository.search(q, pageable);
        model.addAttribute("suppliers", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/fornitori", principal.getUsername()));
        return "fornitori/list";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        Supplier s = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Fornitore non trovato: " + id));
        model.addAttribute("supplier", s);
        List<Crumb> crumbs = new ArrayList<>(breadcrumbService.forUrl("/fornitori", principal.getUsername()));
        crumbs.add(new Crumb(s.getCode(), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "fornitori/detail";
    }

    @GetMapping("/{id}/edit")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String edit(@PathVariable String id, Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        Supplier s = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Fornitore non trovato: " + id));
        model.addAttribute("supplier", s);
        List<Crumb> crumbs = new ArrayList<>(breadcrumbService.forUrl("/fornitori", principal.getUsername()));
        crumbs.add(new Crumb(s.getCode(), "/fornitori/" + id));
        crumbs.add(new Crumb("Modifica", null));
        model.addAttribute("breadcrumbs", crumbs);
        model.addAttribute("formTabs", buildSupplierFormTabs());
        return "fornitori/edit";
    }

    /** Definizione dei tab del form anagrafica fornitore, replica VFP MENU_FOR000. */
    private List<FormTab> buildSupplierFormTabs() {
        return List.of(
            FormTab.builder().id("tab-anag")  .label("Anagrafica")     .icon("bi-card-text").active(true).build(),
            FormTab.builder().id("tab-indir") .label("Indirizzo")      .icon("bi-geo-alt").build(),
            FormTab.builder().id("tab-iva")   .label("IVA")            .icon("bi-receipt").build(),
            FormTab.builder().id("tab-comm")  .label("Commerciali")    .icon("bi-cart").build(),
            FormTab.builder().id("tab-contab").label("Dati contabili") .icon("bi-calculator").build(),
            FormTab.builder().id("tab-banca") .label("Dati bancari")   .icon("bi-bank").build(),
            FormTab.builder().id("tab-rifforn").label("Rif. fornitore").icon("bi-people").build()
        );
    }

    @PostMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager")
    public String save(@PathVariable String id,
                       @ModelAttribute("supplier") Supplier formData,
                       RedirectAttributes ra) {
        Supplier dbEntity = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Fornitore non trovato: " + id));
        copyEditableFields(formData, dbEntity);
        try {
            supplierRepository.save(dbEntity);
            log.info("Fornitore {}/{} aggiornato da web (id={})",
                     dbEntity.getSocietyCode(), dbEntity.getCode(), id);
            ra.addFlashAttribute("flashSuccess",
                "Modifiche al fornitore " + dbEntity.getCode() + " salvate.");
        } catch (Exception e) {
            log.error("Errore salvando fornitore id={}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("flashError",
                "Errore durante il salvataggio: " + e.getMessage());
            return "redirect:/fornitori/" + id + "/edit";
        }
        return "redirect:/fornitori/" + id;
    }

    /** Patch esplicito dei campi modificabili (whitelist, come per i clienti). */
    private void copyEditableFields(Supplier src, Supplier dst) {
        // tab-anag
        dst.setBusinessName(src.getBusinessName());
        dst.setBusinessName2(src.getBusinessName2());
        dst.setVatNumber(src.getVatNumber());
        dst.setTaxCode(src.getTaxCode());
        dst.setVatCee(src.getVatCee());
        dst.setSearchKey(src.getSearchKey());
        dst.setZone(src.getZone());
        // tab-indir
        dst.setAddress(src.getAddress());
        dst.setCity(src.getCity());
        dst.setZipCode(src.getZipCode());
        dst.setProvince(src.getProvince());
        dst.setCountry(src.getCountry());
        dst.setPhone(src.getPhone());
        dst.setFax(src.getFax());
        dst.setTelex(src.getTelex());
        dst.setEmail(src.getEmail());
        // tab-iva
        dst.setCeeCustomer(src.getCeeCustomer());
        // tab-contab
        dst.setAccountPct1(src.getAccountPct1());
        dst.setAccountPct2(src.getAccountPct2());
        dst.setAccountPct3(src.getAccountPct3());
        dst.setAccountPct4(src.getAccountPct4());
        dst.setAccountPct5(src.getAccountPct5());
        // tab-banca
        dst.setBankAccountNo(src.getBankAccountNo());
        dst.setAbi(src.getAbi());
        dst.setCab(src.getCab());
        dst.setCin(src.getCin());
        dst.setIban(src.getIban());
        dst.setBank2AccountNo(src.getBank2AccountNo());
        dst.setBank2Abi(src.getBank2Abi());
        dst.setBank2Cab(src.getBank2Cab());
        dst.setBank2Iban(src.getBank2Iban());
        // tab-comm
        dst.setCurrency(src.getCurrency());
        dst.setSupplyDays(src.getSupplyDays());
        // tab-rifforn
        dst.setOurCustomerCode(src.getOurCustomerCode());
        // tab-contab
        dst.setAccount1(src.getAccount1());
        dst.setCostCenter1(src.getCostCenter1());
        dst.setAccount2(src.getAccount2());
        dst.setCostCenter2(src.getCostCenter2());
        dst.setAccount3(src.getAccount3());
        dst.setCostCenter3(src.getCostCenter3());
        dst.setAccount4(src.getAccount4());
        dst.setCostCenter4(src.getCostCenter4());
        dst.setAccount5(src.getAccount5());
        dst.setCostCenter5(src.getCostCenter5());
    }
}
