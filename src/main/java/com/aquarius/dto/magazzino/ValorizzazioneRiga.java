package com.aquarius.dto.magazzino;

import java.math.BigDecimal;

/**
 * Una riga della valorizzazione magazzino (query §3.2 del documento di
 * specifica): giacenza + valore FIFO di un articolo alla data di riferimento.
 */
public record ValorizzazioneRiga(
    String codiceArticolo,
    BigDecimal giacenza,
    BigDecimal qtaValorizzata,
    BigDecimal valore,
    BigDecimal prezzoMedio,
    boolean cambioMancante,
    /** OK / GIACENZA_NEGATIVA / NON_VALORIZZATA / VALORIZZAZIONE_PARZIALE / CAMBIO_MANCANTE */
    String stato
) {}
