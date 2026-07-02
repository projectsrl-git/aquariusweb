package com.aquarius.controller;

import com.aquarius.entity.tenant.ParameterItem;
import com.aquarius.repository.tenant.ParameterRepository;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.service.BreadcrumbService.Crumb;
import com.aquarius.service.ParameterCategoryCatalog;
import com.aquarius.service.ParameterCategoryCatalog.Category;
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
import java.util.Optional;

/**
 * Gestione dei parametri di sistema Aquarius. Replica web del form VFP
 * {@code PARAGEST}, che il VFP usa come "form generico" filtrato per
 * categoria (es. PARAGEST con WPARA="TOP" gestisce i tipi operazione,
 * con WPARA="IVA" gestisce i codici IVA, ecc.).
 *
 * <p>3 livelli di navigazione:</p>
 * <ul>
 *   <li>{@code /parametri} — overview di tutte le ~340 categorie (raggruppate per area)</li>
 *   <li>{@code /parametri/{prefix}} — lista paginata dei valori di una categoria</li>
 *   <li>{@code /parametri/{prefix}/{id}/edit} — form di modifica di un singolo valore</li>
 * </ul>
 */
@Controller
@RequestMapping("/parametri")
@RequiredArgsConstructor
@Slf4j
public class ParametriController {

    private final ParameterRepository repository;
    private final ParameterCategoryCatalog catalog;
    private final BreadcrumbService breadcrumbService;

    private static final int PAGE_SIZE = 50;

    /**
     * Overview: tutte le categorie raggruppate per area + searchbox.
     */
    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String overview(@RequestParam(value = "q", required = false) String q,
                           Model model,
                           @AuthenticationPrincipal AquariusPrincipal principal) {
        if (q != null && !q.isBlank()) {
            // Filtro semplice client-side via Java
            String needle = q.trim().toLowerCase();
            List<Category> filtered = new ArrayList<>();
            for (Category c : catalog.all()) {
                if (c.getPrefix().toLowerCase().contains(needle)
                    || c.getDescription().toLowerCase().contains(needle)) {
                    filtered.add(c);
                }
            }
            model.addAttribute("filteredCategories", filtered);
            model.addAttribute("filtered", true);
        } else {
            model.addAttribute("byArea", catalog.byArea());
            model.addAttribute("filtered", false);
        }
        model.addAttribute("totalCount", catalog.all().size());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/parametri", principal.getUsername()));
        return "parametri/overview";
    }

    /**
     * Lista dei valori di una specifica categoria. Paginata + filtrabile.
     */
    @GetMapping("/{prefix}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String category(@PathVariable String prefix,
                           @RequestParam(value = "q", required = false) String q,
                           @RequestParam(value = "page", defaultValue = "0") int page,
                           Model model,
                           @AuthenticationPrincipal AquariusPrincipal principal) {
        Optional<Category> catOpt = catalog.byPrefix(prefix);
        if (catOpt.isEmpty()) {
            throw new IllegalArgumentException("Categoria parametri sconosciuta: " + prefix);
        }
        Category cat = catOpt.get();

        Pageable pg = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        Page<ParameterItem> items = repository.searchByPrefix(cat.getPrefix(), q, pg);

        model.addAttribute("category", cat);
        model.addAttribute("items", items);
        model.addAttribute("catalog", catalog);
        model.addAttribute("q", q == null ? "" : q);

        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/parametri", principal.getUsername()));
        crumbs.add(new Crumb(cat.getDescription(), null));
        model.addAttribute("breadcrumbs", crumbs);

        return "parametri/category";
    }

    /**
     * Form di modifica di un singolo parametro.
     */
    @GetMapping("/{prefix}/{id}/edit")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String edit(@PathVariable String prefix,
                       @PathVariable String id,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        Category cat = catalog.byPrefix(prefix).orElseThrow(
            () -> new IllegalArgumentException("Categoria sconosciuta: " + prefix));
        ParameterItem item = repository.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Parametro non trovato: " + id));

        model.addAttribute("category", cat);
        model.addAttribute("item", item);
        model.addAttribute("value", catalog.extractValue(item.getCodice(), cat));

        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/parametri", principal.getUsername()));
        crumbs.add(new Crumb(cat.getDescription(), "/parametri/" + cat.getPrefix()));
        crumbs.add(new Crumb(item.getCodice(), null));
        model.addAttribute("breadcrumbs", crumbs);

        return "parametri/edit";
    }

    /**
     * Salvataggio modifiche di un parametro. Solo descri, libera, disattivo.
     * Il codice è chiave business immutabile dal web.
     */
    @PostMapping("/{prefix}/{id}")
    @Transactional(transactionManager = "tenantTransactionManager")
    public String save(@PathVariable String prefix,
                       @PathVariable String id,
                       @ModelAttribute("item") ParameterItem formData,
                       RedirectAttributes ra) {
        ParameterItem dbEntity = repository.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Parametro non trovato: " + id));

        dbEntity.setDescri(formData.getDescri());
        dbEntity.setLibera(formData.getLibera());
        dbEntity.setDisattivo(formData.getDisattivo());

        try {
            repository.save(dbEntity);
            log.info("Parametro {} aggiornato da web (id={})", dbEntity.getCodice(), id);
            ra.addFlashAttribute("flashSuccess",
                "Parametro '" + dbEntity.getCodice() + "' aggiornato.");
        } catch (Exception e) {
            log.error("Errore salvando parametro id={}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("flashError",
                "Errore durante il salvataggio: " + e.getMessage());
            return "redirect:/parametri/" + prefix + "/" + id + "/edit";
        }
        return "redirect:/parametri/" + prefix;
    }
}
