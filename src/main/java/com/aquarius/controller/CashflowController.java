package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.repository.tenant.PartitaClienteRepository;
import com.aquarius.repository.tenant.PartitaFornitoreRepository;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
 * Situazione cash flow (read-only): liquidita' attesa dalle PARTITE
 * APERTE — il pendant "in avanti" dello scadenziario (stesse ampiezze
 * di fascia, direzione futura). Inflow = partite clienti aperte,
 * outflow = partite fornitori aperte (residuo = PAR_TOTIM − PAR_PAGATO,
 * scadenza = PAR_DTSCAD).
 *
 * Limite DICHIARATO rispetto al legacy (Situazione Cash Inflow/Outflow):
 * il legacy proietta anche ordini da evadere (ORD/ORF) e DDT non
 * fatturati (BOL/BFO) simulando le condizioni di pagamento; qui c'e'
 * la sola componente fatture/partite (FAT/FAF). Estensione tracciata
 * come NEEDS_DOMAIN (ecashlib.prg vuoto nel repo: regole di proiezione
 * non ricostruibili dai sorgenti).
 *
 * Scelte di onesta': residui NEGATIVI (note di accredito, anticipi)
 * inclusi cosi' come sono, come nel legacy; scadenze assenti o in
 * formato anomalo esposte in una fascia dedicata "senza data" (il form
 * legacy le RISCRIVE in tabella con DBO.RIBALTA2 — qui niente scrittura).
 */
@Controller
@RequestMapping("/contabilita/cashflow")
@RequiredArgsConstructor
public class CashflowController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final String[] FASCE = {
        "Scaduto/oggi", "1–30 gg", "31–60 gg", "61–90 gg", "Oltre 90 gg", "Senza data"};

    private final PartitaClienteRepository clientiRepo;
    private final PartitaFornitoreRepository fornitoriRepo;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    /** Riga per anagrafica con importi per fascia futura. */
    @Getter
    public static class PartyRow {
        private final String code;
        private final String name;
        private final BigDecimal[] fasce = new BigDecimal[6];
        public PartyRow(String code, String name) {
            this.code = code; this.name = name;
            for (int i = 0; i < 6; i++) fasce[i] = BigDecimal.ZERO;
        }
        void add(int idx, BigDecimal v) { fasce[idx] = fasce[idx].add(v); }
        public BigDecimal getTotale() {
            BigDecimal t = BigDecimal.ZERO;
            for (BigDecimal f : fasce) t = t.add(f);
            return t;
        }
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String view(Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        LocalDate oggi = LocalDate.now();

        Map<String, PartyRow> inRows = new LinkedHashMap<>();
        Map<String, PartyRow> outRows = new LinkedHashMap<>();
        BigDecimal[] inflow = zeros();
        BigDecimal[] outflow = zeros();

        Consumer<Object[]> accumIn = p -> accumulate(inRows, inflow, oggi, p);
        Consumer<Object[]> accumOut = p -> accumulate(outRows, outflow, oggi, p);

        clientiRepo.findAperte(soc, anno).forEach(p -> accumIn.accept(
            new Object[]{ p.getPartyCode(), p.getPartyName(), p.getResidual(), p.getDueDate() }));
        fornitoriRepo.findAperte(soc, anno).forEach(p -> accumOut.accept(
            new Object[]{ p.getPartyCode(), p.getPartyName(), p.getResidual(), p.getDueDate() }));

        // netto per fascia + cumulato progressivo (la "curva")
        BigDecimal[] netto = zeros();
        BigDecimal[] cumulato = zeros();
        BigDecimal running = BigDecimal.ZERO;
        for (int i = 0; i < 6; i++) {
            netto[i] = inflow[i].subtract(outflow[i]);
            if (i < 5) {           // la fascia "senza data" resta fuori dal cumulato
                running = running.add(netto[i]);
            }
            cumulato[i] = i < 5 ? running : null;
        }

        BigDecimal totIn = sum(inflow), totOut = sum(outflow);

        List<PartyRow> clienti = new ArrayList<>(inRows.values());
        clienti.removeIf(r -> r.getTotale().signum() == 0);
        clienti.sort(Comparator.comparing(PartyRow::getTotale).reversed());
        List<PartyRow> fornitori = new ArrayList<>(outRows.values());
        fornitori.removeIf(r -> r.getTotale().signum() == 0);
        fornitori.sort(Comparator.comparing(PartyRow::getTotale).reversed());

        long senzaData = 0;
        if (inflow[5].signum() != 0 || outflow[5].signum() != 0) senzaData = 1;

        model.addAttribute("fasce", FASCE);
        model.addAttribute("inflow", inflow);
        model.addAttribute("outflow", outflow);
        model.addAttribute("netto", netto);
        model.addAttribute("cumulato", cumulato);
        model.addAttribute("totIn", totIn);
        model.addAttribute("totOut", totOut);
        model.addAttribute("totNetto", totIn.subtract(totOut));
        model.addAttribute("clienti", clienti);
        model.addAttribute("fornitori", fornitori);
        model.addAttribute("hasSenzaData", senzaData > 0);
        model.addAttribute("anno", anno);
        model.addAttribute("oggi", oggi.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/cashflow", principal.getUsername()));
        return "contabilita/cashflow";
    }

    /**
     * Fascia futura: 0=scaduto/oggi (scadenza <= oggi), 1=1-30, 2=31-60,
     * 3=61-90, 4=oltre 90, 5=senza data/formato anomalo.
     */
    private static void accumulate(Map<String, PartyRow> rows, BigDecimal[] totals,
                                   LocalDate oggi, Object[] p) {
        BigDecimal residuo = (BigDecimal) p[2];
        if (residuo == null || residuo.signum() == 0) return;
        String dueStr = (String) p[3];
        int idx = 5;
        if (dueStr != null && dueStr.trim().length() == 10) {
            try {
                long giorni = ChronoUnit.DAYS.between(oggi, LocalDate.parse(dueStr.trim(), FMT));
                if (giorni <= 0) idx = 0;
                else if (giorni <= 30) idx = 1;
                else if (giorni <= 60) idx = 2;
                else if (giorni <= 90) idx = 3;
                else idx = 4;
            } catch (Exception ignore) {
                idx = 5;
            }
        }
        String code = p[0] == null ? "" : ((String) p[0]).trim();
        String name = p[1] == null ? "" : ((String) p[1]).trim();
        rows.computeIfAbsent(code, k -> new PartyRow(k, name)).add(idx, residuo);
        totals[idx] = totals[idx].add(residuo);
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] a = new BigDecimal[6];
        for (int i = 0; i < 6; i++) a[i] = BigDecimal.ZERO;
        return a;
    }

    private static BigDecimal sum(BigDecimal[] a) {
        BigDecimal t = BigDecimal.ZERO;
        for (BigDecimal v : a) t = t.add(v);
        return t;
    }
}
