package com.aquarius.dto;

import com.aquarius.entity.tenant.MovContabile;
import lombok.Getter;

import java.math.BigDecimal;

/** Riga di mastrino: movimento + saldo progressivo (scalare). */
@Getter
public class LedgerRow {
    private final MovContabile mov;
    private final BigDecimal dare;
    private final BigDecimal avere;
    private final BigDecimal runningBalance;

    public LedgerRow(MovContabile mov, BigDecimal runningBalance) {
        this.mov = mov;
        boolean isDare = "D".equalsIgnoreCase(mov.getMovementType());
        BigDecimal imp = mov.getAmount() != null ? mov.getAmount() : BigDecimal.ZERO;
        this.dare  = isDare ? imp : BigDecimal.ZERO;
        this.avere = isDare ? BigDecimal.ZERO : imp;
        this.runningBalance = runningBalance;
    }
}
