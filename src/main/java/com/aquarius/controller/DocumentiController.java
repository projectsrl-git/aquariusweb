package com.aquarius.controller;

import com.aquarius.context.FiscalContext;
import com.aquarius.dto.documenti.DocumentType;
import com.aquarius.dto.documenti.DocumentoCollegato;
import com.aquarius.dto.documenti.DocumentoRiga;
import com.aquarius.dto.documenti.DocumentoTestata;
import com.aquarius.repository.tenant.DocumentArchiveDao;
import com.aquarius.repository.tenant.DocumentArchiveDao.Filtri;
import com.aquarius.repository.tenant.DocumentArchiveDao.SortKey;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.service.BreadcrumbService.Crumb;
import com.aquarius.web.ListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Cruscotto "Ristampa documenti" — consultazione unificata di TUTTI gli
 * archivi documento + tracciabilita' ordine ↔ DDT ↔ fattura. Porting del
 * form VFP MENU_RISTAMPA_DOC (il modulo piu' usato del gestionale).
 * Sola lettura: la ristampa fisica (.frx), l'invio e-mail e la generazione
 * FE restano sul gestionale.
 *
 * Nota UI: questo cruscotto ha filtri PROPRI (tipo, periodo, numeri, ...)
 * oltre a q/size/sort/dir; i link di ordinamento/paginazione sono costruiti
 * localmente per preservarli tutti (i fragment condivisi propagano solo la
 * quaterna standard — contratto invariato).
 */
@Controller
@RequestMapping("/documenti")
@RequiredArgsConstructor
@Slf4j
public class DocumentiController {

    private final DocumentArchiveDao dao;
    private final BreadcrumbService breadcrumbService;
    private final FiscalContext fiscalContext;

    @GetMapping
    public String list(@RequestParam(value = "tipo", required = false) String tipo,
                       @RequestParam(value = "anno", required = false) String anno,
                       @RequestParam(value = "numDa", required = false) String numDa,
                       @RequestParam(value = "numA", required = false) String numA,
                       @RequestParam(value = "dataDa", required = false) String dataDa,
                       @RequestParam(value = "dataA", required = false) String dataA,
                       @RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "rif", required = false) String rif,
                       @RequestParam(value = "art", required = false) String art,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "dir", required = false) String dir,
                       Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        DocumentType dt = DocumentType.from(tipo);
        String annoEff = (anno == null || anno.trim().isEmpty())
            ? fiscalContext.getFiscalYear() : anno.trim();

        int pageEff = page == null || page < 0 ? 0 : page;
        int sizeEff = size == null || !ListParams.PAGE_SIZE_OPTIONS.contains(size) ? 20 : size;
        SortKey sortKey = SortKey.from(sort);
        boolean asc = "asc".equalsIgnoreCase(dir); // default desc (piu' recenti in alto)

        Filtri filtri = new Filtri(
            fiscalContext.getSocietyCode(), annoEff,
            trim(numDa), trim(numA),
            toLegacyDate(dataDa), toLegacyDate(dataA),
            trim(q), trim(rif), trim(art));

        long total = dao.count(dt, filtri);
        List<DocumentoTestata> content =
            dao.search(dt, filtri, sortKey, asc, pageEff, sizeEff);
        Page<DocumentoTestata> result =
            new PageImpl<>(content, PageRequest.of(pageEff, sizeEff), total);

        // query-string dei soli FILTRI (senza page/sort/dir/size): base per i
        // link di ordinamento e paginazione del template.
        String filterQs = UriComponentsBuilder.newInstance()
            .queryParam("tipo", dt.name())
            .queryParamIfPresent("anno", opt(anno))
            .queryParamIfPresent("numDa", opt(numDa))
            .queryParamIfPresent("numA", opt(numA))
            .queryParamIfPresent("dataDa", opt(dataDa))
            .queryParamIfPresent("dataA", opt(dataA))
            .queryParamIfPresent("q", opt(q))
            .queryParamIfPresent("rif", opt(rif))
            .queryParamIfPresent("art", opt(art))
            .build().encode().getQuery();

        model.addAttribute("tipi", DocumentType.values());
        model.addAttribute("tipo", dt);
        model.addAttribute("anno", annoEff);
        model.addAttribute("numDa", nvl(numDa));
        model.addAttribute("numA", nvl(numA));
        model.addAttribute("dataDa", nvl(dataDa));
        model.addAttribute("dataA", nvl(dataA));
        model.addAttribute("q", nvl(q));
        model.addAttribute("rif", nvl(rif));
        model.addAttribute("art", nvl(art));
        model.addAttribute("documenti", result);
        model.addAttribute("page", pageEff);
        model.addAttribute("size", sizeEff);
        model.addAttribute("sort", sortKey.name());
        model.addAttribute("dir", asc ? "asc" : "desc");
        model.addAttribute("filterQs", filterQs);
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/documenti", principal.getUsername()));
        return "documenti/list";
    }

    @GetMapping("/{tipo}/{id}")
    public String detail(@PathVariable("tipo") String tipo, @PathVariable("id") String id,
                         Model model,
                         @AuthenticationPrincipal AquariusPrincipal principal) {
        DocumentType dt = DocumentType.from(tipo);
        DocumentoTestata head = dao.findHead(dt, id);
        if (head == null) {
            throw new IllegalArgumentException("Documento non trovato: " + tipo + "/" + id);
        }
        List<DocumentoRiga> rows =
            (head.getAggancio() != null && !head.getAggancio().trim().isEmpty())
                ? dao.findRows(dt, head.getAggancio())
                : List.of();

        // Tracciabilita' (verificata per la catena vendite ORD/BOL/FAT)
        List<DocumentoCollegato> collegati = switch (dt) {
            case ORD -> dao.linkedForOrder(head.getNumero(), head.getData());
            case BOL -> dao.linkedForDdt(head.getAggancio());
            case FAT -> dao.linkedForInvoice(head.getNumero(), head.getData());
            default -> List.of();
        };

        model.addAttribute("tipo", dt);
        model.addAttribute("doc", head);
        model.addAttribute("rows", rows);
        model.addAttribute("collegati", collegati);
        List<Crumb> crumbs = new ArrayList<>(
            breadcrumbService.forUrl("/documenti", principal.getUsername()));
        crumbs.add(new Crumb(dt.name() + " " +
            (head.getNumero() == null ? id : head.getNumero().trim()), null));
        model.addAttribute("breadcrumbs", crumbs);
        return "documenti/detail";
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private static String trim(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static java.util.Optional<String> opt(String s) {
        String t = trim(s);
        return java.util.Optional.ofNullable(t);
    }

    /** HTML date input (yyyy-MM-dd) → formato legacy yyyy/MM/dd; altri formati invariati. */
    private static String toLegacyDate(String s) {
        String t = trim(s);
        if (t == null) return null;
        return t.length() == 10 && t.charAt(4) == '-' && t.charAt(7) == '-'
            ? t.replace('-', '/') : t;
    }
}
