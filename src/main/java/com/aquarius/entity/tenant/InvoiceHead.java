package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Fattura di vendita (testata) — read-only mapping of {@code U_FAT_TT}.
 * Web counterpart of the VFP form {@code MENU_FAT000} ("Fatture di vendita").
 * NOTE (legacy convention): on the invoice tables {@code ORD_NUMORD} /
 * {@code ORD_DATORD} are the INVOICE's own number/date (legacy lookups match
 * {@code ord_numord = ALLTRIM(Z_NUMFAT)}). Header↔rows link:
 * {@code TT.TAGGANCIO = DD.DAGGANCIO}.
 */
@Entity
@Table(name = "U_FAT_TT")
@DynamicUpdate
@Data
public class InvoiceHead {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "TAGGANCIO", length = 10, insertable = false, updatable = false)
    private String aggancio;

    @Column(name = "ORD_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    @Column(name = "ORD_ANNO", length = 4, insertable = false, updatable = false)
    private String fiscalYear;

    /** Invoice number (legacy reuses the ORD_NUMORD column name). */
    @Column(name = "ORD_NUMORD", length = 6, insertable = false, updatable = false)
    private String invoiceNumber;

    /** Invoice date (varchar {@code yyyy/MM/dd}). */
    @Column(name = "ORD_DATORD", length = 10, insertable = false, updatable = false)
    private String invoiceDate;

    @Column(name = "ORD_CODCLI", length = 10, insertable = false, updatable = false)
    private String customerCode;

    @Column(name = "ORD_RAGSOC", length = 150, insertable = false, updatable = false)
    private String customerName;

    @Column(name = "ORD_PARIVA", length = 20, insertable = false, updatable = false)
    private String vatNumber;

    /** Document type code (badge). */
    @Column(name = "ORD_TIPORD", length = 3, insertable = false, updatable = false)
    private String documentType;

    @Column(name = "ORD_CAUS", length = 10, insertable = false, updatable = false)
    private String causale;

    @Column(name = "ORD_RIFERI", length = 60, insertable = false, updatable = false)
    private String reference;

    /** Payment condition code. */
    @Column(name = "ORD_CPA", length = 3, insertable = false, updatable = false)
    private String paymentCode;

    @Column(name = "ORD_AGE", length = 3, insertable = false, updatable = false)
    private String agentCode;

    @Column(name = "ORD_VALUTA", length = 3, insertable = false, updatable = false)
    private String currency;

    @Column(name = "ORD_IMPONIB", insertable = false, updatable = false)
    private BigDecimal taxableAmount;

    @Column(name = "ORD_IMPOSTA", insertable = false, updatable = false)
    private BigDecimal taxAmount;

    /** Net-to-pay stored by the legacy. */
    @Column(name = "t_netto", insertable = false, updatable = false)
    private BigDecimal netAmount;

    /** Transmitted flag (fatturazione elettronica). */
    @Column(name = "ORD_TRASME", insertable = false, updatable = false)
    private Boolean transmitted;

    /** SDI identifier (fatturazione elettronica). */
    @Column(name = "ORD_IDSDI", length = 10, insertable = false, updatable = false)
    private String sdiId;

    /** Document total = taxable + tax (null-safe; computed here, never in templates). */
    public BigDecimal getTotal() {
        BigDecimal imp = taxableAmount == null ? BigDecimal.ZERO : taxableAmount;
        BigDecimal tax = taxAmount == null ? BigDecimal.ZERO : taxAmount;
        return imp.add(tax);
    }

    public boolean isTransmitted() {
        return Boolean.TRUE.equals(transmitted);
    }
}
