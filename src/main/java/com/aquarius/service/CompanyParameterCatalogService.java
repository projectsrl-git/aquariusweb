package com.aquarius.service;

import com.aquarius.dto.parametri.CompanyParameterEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Legge il catalogo dei parametri aziendali dal classpath
 * ({@code parametri/parametri_aziendali_catalog.csv}) — output dell'analisi
 * DEEP di MENU_AZI000.SCX (schema e metodo nel README accanto al CSV).
 * Parser CSV quote-aware (campi tra virgolette con virgole e doppi apici
 * raddoppiati, RFC 4180), stesso approccio di {@link MigrationTrackerService}.
 */
@Service
@Slf4j
public class CompanyParameterCatalogService {

    private static final String RESOURCE = "parametri/parametri_aziendali_catalog.csv";
    private static final int COLUMNS = 11;

    public List<CompanyParameterEntry> loadCatalog() {
        List<CompanyParameterEntry> out = new ArrayList<>();
        ClassPathResource res = new ClassPathResource(RESOURCE);
        if (!res.exists()) {
            log.warn("Catalogo parametri aziendali non trovato: {}", RESOURCE);
            return out;
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                if (header) { header = false; continue; }
                String[] f = parseCsvLine(line);
                if (f.length < COLUMNS) continue;
                CompanyParameterEntry e = new CompanyParameterEntry();
                e.setGroup(f[0]);
                e.setObjectName(f[1]);
                e.setLabel(f[2]);
                e.setTableColumn(f[3]);
                e.setType(f[4]);
                e.setAllowedValues(f[5]);
                e.setPurpose(f[6]);
                e.setHowItWorks(f[7]);
                e.setUsedIn(f[8]);
                e.setConfidence(f[9]);
                e.setNotes(f[10]);
                // colonna AZI_ per il lookup del valore
                String tc = f[3];
                int dot = tc.indexOf('.');
                e.setColumn(dot > 0 ? tc.substring(dot + 1).trim() : "");
                out.add(e);
            }
        } catch (Exception ex) {
            log.error("Errore lettura catalogo parametri aziendali", ex);
        }
        return out;
    }

    /** Parser CSV RFC-4180: virgole nei campi tra virgolette, "" = doppio apice. */
    static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"'); i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') inQuotes = true;
                else if (c == ',') { fields.add(cur.toString()); cur.setLength(0); }
                else cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields.toArray(new String[0]);
    }
}
