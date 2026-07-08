package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * DDT / documento di trasporto (testata) — read-only mapping of {@code U_BOL_TT}.
 * Web counterpart of the VFP form {@code menu_BOL000} ("Documenti di trasporto").
 * NOTE: legacy column names on the DDT tables reuse the {@code ORD_} prefix.
 * Header↔rows link: {@code TT.TAGGANCIO = DD.DAGGANCIO} (legacy SQL joins).
 * Columns verified against {@code docs/db_schema/aquarius_schema_full.csv}.
 */
@Entity
@Table(name = "U_BOL_TT")
@DynamicUpdate
@Data
public class DdtHead {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    /** Header↔rows hook key. */
    @Column(name = "TAGGANCIO", length = 10, insertable = false, updatable = false)
    private String aggancio;

    @Column(name = "ORD_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    @Column(name = "ORD_ANNO", length = 4, insertable = false, updatable = false)
    private String fiscalYear;

    @Column(name = "ORD_NUMDDT", length = 6, insertable = false, updatable = false)
    private String ddtNumber;

    /** Legacy varchar date {@code yyyy/MM/dd}. */
    @Column(name = "ORD_DATDDT", length = 10, insertable = false, updatable = false)
    private String ddtDate;

    @Column(name = "ORD_CODCLI", length = 10, insertable = false, updatable = false)
    private String customerCode;

    @Column(name = "ORD_RAGSOC", length = 150, insertable = false, updatable = false)
    private String customerName;

    /** Causale trasporto/magazzino (code; badge in the UI). */
    @Column(name = "ORD_CAUS", length = 10, insertable = false, updatable = false)
    private String causale;

    @Column(name = "ORD_RIFERI", length = 60, insertable = false, updatable = false)
    private String reference;

    @Column(name = "ORD_DESTIN", length = 30, insertable = false, updatable = false)
    private String destination;

    /** Carrier ("vettore", first line). */
    @Column(name = "ORD_VETTO1", length = 40, insertable = false, updatable = false)
    private String carrier;

    @Column(name = "ORD_PORTO", length = 9, insertable = false, updatable = false)
    private String porto;

    @Column(name = "ORD_COLLI", length = 8, insertable = false, updatable = false)
    private String packages;

    @Column(name = "ORD_PESO", insertable = false, updatable = false)
    private BigDecimal grossWeight;

    @Column(name = "ORD_PESNET", insertable = false, updatable = false)
    private BigDecimal netWeight;

    @Column(name = "ORD_VALUTA", length = 3, insertable = false, updatable = false)
    private String currency;

    @Column(name = "ORD_IMPONIB", insertable = false, updatable = false)
    private BigDecimal taxableAmount;

    @Column(name = "ORD_IMPOSTA", insertable = false, updatable = false)
    private BigDecimal taxAmount;

    /** Document value stored by the legacy. */
    @Column(name = "ORD_VALORE", insertable = false, updatable = false)
    private BigDecimal documentValue;

    // ─── Linked documents ────────────────────────────────────────────────────
    /** Linked customer order (number/date). */
    @Column(name = "ORD_NUMORD", length = 6, insertable = false, updatable = false)
    private String linkedOrderNumber;

    @Column(name = "ORD_DATORD", length = 10, insertable = false, updatable = false)
    private String linkedOrderDate;

    /** Invoicing date — non-empty when the DDT has been invoiced. */
    @Column(name = "ORD_DATFAT", length = 10, insertable = false, updatable = false)
    private String invoicedDate;

    /** True when the DDT has been invoiced (business-language badge "Fatturato"). */
    public boolean isInvoiced() {
        return invoicedDate != null && !invoicedDate.trim().isEmpty();
    }
}
