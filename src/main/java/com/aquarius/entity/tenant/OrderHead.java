package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Ordine cliente (testata) — read-only mapping of {@code U_ORD_TT} (legacy).
 * Web counterpart of the VFP form {@code MENU_ORD000} ("Ordini clienti").
 * Pure consultation: every column {@code insertable=false, updatable=false}.
 * Legacy TT↔DD link: primary hook {@code TT.TAGGANCIO = DD.DAGGANCIO} (modern
 * legacy SQL); fallback triple key from ristampelib:
 * {@code ORS_DATORD = ORD_DATORD AND ORS_NUMORD = ORD_NUMORD AND ORS_CODCLI = ORD_CODCLI}.
 * Columns verified against {@code docs/db_schema/aquarius_schema_full.csv}.
 */
@Entity
@Table(name = "U_ORD_TT")
@DynamicUpdate
@Data
public class OrderHead {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    /** Header↔rows hook key: legacy SQL joins {@code TT.TAGGANCIO = DD.DAGGANCIO}. */
    @Column(name = "TAGGANCIO", length = 10, insertable = false, updatable = false)
    private String aggancio;

    @Column(name = "ORD_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    @Column(name = "ORD_ANNO", length = 4, insertable = false, updatable = false)
    private String fiscalYear;

    @Column(name = "ORD_NUMORD", length = 6, insertable = false, updatable = false)
    private String orderNumber;

    /** Legacy varchar date {@code yyyy/MM/dd} — filter/sort as string, render with itDate. */
    @Column(name = "ORD_DATORD", length = 10, insertable = false, updatable = false)
    private String orderDate;

    @Column(name = "ORD_CODCLI", length = 10, insertable = false, updatable = false)
    private String customerCode;

    @Column(name = "ORD_RAGSOC", length = 150, insertable = false, updatable = false)
    private String customerName;

    @Column(name = "ORD_RIFERI", length = 80, insertable = false, updatable = false)
    private String reference;

    /** Requested delivery date (varchar {@code yyyy/MM/dd}). */
    @Column(name = "ORD_DTCONS", length = 10, insertable = false, updatable = false)
    private String deliveryDate;

    @Column(name = "ORD_VALUTA", length = 3, insertable = false, updatable = false)
    private String currency;

    /** Sales agent code. */
    @Column(name = "ORD_AGE", length = 3, insertable = false, updatable = false)
    private String agentCode;

    @Column(name = "ORD_IMPONIB", insertable = false, updatable = false)
    private BigDecimal taxableAmount;

    @Column(name = "ORD_IMPOSTA", insertable = false, updatable = false)
    private BigDecimal taxAmount;

    @Column(name = "ORD_CHIUSO", insertable = false, updatable = false)
    private Boolean closed;

    /** Fully fulfilled ("evaso totale"). */
    @Column(name = "ORD_EVATOT", insertable = false, updatable = false)
    private Boolean fullyFulfilled;

    /** Partially fulfilled ("evaso parziale"). */
    @Column(name = "ORD_EVAPAR", insertable = false, updatable = false)
    private Boolean partiallyFulfilled;

    /** Document total = taxable + tax (null-safe, computed here — never in templates). */
    public BigDecimal getTotal() {
        BigDecimal imp = taxableAmount == null ? BigDecimal.ZERO : taxableAmount;
        BigDecimal tax = taxAmount == null ? BigDecimal.ZERO : taxAmount;
        return imp.add(tax);
    }

    /** Business-language status label for badges. */
    public String getStatusLabel() {
        if (Boolean.TRUE.equals(closed)) return "Chiuso";
        if (Boolean.TRUE.equals(fullyFulfilled)) return "Evaso";
        if (Boolean.TRUE.equals(partiallyFulfilled)) return "Evaso parz.";
        return "Aperto";
    }
}
