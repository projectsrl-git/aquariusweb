package com.aquarius.controller;

import com.aquarius.entity.tenant.CustomReport;
import com.aquarius.repository.tenant.CustomReportRepository;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.CustomReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestione delle query personalizzate (porting da CReaM).
 *
 * Tutti gli endpoint sono tenant-scoped: il routing DataSource attivo
 * indirizza al DB della società corrente (vedi TenantRequestFilter).
 *
 * Endpoint:
 *   GET  /custom-reports                  → lista report attivi (per categoria)
 *   GET  /custom-reports/new              → form creazione
 *   POST /custom-reports                  → salvataggio nuovo report
 *   GET  /custom-reports/{id}             → dettaglio + form parametri se richiesti
 *   POST /custom-reports/{id}/execute     → esecuzione query (AJAX → JSON)
 *   GET  /custom-reports/{id}/edit        → form modifica
 *   POST /custom-reports/{id}             → salvataggio modifica
 *   POST /custom-reports/{id}/delete      → soft-delete (isActive=false)
 *   POST /custom-reports/validate         → validazione AJAX della query
 */
@Controller
@RequestMapping("/custom-reports")
@RequiredArgsConstructor
public class CustomReportController {

    private final CustomReportRepository reportRepository;
    private final CustomReportService reportService;

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String list(Model model) {
        List<CustomReport> reports = reportRepository.findByIsActiveTrueOrderByCategoryAscNameAsc();
        model.addAttribute("reports", reports);
        return "custom-reports/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("report", new CustomReport());
        model.addAttribute("tables", reportService.getAvailableTables());
        return "custom-reports/form";
    }

    @PostMapping
    @Transactional(transactionManager = "tenantTransactionManager")
    public String save(@Valid @ModelAttribute CustomReport report,
                       @AuthenticationPrincipal AquariusPrincipal principal,
                       RedirectAttributes ra) {
        if (report.getId() == null) {
            report.setCreatedBy(principal.getUsername());
        }
        reportRepository.save(report);
        ra.addFlashAttribute("success", "Report '" + report.getName() + "' salvato.");
        return "redirect:/custom-reports";
    }

    @GetMapping("/{id}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String detail(@PathVariable Long id, Model model) {
        CustomReport report = reportRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Report non trovato: " + id));
        model.addAttribute("report", report);
        model.addAttribute("parameters", reportService.extractParameters(report.getSqlQuery()));
        return "custom-reports/detail";
    }

    @GetMapping("/{id}/edit")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String editForm(@PathVariable Long id, Model model) {
        CustomReport report = reportRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Report non trovato: " + id));
        model.addAttribute("report", report);
        model.addAttribute("tables", reportService.getAvailableTables());
        return "custom-reports/form";
    }

    @PostMapping("/{id}/delete")
    @Transactional(transactionManager = "tenantTransactionManager")
    public String softDelete(@PathVariable Long id, RedirectAttributes ra) {
        reportRepository.findById(id).ifPresent(r -> {
            r.setIsActive(false);
            reportRepository.save(r);
        });
        ra.addFlashAttribute("success", "Report disattivato.");
        return "redirect:/custom-reports";
    }

    // ─── REST endpoints (AJAX) ──────────────────────────────────────────

    @PostMapping("/{id}/execute")
    @ResponseBody
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> execute(@PathVariable Long id,
                                       @RequestBody(required = false) Map<String, Object> params) {
        CustomReport report = reportRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Report non trovato: " + id));

        Map<String, Object> result = reportService.executeQuery(
            report.getSqlQuery(),
            params == null ? new HashMap<>() : params
        );

        if (Boolean.TRUE.equals(result.get("success"))) {
            report.recordExecution();
            reportRepository.save(report);
        }
        return result;
    }

    @PostMapping("/validate")
    @ResponseBody
    public Map<String, Object> validate(@RequestBody Map<String, String> body) {
        return reportService.validateQuery(body.get("sql"));
    }

    @GetMapping("/tables")
    @ResponseBody
    public List<String> tables() {
        return reportService.getAvailableTables();
    }

    @GetMapping("/tables/{tableName}/columns")
    @ResponseBody
    public List<Map<String, String>> tableColumns(@PathVariable String tableName) {
        return reportService.getTableColumns(tableName);
    }
}
