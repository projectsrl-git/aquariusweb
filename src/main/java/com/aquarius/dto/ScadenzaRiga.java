package com.aquarius.dto;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Riga di scadenziario per anagrafica (cliente/fornitore) con aging degli
 * importi aperti: a scadere + fasce di scaduto (0-30, 31-60, 61-90, oltre 90 gg).
 */
@Getter
public class ScadenzaRiga {

    private final String partyCode;
    private final String partyName;
    private BigDecimal aScadere = BigDecimal.ZERO;
    private BigDecimal sc0_30   = BigDecimal.ZERO;
    private BigDecimal sc31_60  = BigDecimal.ZERO;
    private BigDecimal sc61_90  = BigDecimal.ZERO;
    private BigDecimal scOltre90 = BigDecimal.ZERO;

    public ScadenzaRiga(String partyCode, String partyName) {
        this.partyCode = partyCode;
        this.partyName = partyName;
    }

    /** Aggiunge un importo aperto nella fascia corretta in base ai giorni di scaduto. */
    public void add(long giorniScaduto, BigDecimal importo) {
        if (importo == null) return;
        if (giorniScaduto <= 0)       aScadere  = aScadere.add(importo);
        else if (giorniScaduto <= 30) sc0_30    = sc0_30.add(importo);
        else if (giorniScaduto <= 60) sc31_60   = sc31_60.add(importo);
        else if (giorniScaduto <= 90) sc61_90   = sc61_90.add(importo);
        else                          scOltre90 = scOltre90.add(importo);
    }

    public BigDecimal getScadutoTot() {
        return sc0_30.add(sc31_60).add(sc61_90).add(scOltre90);
    }

    public BigDecimal getTotale() {
        return aScadere.add(getScadutoTot());
    }
}
