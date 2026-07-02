package com.aquarius.controller;

import com.aquarius.dto.FormTab;
import com.aquarius.entity.tenant.Customer;
import com.aquarius.repository.tenant.CustomerRepository;
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

@Controller
@RequestMapping("/clienti")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final BreadcrumbService breadcrumbService;

    private static final int PAGE_SIZE = 50;

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        Pageable pageable = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        Page<Customer> result = customerRepository.search(q, pageable);
        model.addAttribute("customers", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/clienti", principal.getUsername()));
        return "clienti/list";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        Customer c = customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente non trovato: " + id));
        model.addAttribute("customer", c);

        // Breadcrumb: parte dal menu, aggiunge il cliente come ultimo crumb non cliccabile
        List<Crumb> crumbs = new ArrayList<>(breadcrumbService.forUrl("/clienti", principal.getUsername()));
        crumbs.add(new Crumb(c.getCode(), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "clienti/detail";
    }

    /**
     * Mostra il form di modifica anagrafica — replica web di MENU_CLI000.scx.
     * Tab allineati al form VFP originale: 7 attivi + 12 placeholder.
     */
    @GetMapping("/{id}/edit")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String edit(@PathVariable String id, Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        Customer c = customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente non trovato: " + id));
        model.addAttribute("customer", c);

        // Breadcrumb del form di modifica
        List<Crumb> crumbs = new ArrayList<>(breadcrumbService.forUrl("/clienti", principal.getUsername()));
        crumbs.add(new Crumb(c.getCode(), "/clienti/" + id));
        crumbs.add(new Crumb("Modifica", null));
        model.addAttribute("breadcrumbs", crumbs);

        // Definizione dei tab del form — replica i 19 tab del VFP MENU_CLI000.
        // I 7 con dati editabili sono "attivi", gli altri 12 sono placeholder.
        model.addAttribute("formTabs", buildCustomerFormTabs());
        return "clienti/edit";
    }

    /** Definizione dei 19 tab del form anagrafica cliente, replica VFP. */
    private List<FormTab> buildCustomerFormTabs() {
        return List.of(
            // ─── attivi (modificabili) ──────────────────────────────────────
            FormTab.builder().id("tab-anag")  .label("Anagrafica")    .icon("bi-card-text") .active(true).build(),
            FormTab.builder().id("tab-iva")   .label("IVA")           .icon("bi-receipt").build(),
            FormTab.builder().id("tab-cont")  .label("Contatti")      .icon("bi-telephone").build(),
            FormTab.builder().id("tab-comm")  .label("Commercio")     .icon("bi-cart").build(),
            FormTab.builder().id("tab-conti") .label("Conti")         .icon("bi-bank").build(),
            FormTab.builder().id("tab-indir") .label("Indirizzo")     .icon("bi-geo-alt").build(),
            FormTab.builder().id("tab-note")  .label("Annotazioni")   .icon("bi-journal").build(),
            // ─── placeholder (slice future) ─────────────────────────────────
            FormTab.builder().id("tab-vett")  .label("Vettori")       .icon("bi-truck").placeholder(true).build(),
            FormTab.builder().id("tab-contab").label("Contabili")     .icon("bi-calculator").placeholder(true).build(),
            FormTab.builder().id("tab-postic").label("Posticipi riba").icon("bi-calendar-event").placeholder(true).build(),
            FormTab.builder().id("tab-fido")  .label("Fido")          .icon("bi-cash-stack").placeholder(true).build(),
            FormTab.builder().id("tab-rifcli").label("Rif. cliente")  .icon("bi-people").placeholder(true).build(),
            FormTab.builder().id("tab-web")   .label("Web")           .icon("bi-globe").placeholder(true).build(),
            FormTab.builder().id("tab-legali").label("Legali")        .icon("bi-shield-check").placeholder(true).build(),
            FormTab.builder().id("tab-prod")  .label("Produzione")    .icon("bi-gear").placeholder(true).build(),
            FormTab.builder().id("tab-emails").label("Gruppi E-mail") .icon("bi-envelope").placeholder(true).build(),
            FormTab.builder().id("tab-testi") .label("Testi")         .icon("bi-text-paragraph").placeholder(true).build(),
            FormTab.builder().id("tab-fattu") .label("Fatturare a")   .icon("bi-arrow-right-circle").placeholder(true).build(),
            FormTab.builder().id("tab-intra") .label("Intrastat")     .icon("bi-globe2").placeholder(true).build(),
            FormTab.builder().id("tab-rid")   .label("Distinte RID")  .icon("bi-credit-card").placeholder(true).build(),
            FormTab.builder().id("tab-fe")    .label("Fattura elettronica").icon("bi-envelope-paper").placeholder(true).build()
        );
    }

    /**
     * Salva le modifiche dell'anagrafica cliente.
     */
    @PostMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager")
    public String save(@PathVariable String id,
                       @ModelAttribute("customer") Customer formData,
                       RedirectAttributes ra) {
        Customer dbEntity = customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente non trovato: " + id));

        copyEditableFields(formData, dbEntity);

        try {
            customerRepository.save(dbEntity);
            log.info("Cliente {}/{} aggiornato da web (id={})",
                     dbEntity.getSocietyCode(), dbEntity.getCode(), id);
            ra.addFlashAttribute("flashSuccess",
                "Modifiche al cliente " + dbEntity.getCode() + " salvate.");
        } catch (Exception e) {
            log.error("Errore salvando cliente id={}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("flashError",
                "Errore durante il salvataggio: " + e.getMessage());
            return "redirect:/clienti/" + id + "/edit";
        }
        return "redirect:/clienti/" + id;
    }

    @GetMapping("/map")
    public String map() {
        return "clienti/map-coming-soon";
    }

    /**
     * Patch esplicito dei campi modificabili. Usare un metodo dedicato (e non
     * BeanUtils.copyProperties) per documentare quali campi sono "scrivibili
     * dal web" e per evitare bug se in futuro aggiungiamo campi sensibili.
     */
    private void copyEditableFields(Customer src, Customer dst) {
        // Anagrafica
        dst.setBusinessName(src.getBusinessName());
        dst.setBusinessName2(src.getBusinessName2());
        dst.setVatNumber(src.getVatNumber());
        dst.setTaxCode(src.getTaxCode());
        dst.setPersonType(src.getPersonType());
        dst.setVatCeeNumber(src.getVatCeeNumber());
        // Indirizzo
        dst.setAddress(src.getAddress());
        dst.setZipCode(src.getZipCode());
        dst.setCity(src.getCity());
        dst.setProvince(src.getProvince());
        dst.setCountry(src.getCountry());
        dst.setZone(src.getZone());
        // Contatti
        dst.setPhone(src.getPhone());
        dst.setFax(src.getFax());
        dst.setTelex(src.getTelex());
        dst.setEmail(src.getEmail());
        dst.setEmailAlt1(src.getEmailAlt1());
        dst.setEmailAlt2(src.getEmailAlt2());
        // Commerciali
        dst.setAgent(src.getAgent());
        dst.setPaymentTerms(src.getPaymentTerms());
        dst.setPriceList(src.getPriceList());
        dst.setCurrency(src.getCurrency());
        dst.setDiscountPercent(src.getDiscountPercent());
        dst.setCreditLimitEnabled(src.getCreditLimitEnabled());
        dst.setCreditLimit(src.getCreditLimit());
        // IVA
        dst.setVatRateCode(src.getVatRateCode());
        dst.setAlternateVatCode(src.getAlternateVatCode());
        dst.setTaxOffice(src.getTaxOffice());
        dst.setSuspendedTax(src.getSuspendedTax());
        dst.setExemptionCode(src.getExemptionCode());
        dst.setExemptionDate(src.getExemptionDate());
        // Banca
        dst.setBankCode(src.getBankCode());
        dst.setIban(src.getIban());
        dst.setAbi(src.getAbi());
        dst.setCab(src.getCab());
        // Note
        dst.setNotes(src.getNotes());
    }
}
