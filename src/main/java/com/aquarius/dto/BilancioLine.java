package com.aquarius.dto;

import lombok.Getter;

import java.math.BigDecimal;

/** Riga di bilancio: conto + descrizione + totali Dare/Avere + saldo. */
@Getter
public class BilancioLine {
    private final String account;
    private final String description;
    private final BigDecimal totDare;
    private final BigDecimal totAvere;
    private final BigDecimal saldo;   // Dare - Avere

    public BilancioLine(String account, String description,
                        BigDecimal totDare, BigDecimal totAvere) {
        this.account = account;
        this.description = description;
        this.totDare  = totDare  != null ? totDare  : BigDecimal.ZERO;
        this.totAvere = totAvere != null ? totAvere : BigDecimal.ZERO;
        this.saldo = this.totDare.subtract(this.totAvere);
    }
}
