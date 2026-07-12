package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.dto.ScadenzaRiga;
import com.aquarius.repository.tenant.PartitaClienteRepository;
import com.aquarius.repository.tenant.PartitaFornitoreRepository;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Scadenziario clienti/fornitori (read-only): partite aperte con aging degli
 * importi (a scadere + fasce di scaduto). Fonte: PART_CLI / PART_FOR (residuo =
 * totale − pagato, scadenza PAR_DTSCAD).
 */
@Controller
@RequestMapping("/contabilita/scadenziario")
@RequiredArgsConstructor
public class ScadenziarioController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final PartitaClienteRepository clientiRepo;
    private final PartitaFornitoreRepository fornitoriRepo;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String view(@RequestParam(value = "tipo", defaultValue = "clienti") String tipo,
                       Model model, @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        boolean fornitori = "fornitori".equals(tipo);
        LocalDate oggi = LocalDate.now();

        // Aggregazione per anagrafica (ordine di inserimento = per ragione sociale)
        Map<String, ScadenzaRiga> byParty = new LinkedHashMap<>();

        // Consumer che accumula una partita (codice, nome, residuo, scadenza) nell'aging
        Consumer<Object[]> accum = p -> {
            String code = (String) p[0];
            String name = (String) p[1];
            BigDecimal residuo = (BigDecimal) p[2];
            String dueStr = (String) p[3];
            if (residuo == null || residuo.signum() == 0) return;
            long giorni = 0;
            if (dueStr != null && dueStr.trim().length() == 10) {
                try { giorni = ChronoUnit.DAYS.between(LocalDate.parse(dueStr.trim(), FMT), oggi); }
                catch (Exception ignore) { giorni = 0; }
            }
            ScadenzaRiga r = byParty.computeIfAbsent(code == null ? "" : code.trim(),
                k -> new ScadenzaRiga(k, name != null ? name.trim() : ""));
            r.add(giorni, residuo);
        };

        if (fornitori) {
            fornitoriRepo.findAperte(soc, anno).forEach(p ->
                accum.accept(new Object[]{ p.getPartyCode(), p.getPartyName(), p.getResidual(), p.getDueDate() }));
        } else {
            clientiRepo.findAperte(soc, anno).forEach(p ->
                accum.accept(new Object[]{ p.getPartyCode(), p.getPartyName(), p.getResidual(), p.getDueDate() }));
        }

        List<ScadenzaRiga> righe = new ArrayList<>(byParty.values());
        // via le anagrafiche che a saldo non hanno aperto
        righe.removeIf(r -> r.getTotale().signum() == 0);
        righe.sort(Comparator.comparing(ScadenzaRiga::getTotale).reversed());

        // Totali generali
        BigDecimal tAScadere = BigDecimal.ZERO, t0 = BigDecimal.ZERO, t1 = BigDecimal.ZERO,
                   t2 = BigDecimal.ZERO, t3 = BigDecimal.ZERO;
        for (ScadenzaRiga r : righe) {
            tAScadere = tAScadere.add(r.getAScadere());
            t0 = t0.add(r.getSc0_30());
            t1 = t1.add(r.getSc31_60());
            t2 = t2.add(r.getSc61_90());
            t3 = t3.add(r.getScOltre90());
        }

        model.addAttribute("tipo", fornitori ? "fornitori" : "clienti");
        model.addAttribute("righe", righe);
        model.addAttribute("totAScadere", tAScadere);
        model.addAttribute("tot0_30", t0);
        model.addAttribute("tot31_60", t1);
        model.addAttribute("tot61_90", t2);
        model.addAttribute("totOltre90", t3);
        model.addAttribute("totScaduto", t0.add(t1).add(t2).add(t3));
        model.addAttribute("totGenerale", tAScadere.add(t0).add(t1).add(t2).add(t3));
        model.addAttribute("anno", anno);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/scadenziario", principal.getUsername()));
        return "contabilita/scadenziario";
    }
}
