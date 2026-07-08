package com.aquarius.dto.magazzino;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Giacenza aggregata per articolo+magazzino (SUM su U_MAG_GG, come i PRG
 * legacy). {@code rowCount} espone il numero di righe fisiche aggregate:
 * serve a segnalare onestamente le "righe fantasma" (totale zero ma righe
 * storiche presenti) invece di nasconderle.
 */
@Data
@AllArgsConstructor
public class GiacenzaRiga {
    private String articleCode;
    private String articleDescription;
    private String warehouseCode;
    private BigDecimal totalQuantity;
    private Long rowCount;

    public boolean isNegative() {
        return totalQuantity != null && totalQuantity.signum() < 0;
    }

    /** Righe fisiche presenti ma saldo zero: anomalia da mostrare, non da nascondere. */
    public boolean isZeroWithHistory() {
        return totalQuantity != null && totalQuantity.signum() == 0
            && rowCount != null && rowCount > 0;
    }
}
