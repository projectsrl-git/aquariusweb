package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.entity.tenant.Agent;
import com.aquarius.entity.tenant.AreaManagerAgent;
import com.aquarius.entity.tenant.Bank;
import com.aquarius.repository.tenant.AgentRepository;
import com.aquarius.repository.tenant.AreaManagerAgentRepository;
import com.aquarius.repository.tenant.BankRepository;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Anagrafiche minori — consultazione read-only:
 * agenti (U_AGE_AN, MENU_AGE000), banche (U_BAN_AN, MENU_BAN000) e
 * associazione capo area ↔ agente (U_CAR_AN, MENU_CAR000 "Gestione agenti
 * per capo area" — la % provvigione e' calcolata sull'imponibile del
 * venduto, come dichiara la maschera legacy).
 * Condizioni di pagamento e vettori NON sono qui: sono categorie PARA
 * (CPA e VET) gia' consultabili in /parametri/CPA e /parametri/VET.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AnagraficheMinoriController {

    private static final Set<String> AGE_SORTABLE = Set.of(
        "code", "name", "city", "province", "commissionPct");
    private static final Set<String> BAN_SORTABLE = Set.of(
        "code", "name", "city", "abi", "cab");
    private static final Set<String> CAR_SORTABLE = Set.of(
        "areaManagerCode", "areaManagerName", "agentCode", "agentName", "commissionPct");

    private final AgentRepository agentRepository;
    private final BankRepository bankRepository;
    private final AreaManagerAgentRepository areaManagerRepository;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    // ─── Agenti ──────────────────────────────────────────────────────────────

    @GetMapping("/agenti")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String agenti(@RequestParam(value = "q", required = false) String q,
                         @RequestParam(value = "page", required = false) Integer page,
                         @RequestParam(value = "size", required = false) Integer size,
                         @RequestParam(value = "sort", required = false) String sort,
                         @RequestParam(value = "dir", required = false) String dir,
                         Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        ListParams lp = ListParams.of(page, size, sort, dir, AGE_SORTABLE, "code", "asc");
        Page<Agent> result = agentRepository.search(
            fiscalContext.getSocietyCode(), q, lp.toPageable());
        model.addAttribute("agenti", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/agenti", principal.getUsername()));
        return "agenti/list";
    }

    @GetMapping("/agenti/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String agenteDetail(@PathVariable String id, Model model,
                               @AuthenticationPrincipal AquariusPrincipal principal) {
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Agente non trovato: " + id));
        model.addAttribute("agente", agent);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/agenti", principal.getUsername()));
        crumbs.add(new Crumb(agent.getName() == null ? id : agent.getName().trim(), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "agenti/detail";
    }

    // ─── Banche ──────────────────────────────────────────────────────────────

    @GetMapping("/banche")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String banche(@RequestParam(value = "q", required = false) String q,
                         @RequestParam(value = "page", required = false) Integer page,
                         @RequestParam(value = "size", required = false) Integer size,
                         @RequestParam(value = "sort", required = false) String sort,
                         @RequestParam(value = "dir", required = false) String dir,
                         Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        ListParams lp = ListParams.of(page, size, sort, dir, BAN_SORTABLE, "code", "asc");
        Page<Bank> result = bankRepository.search(
            fiscalContext.getSocietyCode(), q, lp.toPageable());
        model.addAttribute("banche", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/banche", principal.getUsername()));
        return "banche/list";
    }

    @GetMapping("/banche/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String bancaDetail(@PathVariable String id, Model model,
                              @AuthenticationPrincipal AquariusPrincipal principal) {
        Bank bank = bankRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Banca non trovata: " + id));
        model.addAttribute("banca", bank);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/banche", principal.getUsername()));
        crumbs.add(new Crumb(bank.getName() == null ? id : bank.getName().trim(), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "banche/detail";
    }

    // ─── Capi area (provvigioni) ─────────────────────────────────────────────

    @GetMapping("/capi-area")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String capiArea(@RequestParam(value = "q", required = false) String q,
                           @RequestParam(value = "page", required = false) Integer page,
                           @RequestParam(value = "size", required = false) Integer size,
                           @RequestParam(value = "sort", required = false) String sort,
                           @RequestParam(value = "dir", required = false) String dir,
                           Model model,
                           @AuthenticationPrincipal AquariusPrincipal principal) {
        ListParams lp = ListParams.of(page, size, sort, dir, CAR_SORTABLE,
            "areaManagerCode", "asc");
        Page<AreaManagerAgent> result = areaManagerRepository.search(
            fiscalContext.getSocietyCode(), q, lp.toPageable());
        model.addAttribute("associazioni", result);
        model.addAttribute("pageObj", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/capi-area", principal.getUsername()));
        return "capi-area/list";
    }
}
