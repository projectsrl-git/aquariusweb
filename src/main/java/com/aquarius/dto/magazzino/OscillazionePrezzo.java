package com.aquarius.dto.magazzino;

import java.math.BigDecimal;

/**
 * Statistica di oscillazione del prezzo d'acquisto negli ultimi 6 mesi
 * (query §3.4), con proiezione a +6 mesi via regressione lineare.
 * La proiezione è una EURISTICA: in UI va presentata come indicazione
 * di tendenza, mai come previsione certa.
 */
public record OscillazionePrezzo(
    String codiceArticolo,
    int nCarichi,
    BigDecimal prezzoMin6m,
    BigDecimal prezzoMax6m,
    BigDecimal prezzoMedio6m,
    BigDecimal devStd6m,
    BigDecimal coeffVariazione,
    /** €/mese: positivo = in aumento, negativo = in calo. */
    BigDecimal trendMensile,
    BigDecimal prezzoProiettato6m
) {}
