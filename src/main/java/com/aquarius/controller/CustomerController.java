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
     * Tab allineati al form VFP MENU_CLI000: 18 attivi + 1 placeholder (Distinte RID).
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
        // 18 tab con campi editabili; solo Distinte RID resta placeholder (colonne CLI_*RID assenti nello schema U_CLI_AN).
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
            FormTab.builder().id("tab-vett")  .label("Vettori")       .icon("bi-truck").build(),
            FormTab.builder().id("tab-contab").label("Contabili")     .icon("bi-calculator").build(),
            FormTab.builder().id("tab-postic").label("Posticipi riba").icon("bi-calendar-event").build(),
            FormTab.builder().id("tab-fido")  .label("Fido")          .icon("bi-cash-stack").build(),
            FormTab.builder().id("tab-rifcli").label("Rif. cliente")  .icon("bi-people").build(),
            FormTab.builder().id("tab-web")   .label("Web")           .icon("bi-globe").build(),
            FormTab.builder().id("tab-legali").label("Legali")        .icon("bi-shield-check").build(),
            FormTab.builder().id("tab-prod")  .label("Produzione")    .icon("bi-gear").build(),
            FormTab.builder().id("tab-emails").label("Gruppi E-mail") .icon("bi-envelope").build(),
            FormTab.builder().id("tab-testi") .label("Testi")         .icon("bi-text-paragraph").build(),
            FormTab.builder().id("tab-fattu") .label("Fatturare a")   .icon("bi-arrow-right-circle").build(),
            FormTab.builder().id("tab-intra") .label("Intrastat")     .icon("bi-globe2").build(),
            FormTab.builder().id("tab-rid")   .label("Distinte RID")  .icon("bi-credit-card").placeholder(true).build(),
            FormTab.builder().id("tab-fe")    .label("Fattura elettronica").icon("bi-envelope-paper").build()
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

        // ── Campi tab estesi ──
        // tab-vett
        dst.setShipMethod(src.getShipMethod());
        dst.setShipPort(src.getShipPort());
        dst.setCarrier1(src.getCarrier1());
        dst.setCarrier2(src.getCarrier2());
        dst.setCarrier3(src.getCarrier3());
        dst.setPackagingCode(src.getPackagingCode());
        dst.setPackagingCharge(src.getPackagingCharge());
        dst.setShipToSelf(src.getShipToSelf());
        dst.setTransportCharge(src.getTransportCharge());
        dst.setDeliveryNote(src.getDeliveryNote());
        dst.setOrderAsDdt(src.getOrderAsDdt());
        // tab-contab
        dst.setAccount1(src.getAccount1());
        dst.setAccount2(src.getAccount2());
        dst.setAccount3(src.getAccount3());
        dst.setAccount4(src.getAccount4());
        dst.setAccount5(src.getAccount5());
        dst.setCostCenter1(src.getCostCenter1());
        dst.setCostCenter2(src.getCostCenter2());
        dst.setCostCenter3(src.getCostCenter3());
        dst.setCostCenter4(src.getCostCenter4());
        dst.setCostCenter5(src.getCostCenter5());
        dst.setAccountPct1(src.getAccountPct1());
        dst.setAccountPct2(src.getAccountPct2());
        dst.setAccountPct3(src.getAccountPct3());
        dst.setAccountPct4(src.getAccountPct4());
        dst.setCollectionAccount(src.getCollectionAccount());
        // tab-postic
        dst.setPostpone1From(src.getPostpone1From());
        dst.setPostpone1To(src.getPostpone1To());
        dst.setPostpone1Days(src.getPostpone1Days());
        dst.setPostpone2From(src.getPostpone2From());
        dst.setPostpone2To(src.getPostpone2To());
        dst.setPostpone2Days(src.getPostpone2Days());
        dst.setPostponeM01From(src.getPostponeM01From());
        dst.setPostponeM01To(src.getPostponeM01To());
        dst.setPostponeM01Days(src.getPostponeM01Days());
        dst.setPostponeM02From(src.getPostponeM02From());
        dst.setPostponeM02To(src.getPostponeM02To());
        dst.setPostponeM02Days(src.getPostponeM02Days());
        dst.setPostponeM03From(src.getPostponeM03From());
        dst.setPostponeM03To(src.getPostponeM03To());
        dst.setPostponeM03Days(src.getPostponeM03Days());
        dst.setPostponeM04From(src.getPostponeM04From());
        dst.setPostponeM04To(src.getPostponeM04To());
        dst.setPostponeM04Days(src.getPostponeM04Days());
        dst.setPostponeM05From(src.getPostponeM05From());
        dst.setPostponeM05To(src.getPostponeM05To());
        dst.setPostponeM05Days(src.getPostponeM05Days());
        dst.setPostponeM06From(src.getPostponeM06From());
        dst.setPostponeM06To(src.getPostponeM06To());
        dst.setPostponeM06Days(src.getPostponeM06Days());
        dst.setPostponeM07From(src.getPostponeM07From());
        dst.setPostponeM07To(src.getPostponeM07To());
        dst.setPostponeM07Days(src.getPostponeM07Days());
        dst.setPostponeM09From(src.getPostponeM09From());
        dst.setPostponeM09To(src.getPostponeM09To());
        dst.setPostponeM09Days(src.getPostponeM09Days());
        dst.setPostponeM10From(src.getPostponeM10From());
        dst.setPostponeM10To(src.getPostponeM10To());
        dst.setPostponeM10Days(src.getPostponeM10Days());
        dst.setPostponeM11From(src.getPostponeM11From());
        dst.setPostponeM11To(src.getPostponeM11To());
        dst.setPostponeM11Days(src.getPostponeM11Days());
        // tab-fido
        dst.setCreditClass2(src.getCreditClass2());
        dst.setCreditAmount2(src.getCreditAmount2());
        dst.setRiskClass(src.getRiskClass());
        dst.setCreditCheckFlag(src.getCreditCheckFlag());
        // tab-rifcli
        dst.setRefCategory(src.getRefCategory());
        dst.setRefLastName(src.getRefLastName());
        dst.setRefFirstName(src.getRefFirstName());
        dst.setRefTaxCode(src.getRefTaxCode());
        dst.setRefType(src.getRefType());
        dst.setRefDescription(src.getRefDescription());
        // tab-web
        dst.setHomePage(src.getHomePage());
        dst.setPrivacyConsent(src.getPrivacyConsent());
        dst.setWebProfile(src.getWebProfile());
        // tab-legali
        dst.setLegalName(src.getLegalName());
        dst.setLegalName2(src.getLegalName2());
        dst.setLegalAddress(src.getLegalAddress());
        dst.setLegalCity(src.getLegalCity());
        dst.setLegalZip(src.getLegalZip());
        dst.setLegalProvince(src.getLegalProvince());
        dst.setLegalCountry(src.getLegalCountry());
        dst.setLegalVat(src.getLegalVat());
        dst.setLegalTaxCode(src.getLegalTaxCode());
        dst.setLegalVatCee(src.getLegalVatCee());
        dst.setHasLegalSeat(src.getHasLegalSeat());
        // tab-prod
        dst.setProdFlagUpd(src.getProdFlagUpd());
        dst.setProdFlagQc(src.getProdFlagQc());
        dst.setProdFlagAbp(src.getProdFlagAbp());
        dst.setProdRotation(src.getProdRotation());
        dst.setProdRotationDesc(src.getProdRotationDesc());
        dst.setProdFlagLabel(src.getProdFlagLabel());
        dst.setProdFlagWeight(src.getProdFlagWeight());
        dst.setProdLabelNote(src.getProdLabelNote());
        dst.setProdNoDisplay(src.getProdNoDisplay());
        dst.setProdNoFinished(src.getProdNoFinished());
        dst.setProdWeightOrder(src.getProdWeightOrder());
        dst.setProdWeightDdt(src.getProdWeightDdt());
        dst.setProdWeightInv(src.getProdWeightInv());
        dst.setProdFullDelivery(src.getProdFullDelivery());
        dst.setProdDeliveryDays(src.getProdDeliveryDays());
        dst.setProdNotifyFlag(src.getProdNotifyFlag());
        // tab-emails
        // tab-testi
        dst.setTextCode(src.getTextCode());
        dst.setTextOrder(src.getTextOrder());
        dst.setTextDdt(src.getTextDdt());
        dst.setTextInvoice(src.getTextInvoice());
        dst.setTextProforma(src.getTextProforma());
        dst.setTextTag(src.getTextTag());
        dst.setTextSpecial(src.getTextSpecial());
        // tab-fattu
        dst.setInvoiceToCode(src.getInvoiceToCode());
        dst.setInvoiceType(src.getInvoiceType());
        // tab-intra
        dst.setIntraNationality(src.getIntraNationality());
        dst.setIntraCountry(src.getIntraCountry());
        dst.setIntraBirthPlace(src.getIntraBirthPlace());
        dst.setIntraBirthCity(src.getIntraBirthCity());
        dst.setIntraDomicile(src.getIntraDomicile());
        dst.setIntraBirthProv(src.getIntraBirthProv());
        // tab-fe
        dst.setFeAttachment(src.getFeAttachment());
    }
}
