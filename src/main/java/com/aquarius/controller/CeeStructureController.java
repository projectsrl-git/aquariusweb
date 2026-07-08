package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.repository.tenant.CeeStructureDao;
import com.aquarius.repository.tenant.CeeStructureDao.CeeRow;
import com.aquarius.repository.tenant.CeeStructureDao.ConfluenzaTotale;
import com.aquarius.repository.tenant.CeeStructureDao.ContoMappato;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Viewer READ-ONLY della STRUTTURA del bilancio CEE: voci (BILNEW,
 * nell'ordine di stampa legacy), conti confluenti per voce (U_INT_TT,
 * regola dare/avere), confluenze di totale (U_COR_TT) e pannello anomalie
 * (i tre controlli di ceecont). NIENTE valori calcolati: il motore di
 * calcolo e' riservato a Opus (pseudocodice in resources/cee/README.md).
 */
@Controller
@RequestMapping("/contabilita/bilancio-cee-struttura")
@RequiredArgsConstructor
@Slf4j
public class CeeStructureController {

    private static final int UNMAPPED_LIMIT = 200;
    /** Soglia legacy: VAL(BIL_CODRIG) >= 21600 = conto economico (ceecont). */
    private static final long ECONOMIC_THRESHOLD = 21600;

    private final CeeStructureDao dao;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    @GetMapping
    public String view(@RequestParam(value = "q", required = false) String q,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        String soc = fiscalContext.getSocietyCode();
        String anno = fiscalContext.getFiscalYear();

        List<CeeRow> rows = dao.structure(soc);
        List<ContoMappato> mappings = dao.mappings(soc, anno);
        List<ConfluenzaTotale> edges = dao.totalEdges(soc);

        // indice: voce -> conti che vi confluiscono (dare o avere)
        Map<String, List<ContoMappato>> byRow = new HashMap<>();
        for (ContoMappato m : mappings) {
            if (!m.getRigaDare().isEmpty()) {
                byRow.computeIfAbsent(m.getRigaDare(), k -> new java.util.ArrayList<>()).add(m);
            }
            if (!m.getRigaAvere().isEmpty() && !m.getRigaAvere().equals(m.getRigaDare())) {
                byRow.computeIfAbsent(m.getRigaAvere(), k -> new java.util.ArrayList<>()).add(m);
            }
        }
        // indice: totale -> componenti; e componente -> totali di destinazione
        Map<String, List<ConfluenzaTotale>> composition = new HashMap<>();
        Map<String, List<ConfluenzaTotale>> flowsInto = new HashMap<>();
        for (ConfluenzaTotale e : edges) {
            composition.computeIfAbsent(e.getTotale(), k -> new java.util.ArrayList<>()).add(e);
            flowsInto.computeIfAbsent(e.getRiga(), k -> new java.util.ArrayList<>()).add(e);
        }

        // filtro testuale opzionale su codice/descrizione voce
        String qq = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<CeeRow> visible = qq.isEmpty() ? rows : rows.stream()
            .filter(r -> r.getCodRiga().toLowerCase(Locale.ROOT).contains(qq)
                      || r.getDescrizione().toLowerCase(Locale.ROOT).contains(qq))
            .toList();

        // sezione derivata dalla soglia legacy (SP / CE)
        Map<String, String> section = new LinkedHashMap<>();
        for (CeeRow r : rows) {
            section.put(r.getCodRiga(), sectionOf(r.getCodRiga()));
        }

        // anomalie
        List<CeeStructureDao.ContoNonMappato> unmapped =
            dao.unmappedAccounts(soc, anno, UNMAPPED_LIMIT);
        long unmappedTotal = dao.unmappedAccountsCount(soc, anno);
        List<ContoMappato> broken = dao.brokenMappings(soc);
        List<ConfluenzaTotale> brokenEdges = dao.brokenTotalEdges(soc);

        model.addAttribute("rows", visible);
        model.addAttribute("totRows", rows.size());
        model.addAttribute("byRow", byRow);
        model.addAttribute("composition", composition);
        model.addAttribute("flowsInto", flowsInto);
        model.addAttribute("section", section);
        model.addAttribute("mappingsCount", mappings.size());
        model.addAttribute("edgesCount", edges.size());
        model.addAttribute("unmapped", unmapped);
        model.addAttribute("unmappedTotal", unmappedTotal);
        model.addAttribute("unmappedLimit", UNMAPPED_LIMIT);
        model.addAttribute("broken", broken);
        model.addAttribute("brokenEdges", brokenEdges);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("anno", anno);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/contabilita/bilancio-cee-struttura",
                principal.getUsername()));
        return "contabilita/bilancio-cee-struttura";
    }

    private static String sectionOf(String codRiga) {
        try {
            long v = Long.parseLong(codRiga.trim());
            return v >= ECONOMIC_THRESHOLD ? "CE" : "SP";
        } catch (NumberFormatException e) {
            return "";
        }
    }
}
