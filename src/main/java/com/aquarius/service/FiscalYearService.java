package com.aquarius.service;

import com.aquarius.context.FiscalContext;
import com.aquarius.entity.tenant.ParameterItem;
import com.aquarius.repository.tenant.ParameterRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Carica gli anni contabili disponibili dalla tabella PARA con prefisso
 * "ANN" (categoria parametri del catalogo).
 *
 * <h3>Formato del codice</h3>
 * <p>Il codice è fisso: {@code ANN + <società 2 char> + <anno 4 char>}
 * — totale 9 caratteri. Esempi:</p>
 * <pre>
 *   CODICE='ANN012026'  DESCRI='Esercizio 2026'   DISATTIVO=0
 *   CODICE='ANN012025'  DESCRI='Esercizio 2025'   DISATTIVO=0
 *   CODICE='ANN012024'  DESCRI='Esercizio 2024 (chiuso)'  DISATTIVO=0
 * </pre>
 *
 * <p>Il filtro applicato è {@code CODICE LIKE 'ANN<society>%'} per
 * mostrare solo gli anni della società corrente (equivalente del
 * {@code PUB_CODSOC} VFP).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalYearService {

    private final ParameterRepository parameterRepository;
    private final FiscalContext fiscalContext;

    public static final String PREFIX = "ANN";

    /**
     * Anni contabili abilitati per la società corrente, ordinati per anno
     * descendente (più recente in cima). Cache per evitare query ripetute.
     */
    @Cacheable(value = "fiscalYears", key = "#root.target.cacheKey()")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<FiscalYear> listAvailable() {
        String fullPrefix = PREFIX + fiscalContext.getSocietyCode();
        List<ParameterItem> raw = parameterRepository.findByPrefix(fullPrefix);
        List<FiscalYear> years = new ArrayList<>(raw.size());

        for (ParameterItem p : raw) {
            if (Boolean.TRUE.equals(p.getDisattivo())) continue;
            String year = extractYearFromCode(p.getCodice());
            if (year == null) continue;
            years.add(new FiscalYear(
                year,
                p.getDescri() != null ? p.getDescri().trim() : "Esercizio " + year,
                false
            ));
        }

        years.sort(Comparator.comparing(FiscalYear::getYear).reversed());
        log.info("Anni contabili caricati per società {}: {}",
                 fiscalContext.getSocietyCode(), years.size());
        return years;
    }

    /**
     * Chiave di cache: include la società per non mischiare anni di società
     * diverse (anche se per ora abbiamo sempre 01).
     */
    public String cacheKey() {
        return "soc:" + fiscalContext.getSocietyCode();
    }

    /**
     * Default per la prima scelta: anno corrente se presente in PARA,
     * altrimenti il più recente disponibile.
     */
    public Optional<FiscalYear> defaultYear() {
        List<FiscalYear> all = listAvailable();
        if (all.isEmpty()) return Optional.empty();
        String current = String.valueOf(LocalDate.now().getYear());
        return all.stream()
            .filter(y -> current.equals(y.getYear()))
            .findFirst()
            .or(() -> Optional.of(all.get(0)));
    }

    public Optional<FiscalYear> findByYear(String year) {
        if (year == null) return Optional.empty();
        return listAvailable().stream()
            .filter(y -> year.equals(y.getYear()))
            .findFirst();
    }

    /**
     * Estrae l'anno dal codice PARA. Il formato è fisso a 9 caratteri
     * ({@code ANN + 2 società + 4 anno}), quindi prendiamo gli ultimi 4
     * caratteri del codice se sono cifre. Gestiamo anche casi degeneri
     * (codice più corto) tornando null.
     *
     * <pre>
     *   "ANN012026" → "2026"
     *   "ANN022024" → "2024"
     *   "ANN01"     → null   (codice malformato)
     * </pre>
     */
    static String extractYearFromCode(String codice) {
        if (codice == null) return null;
        String c = codice.trim();
        if (c.length() < 4) return null;
        String tail = c.substring(c.length() - 4);
        for (int i = 0; i < 4; i++) {
            if (!Character.isDigit(tail.charAt(i))) return null;
        }
        return tail;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FiscalYear {
        private String year;
        private String description;
        private boolean closed;
    }
}
