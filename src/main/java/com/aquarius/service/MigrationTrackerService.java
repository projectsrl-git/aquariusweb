package com.aquarius.service;

import com.aquarius.dto.MigrationRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Legge il migration tracker CSV dal classpath
 * ({@code migration/scx_migration_tracker.csv}) e lo espone come righe.
 * Parser CSV minimale ma quote-aware (campi tra virgolette con virgole interne).
 */
@Service
@Slf4j
public class MigrationTrackerService {

    private static final String RESOURCE = "migration/scx_migration_tracker.csv";

    public List<MigrationRow> loadRows() {
        List<MigrationRow> rows = new ArrayList<>();
        ClassPathResource res = new ClassPathResource(RESOURCE);
        if (!res.exists()) {
            log.warn("Migration tracker CSV non trovato: {}", RESOURCE);
            return rows;
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                if (header) { header = false; continue; }   // salta intestazione
                rows.add(new MigrationRow(parseCsvLine(line)));
            }
        } catch (Exception e) {
            log.error("Errore lettura migration tracker CSV", e);
        }
        return rows;
    }

    /** Split di una riga CSV rispettando le virgolette doppie. */
    private static String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(ch);
            } else {
                if (ch == '"') inQuotes = true;
                else if (ch == ',') { out.add(cur.toString()); cur.setLength(0); }
                else cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
