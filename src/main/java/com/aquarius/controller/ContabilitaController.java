package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.dto.BilancioLine;
import com.aquarius.dto.LedgerRow;
import com.aquarius.entity.tenant.Account;
import com.aquarius.entity.tenant.MovContabile;
import com.aquarius.entity.tenant.PartitaCliente;
import com.aquarius.entity.tenant.PartitaFornitore;
import com.aquarius.repository.tenant.AccountRepository;
import com.aquarius.repository.tenant.MovContabileRepository;
import com.aquarius.repository.tenant.PartitaClienteRepository;
import com.aquarius.repository.tenant.PartitaFornitoreRepository;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Contabilità (consultazione, READ-ONLY) — replica web delle interrogazioni
 * contabili di Aquarius su MOV_CONT / PART_CLI / PART_FOR.
 *
 * <p>Quattro aree:
 * <ul>
 *   <li><b>Primanota</b> — elenco registrazioni + dettaglio righe Dare/Avere.</li>
 *   <li><b>Storico contabile</b> — mastrino di un conto con saldo scalare.</li>
 *   <li><b>Bilancio</b> — totali Dare/Avere e saldo per conto.</li>
 *   <li><b>Partitari</b> — partite aperte clienti e fornitori.</li>
 * </ul>
 * Il data-entry della primanota (scrittura su MOV_CONT) sarà una slice a parte.</p>
 */
@Controller
@RequestMapping("/contabilita")
@RequiredArgsConstructor
@Slf4j
public class ContabilitaController {

    private final MovContabileRepository movRepository;
    private final PartitaClienteRepository partitaClienteRepository;
    private final PartitaFornitoreRepository partitaFornitoreRepository;
    private final AccountRepository accountRepository;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    private static final int PAGE_SIZE = 50;

    // ─────────────────────────────────────────── PRIMANOTA ──────────────
    @GetMapping("/primanota")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String primanota(@RequestParam(value = "q", required = false) String q,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            Model model,
                            @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        Pageable pageable = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        Page<MovContabile> regs = movRepository.searchRegistrations(soc, anno, q, pageable);
        model.addAttribute("registrations", regs);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("anno", anno);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/primanota", principal.getUsername()));
        return "contabilita/primanota";
    }

    @GetMapping("/primanota/{nreg}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String registrationDetail(@PathVariable String nreg, Model model,
                                     @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        List<MovContabile> rows = movRepository.findRegistrationRows(soc, anno, nreg);

        BigDecimal totDare = rows.stream()
            .filter(m -> "D".equalsIgnoreCase(m.getMovementType()))
            .map(m -> m.getAmount() != null ? m.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totAvere = rows.stream()
            .filter(m -> "A".equalsIgnoreCase(m.getMovementType()))
            .map(m -> m.getAmount() != null ? m.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("rows", rows);
        model.addAttribute("nreg", nreg);
        model.addAttribute("header", rows.isEmpty() ? null : rows.get(0));
        model.addAttribute("totDare", totDare);
        model.addAttribute("totAvere", totAvere);
        model.addAttribute("accountNames", accountNameMap());

        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/contabilita/primanota", principal.getUsername()));
        crumbs.add(new Crumb("Registrazione " + nreg, null));
        model.addAttribute("breadcrumbs", crumbs);
        return "contabilita/registrazione";
    }

    // ─────────────────────────────────── STORICO CONTABILE (mastrino) ───
    @GetMapping("/storico")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String storico(@RequestParam(value = "conto", required = false) String conto,
                          Model model,
                          @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        model.addAttribute("conto", conto == null ? "" : conto);
        model.addAttribute("anno", anno);

        if (conto != null && !conto.isBlank()) {
            List<MovContabile> movs = movRepository.findLedger(soc, anno, conto.trim());
            List<LedgerRow> ledger = new ArrayList<>();
            BigDecimal balance = BigDecimal.ZERO;
            for (MovContabile m : movs) {
                BigDecimal imp = m.getAmount() != null ? m.getAmount() : BigDecimal.ZERO;
                balance = "D".equalsIgnoreCase(m.getMovementType())
                    ? balance.add(imp) : balance.subtract(imp);
                ledger.add(new LedgerRow(m, balance));
            }
            model.addAttribute("ledger", ledger);
            model.addAttribute("finalBalance", balance);
            model.addAttribute("accountName", accountNameMap().get(conto.trim()));
        }

        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/storico", principal.getUsername()));
        return "contabilita/storico";
    }

    // ─────────────────────────────────────────────── BILANCIO ──────────
    @GetMapping("/bilancio")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String bilancio(Model model,
                           @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        Map<String, String> names = accountNameMap();

        List<BilancioLine> lines = movRepository.findBilancio(soc, anno).stream()
            .map(r -> new BilancioLine(r.getAccount(), names.get(r.getAccount()),
                                       r.getTotDare(), r.getTotAvere()))
            .collect(Collectors.toList());

        BigDecimal totDare = lines.stream().map(BilancioLine::getTotDare)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totAvere = lines.stream().map(BilancioLine::getTotAvere)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("lines", lines);
        model.addAttribute("totDare", totDare);
        model.addAttribute("totAvere", totAvere);
        model.addAttribute("sbilancio", totDare.subtract(totAvere));
        model.addAttribute("anno", anno);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/bilancio", principal.getUsername()));
        return "contabilita/bilancio";
    }

    // ─────────────────────────────────────────────── PARTITARI ─────────
    @GetMapping("/partitari/clienti")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String partitariClienti(@RequestParam(value = "q", required = false) String q,
                                   @RequestParam(value = "page", defaultValue = "0") int page,
                                   Model model,
                                   @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        Pageable pageable = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        Page<PartitaCliente> partite = partitaClienteRepository.search(soc, anno, q, pageable);
        model.addAttribute("partite", partite);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("anno", anno);
        model.addAttribute("tipo", "clienti");
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/partitari/clienti", principal.getUsername()));
        return "contabilita/partitari";
    }

    @GetMapping("/partitari/fornitori")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String partitariFornitori(@RequestParam(value = "q", required = false) String q,
                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                     Model model,
                                     @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        Pageable pageable = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        Page<PartitaFornitore> partite = partitaFornitoreRepository.search(soc, anno, q, pageable);
        model.addAttribute("partite", partite);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("anno", anno);
        model.addAttribute("tipo", "fornitori");
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/partitari/fornitori", principal.getUsername()));
        return "contabilita/partitari";
    }

    // ─────────────────────────────────────────────── helper ────────────
    /** Mappa codice conto → descrizione per l'anno/società correnti. */
    private Map<String, String> accountNameMap() {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        return accountRepository.findAllByYearAndSociety(anno, soc).stream()
            .filter(a -> a.getCode() != null)
            .collect(Collectors.toMap(
                a -> a.getCode().trim(),
                a -> a.getDescription() != null ? a.getDescription().trim() : "",
                (a, b) -> a));
    }
}
