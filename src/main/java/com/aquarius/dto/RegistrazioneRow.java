package com.aquarius.dto;

import com.aquarius.entity.tenant.MovContabile;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Testata di registrazione aggregata per la lista primanota: una riga per
 * numero registrazione, con tipo operazione, importo (somma Dare), e l'elenco
 * dei conti coinvolti (per i badge). Costruita in Java dalle righe MOV_CONT
 * della registrazione, così la lista mostra UNA riga per scrittura invece di
 * una per movimento.
 */
@Getter
public class RegistrazioneRow {
    private final String registrationNo;
    private final String registrationDate;   // yyyy/MM/dd (stringa legacy)
    private final String documentNo;
    private final String description;
    private final String operationType;      // codice MOV_TOP
    private String operationTypeDesc;         // risolto da TAB_TOPCONT
    private final BigDecimal amount;          // totale Dare della registrazione
    private final List<String[]> accounts;    // [codice, descrizione] conti coinvolti

    public RegistrazioneRow(String registrationNo, String registrationDate,
                            String documentNo, String description,
                            String operationType, BigDecimal amount,
                            List<String[]> accounts) {
        this.registrationNo = registrationNo;
        this.registrationDate = registrationDate;
        this.documentNo = documentNo;
        this.description = description;
        this.operationType = operationType;
        this.amount = amount;
        this.accounts = accounts;
    }

    public void setOperationTypeDesc(String d) { this.operationTypeDesc = d; }

    /**
     * Aggrega una lista di righe MOV_CONT (già ordinate) in testate di
     * registrazione. Mantiene l'ordine di primo arrivo.
     */
    public static List<RegistrazioneRow> fromMovements(
            List<MovContabile> movs,
            java.util.Map<String, String> accountNames) {
        // Raggruppa per numero registrazione preservando l'ordine
        java.util.Map<String, List<MovContabile>> byReg = new java.util.LinkedHashMap<>();
        for (MovContabile m : movs) {
            byReg.computeIfAbsent(m.getRegistrationNo(), k -> new ArrayList<>()).add(m);
        }
        List<RegistrazioneRow> out = new ArrayList<>();
        for (var e : byReg.entrySet()) {
            List<MovContabile> rows = e.getValue();
            MovContabile first = rows.get(0);
            BigDecimal totDare = rows.stream()
                .filter(r -> "D".equalsIgnoreCase(r.getMovementType()))
                .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Conti coinvolti (distinti, ordine di comparsa), con descrizione
            Set<String> seen = new LinkedHashSet<>();
            List<String[]> accounts = new ArrayList<>();
            for (MovContabile r : rows) {
                String code = r.getAccount() != null ? r.getAccount().trim() : null;
                if (code != null && !code.isEmpty() && seen.add(code)) {
                    accounts.add(new String[]{ code, accountNames.getOrDefault(code, "") });
                }
            }
            out.add(new RegistrazioneRow(
                e.getKey(),
                first.getRegistrationDate(),
                first.getDocumentNo(),
                first.getDescription() != null ? first.getDescription() : first.getShortDescription(),
                first.getOperationType(),
                totDare,
                accounts));
        }
        return out;
    }
}
