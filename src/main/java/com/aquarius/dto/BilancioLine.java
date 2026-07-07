package com.aquarius.dto;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Riga di bilancio: conto + descrizione + totali Dare/Avere + saldo, con la
 * classificazione di bilancio (sezione P=Patrimoniale / E=Economico, dal campo
 * CONTI.CON_POSBIL) e il tipo conto (C=cliente / F=fornitore, da CON_TIPOCO).
 *
 * La collocazione a sezioni contrapposte deriva dal SEGNO del saldo:
 *   Patrimoniale + saldo Dare (&gt;=0) → Attività;  saldo Avere (&lt;0) → Passività
 *   Economico    + saldo Dare (&gt;=0) → Costi;      saldo Avere (&lt;0) → Ricavi
 * (stessa logica del report VFP fanni210.prg).
 */
@Getter
public class BilancioLine {
    private final String account;
    private final String description;
    private final BigDecimal totDare;
    private final BigDecimal totAvere;
    private final BigDecimal saldo;      // Dare - Avere
    private final String section;        // "P" | "E" | null (non classificato)
    private final String accountType;    // "C" | "F" | altro/null

    public BilancioLine(String account, String description,
                        BigDecimal totDare, BigDecimal totAvere,
                        String section, String accountType) {
        this.account = account;
        this.description = description;
        this.totDare  = totDare  != null ? totDare  : BigDecimal.ZERO;
        this.totAvere = totAvere != null ? totAvere : BigDecimal.ZERO;
        this.saldo = this.totDare.subtract(this.totAvere);
        this.section = section;
        this.accountType = accountType;
    }

    /** Importo da mostrare nel prospetto (sempre positivo). */
    public BigDecimal getDisplayAmount() {
        return saldo.abs();
    }

    /** true se il saldo è dalla parte del Dare (Attività o Costi). */
    public boolean isDareSide() {
        return saldo.signum() >= 0;
    }

    public boolean isPatrimoniale() { return "P".equals(section); }
    public boolean isEconomico()    { return "E".equals(section); }
    public boolean isCustomerOrSupplier() {
        return "C".equals(accountType) || "F".equals(accountType);
    }
}
