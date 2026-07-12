package com.aquarius.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Legge l'audit di copertura della migrazione
 * ({@code migration/menu_coverage_audit.csv}): una riga per voce di menu
 * legacy con classificazione MIGRATO / PARZIALE / MANCANTE /
 * NON_APPLICABILE, evidenza e note. Parser quote-aware riusato da
 * {@link CompanyParameterCatalogService}.
 */
@Service
@Slf4j
public class CoverageAuditService {

    private static final String RESOURCE = "migration/menu_coverage_audit.csv";
    private static final int COLUMNS = 9;

    @Data
    public static class Entry {
        private String area;
        private String percorso;
        private String voce;
        private String formOrPrg;
        private String copertura;   // MIGRATO | PARZIALE | MANCANTE | NON_APPLICABILE
        private String evidenza;
        private String reason;
        private String note;
        private String metodo;
    }

    public List<Entry> load() {
        List<Entry> out = new ArrayList<>();
        ClassPathResource res = new ClassPathResource(RESOURCE);
        if (!res.exists()) {
            log.warn("Audit copertura non trovato: {}", RESOURCE);
            return out;
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String line; boolean header = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                if (header) { header = false; continue; }
                String[] f = CompanyParameterCatalogService.parseCsvLine(line);
                if (f.length < COLUMNS) continue;
                Entry e = new Entry();
                e.setArea(f[0]); e.setPercorso(f[1]); e.setVoce(f[2]);
                e.setFormOrPrg(f[3]); e.setCopertura(f[4]); e.setEvidenza(f[5]);
                e.setReason(f[6]); e.setNote(f[7]); e.setMetodo(f[8]);
                out.add(e);
            }
        } catch (Exception ex) {
            log.error("Errore lettura audit copertura", ex);
        }
        return out;
    }
}
