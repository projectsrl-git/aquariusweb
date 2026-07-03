package com.aquarius.dto.magazzino;

import java.math.BigDecimal;

/**
 * Uno strato FIFO del drill-down articolo (query §3.3): un movimento di
 * carico e quanto di esso è ancora "in giacenza" alla data di riferimento.
 */
public record StratoFifo(
    String dataCarico,
    BigDecimal qtaCarico,
    String valuta,
    BigDecimal prezzoOriginale,
    BigDecimal cambioApplicato,
    BigDecimal prezzoConvertito,
    BigDecimal qtaResiduaInGiacenza
) {}
