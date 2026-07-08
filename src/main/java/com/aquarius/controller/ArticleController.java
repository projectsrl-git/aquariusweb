package com.aquarius.controller;

import com.aquarius.entity.tenant.Article;
import com.aquarius.repository.tenant.ArticleRepository;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.service.BreadcrumbService.Crumb;
import com.aquarius.web.ListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Anagrafica articoli — read-only consultation of U_ART_PR.
 * Web counterpart of the VFP form MENU_ART000 (list + detail; no editing:
 * article maintenance stays on the VFP client for now).
 */
@Controller
@RequestMapping("/articoli")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {

    private static final Set<String> SORTABLE = Set.of(
        "code", "description", "unitOfMeasure", "salePrice1",
        "vatCode", "commodityCategory", "supplierCode");

    private final ArticleRepository articleRepository;
    private final BreadcrumbService breadcrumbService;

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "dir", required = false) String dir,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        ListParams lp = ListParams.of(page, size, sort, dir, SORTABLE, "code", "asc");
        Page<Article> result = articleRepository.search(q, lp.toPageable());
        model.addAttribute("articles", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/articoli", principal.getUsername()));
        return "articoli/list";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        Article a = articleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Articolo non trovato: " + id));
        model.addAttribute("article", a);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/articoli", principal.getUsername()));
        crumbs.add(new Crumb(a.getCode() == null ? id : a.getCode().trim(), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "articoli/detail";
    }
}
