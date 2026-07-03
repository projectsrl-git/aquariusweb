package com.aquarius.controller;

import com.aquarius.dto.magazzino.OscillazionePrezzo;
import com.aquarius.dto.magazzino.StratoFifo;
import com.aquarius.repository.tenant.WarehouseValuationDao.DateBase;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.BreadcrumbService;
import com.aquarius.service.WarehouseValuationExcelExporter;
import com.aquarius.service.WarehouseValuationService;
import com.aquarius.service.WarehouseValuationService.ValuationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard "Valorizzazione magazzino a una data".
 *
 * <p>Pagina shell + endpoint JSON per il refresh asincrono. Il calcolo è
 * FIFO sui movimenti {@code U_MAG_MO}, con conversione valuta as-of da
 * {@code u_vva_ch} — vedi {@link com.aquarius.repository.tenant.WarehouseValuationDao}.</p>
 */
@Controller
@RequestMapping("/magazzino/valorizzazione")
@RequiredArgsConstructor
@Slf4j
public class WarehouseValuationController {

    private final WarehouseValuationService service;
    private final WarehouseValuationExcelExporter excelExporter;
    private final BreadcrumbService breadcrumbService;

    private static final String DEFAULT_MAG = "SEDE";

    /** Pagina della dashboard (shell: i dati arrivano via /dati). */
    @GetMapping
    public String page(Model model,
                       @AuthenticationPrincipal AquariusPrincipal principal) {
        List<String> magazzini = service.elencoMagazzini();
        model.addAttribute("magazzini", magazzini);
        model.addAttribute("defaultMag",
            magazzini.contains(DEFAULT_MAG) ? DEFAULT_MAG
                : (magazzini.isEmpty() ? "" : magazzini.get(0)));
        model.addAttribute("oggi", LocalDate.now().toString());
        model.addAttribute("breadcrumbs",
            breadcrumbService.forUrl("/magazzino/valorizzazione", principal.getUsername()));
        return "magazzino/valorizzazione";
    }

    /** Dataset completo: righe + KPI + ABC + anomalie + volatilità. */
    @GetMapping("/dati")
    @ResponseBody
    public ValuationResult dati(@RequestParam(value = "data", required = false) String data,
                                @RequestParam(value = "codmag", required = false) String codmag,
                                @RequestParam(value = "articolo", required = false) String articolo,
                                @RequestParam(value = "base", defaultValue = "DOCU") String base) {
        LocalDate dataRif = parseData(data);
        String mag = normMag(codmag);
        DateBase db = DateBase.from(base);
        return (articolo == null || articolo.isBlank())
            ? service.valorizzaMagazzino(dataRif, mag, db)
            : service.valorizzaArticolo(dataRif, mag, articolo.trim(), db);
    }

    /** Drill-down: strati FIFO di un articolo. */
    @GetMapping("/strati")
    @ResponseBody
    public List<StratoFifo> strati(@RequestParam("articolo") String articolo,
                                   @RequestParam(value = "data", required = false) String data,
                                   @RequestParam(value = "codmag", required = false) String codmag,
                                   @RequestParam(value = "base", defaultValue = "DOCU") String base) {
        return service.strati(parseData(data), normMag(codmag), articolo.trim(), DateBase.from(base));
    }

    /** Oscillazione prezzi del singolo articolo (per il grafico tendenza). */
    @GetMapping("/prezzi")
    @ResponseBody
    public List<OscillazionePrezzo> prezzi(@RequestParam("articolo") String articolo,
                                           @RequestParam(value = "data", required = false) String data,
                                           @RequestParam(value = "codmag", required = false) String codmag,
                                           @RequestParam(value = "base", defaultValue = "DOCU") String base) {
        return service.oscillazioneArticolo(parseData(data), normMag(codmag),
                                            articolo.trim(), DateBase.from(base));
    }

    /** Autocomplete articolo. */
    @GetMapping("/articoli")
    @ResponseBody
    public List<String> articoli(@RequestParam(value = "q", defaultValue = "") String q,
                                 @RequestParam(value = "codmag", required = false) String codmag) {
        return service.cercaArticoli(normMag(codmag), q);
    }

    /** Export Excel: dati + metriche + anomalie + oscillazione prezzi. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(value = "data", required = false) String data,
                                         @RequestParam(value = "codmag", required = false) String codmag,
                                         @RequestParam(value = "articolo", required = false) String articolo,
                                         @RequestParam(value = "base", defaultValue = "DOCU") String base)
            throws IOException {
        LocalDate dataRif = parseData(data);
        String mag = normMag(codmag);
        DateBase db = DateBase.from(base);
        ValuationResult result = (articolo == null || articolo.isBlank())
            ? service.valorizzaMagazzino(dataRif, mag, db)
            : service.valorizzaArticolo(dataRif, mag, articolo.trim(), db);

        byte[] bytes = excelExporter.export(result);
        String filename = "Valorizzazione_" + mag + "_" + dataRif + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    private static LocalDate parseData(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private String normMag(String codmag) {
        if (codmag != null && !codmag.isBlank()) return codmag.trim();
        List<String> all = service.elencoMagazzini();
        return all.contains(DEFAULT_MAG) ? DEFAULT_MAG : (all.isEmpty() ? DEFAULT_MAG : all.get(0));
    }
}
