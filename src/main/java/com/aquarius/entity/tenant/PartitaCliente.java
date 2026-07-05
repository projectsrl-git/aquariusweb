package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Partita aperta cliente — riga della tabella {@code PART_CLI} (legacy, READ-ONLY).
 * Una "partita" è una posizione debitoria/creditoria (tipicamente una fattura)
 * con il suo pagato e la scadenza. {@code PAR_TMOV} distingue Dare/Avere.
 * Il residuo è {@code PAR_TOTIM - PAR_PAGATO}.
 */
@Entity
@Table(name = "PART_CLI")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartitaCliente {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "PAR_CODSOC", length = 2)
    private String societyCode;

    @Column(name = "PAR_ANNO", length = 4)
    private String fiscalYear;

    /** Codice anagrafica (cliente). Nota: in PART_CLI la colonna e' PAR_CODCLI. */
    @Column(name = "PAR_CODCLI", length = 13)
    private String partyCode;

    @Column(name = "PAR_RAGSOC", length = 40)
    private String partyName;

    @Column(name = "PAR_NPART")
    private BigDecimal partitaNo;

    @Column(name = "PAR_NREG", length = 10)
    private String registrationNo;

    @Column(name = "PAR_DTREG", length = 10)
    private String registrationDate;

    @Column(name = "PAR_NUMFAT", length = 20)
    private String invoiceNo;

    @Column(name = "PAR_TMOV", length = 1)
    private String movementType;   // 'D' / 'A'

    @Column(name = "PAR_TOTIM")
    private BigDecimal totalAmount;

    @Column(name = "PAR_PAGATO")
    private BigDecimal paidAmount;

    @Column(name = "PAR_DTSCAD", length = 10)
    private String dueDate;

    /** Residuo aperto = totale - pagato (calcolato, non persistito). */
    @javax.persistence.Transient
    public BigDecimal getResidual() {
        BigDecimal t = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        BigDecimal p = paidAmount  != null ? paidAmount  : BigDecimal.ZERO;
        return t.subtract(p);
    }
}
