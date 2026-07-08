package com.aquarius.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Nodo dell'albero di bilancio a mastri/gruppi (come "Bilancio di verifica").
 * Struttura: Mastro (livello 1, codice 2 char) → Gruppo (livello 2, codice 4 char)
 * → righe sottoconto ({@link BilancioLine}). Progressivi Dare/Avere/Saldo
 * aggregati da TUTTE le righe (clienti/fornitori inclusi), così i totali e la
 * quadratura non dipendono dal toggle di visualizzazione C/F.
 */
@Getter
public class BilancioGroup {

    private final String code;
    private final String description;
    private final int level;                 // 1 = mastro, 2 = gruppo
    private BigDecimal totDare  = BigDecimal.ZERO;
    private BigDecimal totAvere = BigDecimal.ZERO;

    private final List<BilancioGroup> subGroups = new ArrayList<>();  // per mastro: i gruppi
    private final List<BilancioLine>  lines     = new ArrayList<>();  // per gruppo: i sottoconti (visualizzati)

    public BilancioGroup(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }

    /** Accumula nei progressivi (chiamato per OGNI riga, C/F inclusi). */
    public void addTotals(BigDecimal dare, BigDecimal avere) {
        if (dare  != null) totDare  = totDare.add(dare);
        if (avere != null) totAvere = totAvere.add(avere);
    }

    public BigDecimal getSaldo() { return totDare.subtract(totAvere); }

    public void addSubGroup(BilancioGroup g) { subGroups.add(g); }
    public void addLine(BilancioLine l)      { lines.add(l); }

    /** True se, in modalità "nascondi C/F", il gruppo ha righe C/F non mostrate
     *  (i loro importi restano però nei progressivi → quadratura preservata). */
    private boolean hasHiddenCF = false;
    public boolean isHasHiddenCF() { return hasHiddenCF; }
    public void markHiddenCF()     { this.hasHiddenCF = true; }
}
