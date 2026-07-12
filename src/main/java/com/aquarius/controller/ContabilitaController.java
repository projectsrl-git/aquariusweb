package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.dto.BilancioLine;
import com.aquarius.dto.LedgerRow;
import com.aquarius.dto.RegistrazioneRow;
import com.aquarius.entity.tenant.Account;
import com.aquarius.entity.tenant.MovContabile;
import com.aquarius.entity.tenant.PartitaCliente;
import com.aquarius.entity.tenant.PartitaFornitore;
import com.aquarius.repository.tenant.AccountRepository;
import com.aquarius.repository.tenant.MovContabileRepository;
import com.aquarius.repository.tenant.PartitaClienteRepository;
import com.aquarius.repository.tenant.PartitaFornitoreRepository;
import com.aquarius.repository.tenant.ParameterRepository;
import com.aquarius.web.ListParams;
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
    private final ParameterRepository parameterRepository;
    private final AccountRepository accountRepository;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;
    private final com.aquarius.service.BilancioExcelExporter bilancioExcelExporter;

    // ─────────────────────────────────────────── PRIMANOTA ──────────────
    @GetMapping("/primanota")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String primanota(@RequestParam(value = "q", required = false) String q,
                            @RequestParam(value = "page", required = false) Integer page,
                            @RequestParam(value = "size", required = false) Integer size,
                            @RequestParam(value = "sort", required = false) String sort,
                            @RequestParam(value = "dir", required = false) String dir,
                            Model model,
                            @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();

        // Ordinamento: la lista è raggruppata per registrazione; i campi
        // ordinabili sono quelli della testata aggregata.
        // NB: l'ordinamento è sulle property della testata; l'importo (totDare)
        // è un alias di aggregato e NON è ordinabile a DB in modo affidabile
        // con il Sort del Pageable su JPQL/Hibernate 5 → escluso dalla whitelist.
        ListParams lp = ListParams.of(page, size, sort, dir,
            java.util.Set.of("registrationNo", "registrationDate", "documentNo"),
            "registrationDate", "desc");

        // 1) testate paginate (una riga per registrazione).
        // L'ordinamento è gestito nella query via orderCol/asc (compatibile GROUP BY).
        int orderCol = switch (lp.getSort()) {
            case "registrationNo" -> 2;
            case "documentNo" -> 3;
            default -> 1; // registrationDate
        };
        Page<MovContabileRepository.RegHead> heads =
            movRepository.searchRegistrationHeads(soc, anno, q, orderCol, lp.isAsc(),
                                                  lp.toPageableNoSort());

        // 2) righe delle registrazioni della pagina corrente → RegistrazioneRow
        List<String> nregs = heads.getContent().stream()
            .map(MovContabileRepository.RegHead::getRegistrationNo)
            .collect(Collectors.toList());
        Map<String, String> accountNames = accountNameMap();
        List<RegistrazioneRow> rows = nregs.isEmpty()
            ? List.of()
            : RegistrazioneRow.fromMovements(
                  movRepository.findRowsForRegistrations(soc, anno, nregs), accountNames);

        // 3) risolvi descrizione tipo operazione
        Map<String, String> topNames = operationTypeNameMap();
        for (RegistrazioneRow r : rows) {
            if (r.getOperationType() != null) {
                r.setOperationTypeDesc(operationTypeDesc(topNames, r.getOperationType()));
            }
        }
        // Riordina rows secondo l'ordine delle heads (il group-by potrebbe non preservarlo)
        Map<String, RegistrazioneRow> byNo = rows.stream()
            .collect(Collectors.toMap(RegistrazioneRow::getRegistrationNo, x -> x, (a, b) -> a));
        List<RegistrazioneRow> ordered = nregs.stream()
            .map(byNo::get).filter(java.util.Objects::nonNull).collect(Collectors.toList());

        model.addAttribute("rows", ordered);
        model.addAttribute("pageObj", heads);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("anno", anno);

        // Metriche cliccabili in testa
        addPrimanotaMetrics(model, soc, anno, topNames, accountNames);

        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/primanota", principal.getUsername()));
        return "contabilita/primanota";
    }

    /** Costruisce le metriche di raggruppamento cliccabili per la primanota. */
    private void addPrimanotaMetrics(Model model, String soc, String anno,
                                     Map<String, String> topNames,
                                     Map<String, String> accountNames) {
        Pageable top5 = PageRequest.of(0, 5);
        // Per periodo (mese) — importo già formattato in italiano [label, importo]
        var periods = movRepository.amountByPeriod(soc, anno).stream()
            .map(m -> new String[]{ m.getLabel(), formatIt(m.getTotal()) })
            .collect(Collectors.toList());
        model.addAttribute("metricPeriods", periods);
        // TOP 5 conti clienti/fornitori (proxy anagrafiche) [codice, descrizione, importo]
        var topCust = movRepository.topCustomerAccounts(soc, anno, top5).stream()
            .map(m -> new String[]{
                m.getLabel(),
                accountNames.getOrDefault(m.getLabel() != null ? m.getLabel().trim() : "", ""),
                formatIt(m.getTotal()) })
            .collect(Collectors.toList());
        model.addAttribute("metricTopAccounts", topCust);
        // TOP 5 tipi operazione [codice, descrizione, importo]
        var topOps = movRepository.topOperationTypes(soc, anno, top5).stream()
            .map(m -> new String[]{
                m.getLabel(),
                m.getLabel() != null ? topNames.getOrDefault("TOP" + m.getLabel().trim(), "") : "",
                formatIt(m.getTotal()) })
            .collect(Collectors.toList());
        model.addAttribute("metricTopOps", topOps);
    }

    /** Formatta un importo in stile italiano (1.234,56); null → "0,00". */
    private String formatIt(java.math.BigDecimal v) {
        java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols(java.util.Locale.ITALY);
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00", sym);
        return df.format(v != null ? v : java.math.BigDecimal.ZERO);
    }

    /**
     * Mappa descrizione tipo operazione. Nel legacy la descrizione NON sta in
     * TAB_TOPCONT ma nella tabella PARA: CODICE = 'TOP' + ALLTRIM(MOV_TOP),
     * descrizione = PARA.DESCRI (vedi contabilelib: join mov_cont/para_top).
     * La mappa è chiavata sul CODICE PARA completo (es. "TOP01"); il lookup
     * antepone quindi il prefisso "TOP" al codice MOV_TOP.
     */
    private Map<String, String> operationTypeNameMap() {
        return parameterRepository.findByPrefix("TOP").stream()
            .filter(p -> p.getCodice() != null)
            .collect(Collectors.toMap(
                p -> p.getCodice().trim(),
                p -> p.getDescri() != null ? p.getDescri().trim() : "",
                (a, b) -> a));
    }

    /** Descrizione tipo operazione per un codice MOV_TOP (aggiunge prefisso TOP). */
    private String operationTypeDesc(Map<String, String> topNames, String code) {
        if (code == null || code.trim().isEmpty()) return null;
        return topNames.get("TOP" + code.trim());
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
    public String bilancio(@RequestParam(value = "vista", defaultValue = "mastri") String vista,
                           @RequestParam(value = "elabora", defaultValue = "false") boolean elabora,
                           // R1: coppia C/F come scelta singola — normale | solo | nodettaglio
                           @RequestParam(value = "cfMode", defaultValue = "normale") String cfMode,
                           @RequestParam(value = "contiOrdine", defaultValue = "true") boolean contiOrdine,
                           @RequestParam(value = "nonZero", defaultValue = "true") boolean nonZero,
                           @RequestParam(value = "soloSottogruppi", defaultValue = "false") boolean soloSottogruppi,
                           Model model,
                           @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();

        // Normalizzazione opzioni + coerenza (R1/R2)
        String vistaN = "contrapposte".equals(vista) ? "contrapposte" : "mastri";
        if (!"solo".equals(cfMode) && !"nodettaglio".equals(cfMode)) cfMode = "normale";
        // R1: showCF (mostra le foglie C/F) è falso solo con "nodettaglio".
        boolean showCF = !"nodettaglio".equals(cfMode);

        model.addAttribute("vista", vistaN);
        model.addAttribute("elabora", elabora);
        model.addAttribute("cfMode", cfMode);
        model.addAttribute("contiOrdine", contiOrdine);
        model.addAttribute("nonZero", nonZero);
        model.addAttribute("soloSottogruppi", soloSottogruppi);
        model.addAttribute("anno", anno);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/bilancio", principal.getUsername()));

        // Passo 1: senza "Elabora" mostra solo il form opzioni (come lo "Stampa" legacy).
        if (!elabora) {
            return "contabilita/bilancio";
        }

        // Anagrafica conti: codice → Account (per sezione bilancio + tipo conto)
        Map<String, Account> accByCode = accountByCode();

        // Righe di bilancio (saldo per conto dai movimenti), arricchite con
        // sezione (CON_POSBIL: P/E) e tipo conto (CON_TIPOCO: C/F).
        List<BilancioLine> all = movRepository.findBilancio(soc, anno).stream()
            .map(r -> {
                String code = r.getAccount() != null ? r.getAccount().trim() : "";
                Account a = accByCode.get(code);
                String desc = a != null ? a.getDescription() : null;
                String section = a != null ? trimUpper(a.getBalancePosition()) : null;
                String tipo = a != null ? trimUpper(a.getAccountType()) : null;
                return new BilancioLine(code, desc, r.getTotDare(), r.getTotAvere(), section, tipo);
            })
            .collect(Collectors.toList());

        // Opzioni di visualizzazione (bilancio di verifica):
        // - "Non stampa i conti con saldo a zero"
        if (nonZero) {
            all = all.stream().filter(l -> l.getSaldo().signum() != 0).collect(Collectors.toList());
        }
        // - "Stampa i conti d'ordine": se disattivo, escludi il mastro 04 (conti d'ordine)
        if (!contiOrdine) {
            all = all.stream()
                .filter(l -> l.getAccount() == null || !l.getAccount().startsWith("04"))
                .collect(Collectors.toList());
        }
        // - "Stampa solo i clienti/fornitori" (R1): tiene solo i conti C/F
        if ("solo".equals(cfMode)) {
            all = all.stream().filter(BilancioLine::isCustomerOrSupplier).collect(Collectors.toList());
        }

        // NB: NON filtro qui i C/F. I totali di sezione e la quadratura devono
        // includere sempre clienti/fornitori; il toggle showCF agisce solo sulla
        // VISUALIZZAZIONE delle foglie (i loro importi restano nei progressivi di
        // gruppo/mastro → la quadratura non dipende dal toggle). Risolve 1.2.

        // Bucket a sezioni contrapposte
        List<BilancioLine> attivo = new ArrayList<>();
        List<BilancioLine> passivo = new ArrayList<>();
        List<BilancioLine> costi = new ArrayList<>();
        List<BilancioLine> ricavi = new ArrayList<>();
        List<BilancioLine> nonClassificati = new ArrayList<>();
        for (BilancioLine l : all) {
            if (l.isPatrimoniale()) {
                (l.isDareSide() ? attivo : passivo).add(l);
            } else if (l.isEconomico()) {
                (l.isDareSide() ? costi : ricavi).add(l);
            } else {
                nonClassificati.add(l);   // niente CON_POSBIL: da segnalare, non nascondere
            }
        }
        attivo.sort(java.util.Comparator.comparing(BilancioLine::getAccount));
        passivo.sort(java.util.Comparator.comparing(BilancioLine::getAccount));
        costi.sort(java.util.Comparator.comparing(BilancioLine::getAccount));
        ricavi.sort(java.util.Comparator.comparing(BilancioLine::getAccount));
        nonClassificati.sort(java.util.Comparator.comparing(BilancioLine::getAccount));

        BigDecimal totAttivo  = sumDisplay(attivo);
        BigDecimal totPassivo = sumDisplay(passivo);
        BigDecimal totCosti   = sumDisplay(costi);
        BigDecimal totRicavi  = sumDisplay(ricavi);
        BigDecimal risultato  = totRicavi.subtract(totCosti);   // >0 utile, <0 perdita
        // Quadratura di bilancio: (Attività − Passività) deve = risultato d'esercizio.
        BigDecimal quadraturaSP = totAttivo.subtract(totPassivo);
        BigDecimal sbilancio    = quadraturaSP.subtract(risultato);
        boolean quadraturaOk    = sbilancio.abs().compareTo(new BigDecimal("0.01")) <= 0;

        model.addAttribute("attivo", attivo);
        model.addAttribute("passivo", passivo);
        model.addAttribute("costi", costi);
        model.addAttribute("ricavi", ricavi);
        // Alberi mastro→gruppo→sottoconto (Bilancio di verifica), per sezione
        model.addAttribute("attivoTree",  buildGroups(attivo,  accByCode, showCF));
        model.addAttribute("passivoTree", buildGroups(passivo, accByCode, showCF));
        model.addAttribute("costiTree",   buildGroups(costi,   accByCode, showCF));
        model.addAttribute("ricaviTree",  buildGroups(ricavi,  accByCode, showCF));
        model.addAttribute("nonClassificati", nonClassificati);
        model.addAttribute("totAttivo", totAttivo);
        model.addAttribute("totPassivo", totPassivo);
        model.addAttribute("totCosti", totCosti);
        model.addAttribute("totRicavi", totRicavi);
        model.addAttribute("risultato", risultato.abs());
        model.addAttribute("risultatoUtile", risultato.signum() >= 0);
        // Quadratura patrimoniale: Attivo vs Passivo + risultato d'esercizio
        model.addAttribute("totPassivoConRisultato",
            risultato.signum() >= 0 ? totPassivo.add(risultato) : totPassivo);
        model.addAttribute("showCF", showCF);
        model.addAttribute("quadraturaSP", quadraturaSP.abs());
        model.addAttribute("sbilancio", sbilancio.abs());
        model.addAttribute("quadraturaOk", quadraturaOk);
        return "contabilita/bilancio";
    }

    /** Estrazione Excel del bilancio (foglio Dettaglio + Sintesi). */
    @GetMapping("/bilancio/export")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public org.springframework.http.ResponseEntity<byte[]> bilancioExport() throws java.io.IOException {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        com.aquarius.service.BilancioExcelExporter.Data d = buildBilancioExportData(soc, anno);
        byte[] bytes = bilancioExcelExporter.export(d);
        String filename = "Bilancio_" + anno + ".xlsx";
        return org.springframework.http.ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + "\"")
            .contentType(org.springframework.http.MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    /** Ricalcola i dati di bilancio per l'export (stessa classificazione della vista). */
    private com.aquarius.service.BilancioExcelExporter.Data buildBilancioExportData(String soc, String anno) {
        Map<String, Account> accByCode = accountByCode();
        List<BilancioLine> all = movRepository.findBilancio(soc, anno).stream()
            .map(r -> {
                String code = r.getAccount() != null ? r.getAccount().trim() : "";
                Account a = accByCode.get(code);
                return new BilancioLine(code, a != null ? a.getDescription() : null,
                    r.getTotDare(), r.getTotAvere(),
                    a != null ? trimUpper(a.getBalancePosition()) : null,
                    a != null ? trimUpper(a.getAccountType()) : null);
            })
            .filter(l -> l.getSaldo().signum() != 0)
            .collect(Collectors.toList());
        List<BilancioLine> attivo = new ArrayList<>(), passivo = new ArrayList<>(),
            costi = new ArrayList<>(), ricavi = new ArrayList<>(), nonCl = new ArrayList<>();
        for (BilancioLine l : all) {
            if (l.isPatrimoniale()) (l.isDareSide() ? attivo : passivo).add(l);
            else if (l.isEconomico()) (l.isDareSide() ? costi : ricavi).add(l);
            else nonCl.add(l);
        }
        java.util.Comparator<BilancioLine> byCode = java.util.Comparator.comparing(BilancioLine::getAccount);
        attivo.sort(byCode); passivo.sort(byCode); costi.sort(byCode); ricavi.sort(byCode); nonCl.sort(byCode);

        com.aquarius.service.BilancioExcelExporter.Data d = new com.aquarius.service.BilancioExcelExporter.Data();
        d.anno = anno;
        d.attivo = attivo; d.passivo = passivo; d.costi = costi; d.ricavi = ricavi; d.nonClassificati = nonCl;
        d.totAttivo = sumDisplay(attivo); d.totPassivo = sumDisplay(passivo);
        d.totCosti = sumDisplay(costi); d.totRicavi = sumDisplay(ricavi);
        BigDecimal risultato = d.totRicavi.subtract(d.totCosti);
        d.risultato = risultato.abs();
        d.risultatoUtile = risultato.signum() >= 0;
        d.quadraturaSP = d.totAttivo.subtract(d.totPassivo).abs();
        BigDecimal sbil = d.totAttivo.subtract(d.totPassivo).subtract(risultato);
        d.sbilancio = sbil.abs();
        d.quadraturaOk = sbil.abs().compareTo(new BigDecimal("0.01")) <= 0;
        return d;
    }

    /** Somma degli importi di prospetto (saldo assoluto) di un bucket. */
    private BigDecimal sumDisplay(List<BilancioLine> lines) {
        return lines.stream().map(BilancioLine::getDisplayAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Costruisce l'albero mastro (2 char) → gruppo (4 char) → sottoconti a partire
     * dalle righe (già ordinate per codice) di una sezione. I progressivi
     * Dare/Avere sono aggregati da TUTTE le righe; le foglie C/F sono nascoste dal
     * display se {@code showCF} è false, ma i loro importi restano nei totali.
     */
    private List<com.aquarius.dto.BilancioGroup> buildGroups(
            List<BilancioLine> lines, Map<String, Account> accByCode, boolean showCF) {
        java.util.Map<String, com.aquarius.dto.BilancioGroup> mastri = new java.util.LinkedHashMap<>();
        java.util.Map<String, com.aquarius.dto.BilancioGroup> gruppi = new java.util.LinkedHashMap<>();
        for (BilancioLine l : lines) {
            String code = l.getAccount() != null ? l.getAccount() : "";
            String mCode = code.length() >= 2 ? code.substring(0, 2) : code;
            String gCode = code.length() >= 4 ? code.substring(0, 4) : code;

            com.aquarius.dto.BilancioGroup mastro = mastri.get(mCode);
            if (mastro == null) {
                mastro = new com.aquarius.dto.BilancioGroup(mCode, descOf(accByCode, mCode, "Mastro " + mCode), 1);
                mastri.put(mCode, mastro);
            }
            com.aquarius.dto.BilancioGroup gruppo = gruppi.get(gCode);
            if (gruppo == null) {
                gruppo = new com.aquarius.dto.BilancioGroup(gCode, descOf(accByCode, gCode, "Gruppo " + gCode), 2);
                gruppi.put(gCode, gruppo);
                mastro.addSubGroup(gruppo);
            }
            // progressivi: sempre (C/F inclusi)
            mastro.addTotals(l.getTotDare(), l.getTotAvere());
            gruppo.addTotals(l.getTotDare(), l.getTotAvere());
            // display foglia: rispetta il toggle C/F
            if (showCF || !l.isCustomerOrSupplier()) {
                gruppo.addLine(l);
            } else {
                gruppo.markHiddenCF();
            }
        }
        return new java.util.ArrayList<>(mastri.values());
    }

    private String descOf(Map<String, Account> accByCode, String code, String fallback) {
        Account a = accByCode.get(code);
        String d = a != null ? a.getDescription() : null;
        return (d != null && !d.trim().isEmpty()) ? d.trim() : fallback;
    }

    private String trimUpper(String s) {
        return s == null ? null : s.trim().toUpperCase(java.util.Locale.ROOT);
    }

    /** Mappa codice conto (trimmed) → Account, per l'anno/società correnti. */
    private Map<String, Account> accountByCode() {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        return accountRepository.findAllByYearAndSociety(anno, soc).stream()
            .filter(a -> a.getCode() != null)
            .collect(Collectors.toMap(
                a -> a.getCode().trim(),
                a -> a,
                (a, b) -> a));
    }

    // ─────────────────────────────────────────────── PARTITARI ─────────
    @GetMapping("/partitari/clienti")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String partitariClienti(@RequestParam(value = "q", required = false) String q,
                                   @RequestParam(value = "page", required = false) Integer page,
                                   @RequestParam(value = "size", required = false) Integer size,
                                   @RequestParam(value = "sort", required = false) String sort,
                                   @RequestParam(value = "dir", required = false) String dir,
                                   Model model,
                                   @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        ListParams lp = ListParams.of(page, size, sort, dir,
            java.util.Set.of("partyCode", "partyName", "invoiceNo", "dueDate", "totalAmount", "paidAmount"),
            "dueDate", "asc");
        Page<PartitaCliente> partite = partitaClienteRepository.search(soc, anno, q, lp.toPageable());
        model.addAttribute("partite", partite);
        model.addAttribute("pageObj", partite);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
        model.addAttribute("anno", anno);
        model.addAttribute("tipo", "clienti");
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/partitari/clienti", principal.getUsername()));
        return "contabilita/partitari";
    }

    @GetMapping("/partitari/fornitori")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String partitariFornitori(@RequestParam(value = "q", required = false) String q,
                                     @RequestParam(value = "page", required = false) Integer page,
                                     @RequestParam(value = "size", required = false) Integer size,
                                     @RequestParam(value = "sort", required = false) String sort,
                                     @RequestParam(value = "dir", required = false) String dir,
                                     Model model,
                                     @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();
        ListParams lp = ListParams.of(page, size, sort, dir,
            java.util.Set.of("partyCode", "partyName", "invoiceNo", "dueDate", "totalAmount", "paidAmount"),
            "dueDate", "asc");
        Page<PartitaFornitore> partite = partitaFornitoreRepository.search(soc, anno, q, lp.toPageable());
        model.addAttribute("partite", partite);
        model.addAttribute("pageObj", partite);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("size", lp.getSize());
        model.addAttribute("sort", lp.getSort());
        model.addAttribute("dir", lp.getDir());
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
