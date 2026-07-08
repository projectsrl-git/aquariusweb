package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Ordine a fornitore (testata) — read-only mapping of {@code U_ORF_TT}.
 * Mirror of {@link OrderHead} (U_ORD_TT): the purchase archives share the
 * ORD_* column convention. Web counterpart of MENU_ORF000 ("Aggiornamento
 * ordini di acquisto"). NOTE: the same legacy form also manages "proposte
 * ordini a fornitore" (launched with a flag); no discriminator column is
 * visible in the sources, so this module lists the whole archive
 * (NEEDS_DOMAIN in the tracker).
 * On supplier documents the counterpart lives in ORD_CODCLI/ORD_RAGSOC
 * (legacy column reuse, as in the Ristampa dashboard with party 'F').
 */
@Entity
@Table(name = "U_ORF_TT")
@DynamicUpdate
@Data
public class SupplierOrderHead {

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

    @Column(name = "ORD_NUMORD", length = 6, insertable = false, updatable = false)
    private String orderNumber;

    @Column(name = "ORD_DATORD", length = 10, insertable = false, updatable = false)
    private String orderDate;

    /** Supplier code (legacy reuses the ORD_CODCLI column on purchase docs). */
    @Column(name = "ORD_CODCLI", length = 10, insertable = false, updatable = false)
    private String supplierCode;

    @Column(name = "ORD_RAGSOC", length = 40, insertable = false, updatable = false)
    private String supplierName;

    @Column(name = "ORD_CAUS", length = 10, insertable = false, updatable = false)
    private String causale;

    @Column(name = "ORD_RIFERI", length = 30, insertable = false, updatable = false)
    private String reference;

    @Column(name = "ORD_IMPONIB", insertable = false, updatable = false)
    private BigDecimal taxableAmount;

    @Column(name = "ORD_IMPOSTA", insertable = false, updatable = false)
    private BigDecimal taxAmount;

    @Column(name = "ORD_VALUTA", length = 3, insertable = false, updatable = false)
    private String currency;

    /** Requested delivery (varchar, up to 20 chars on ORF). */
    @Column(name = "ORD_DTCONS", length = 20, insertable = false, updatable = false)
    private String deliveryDate;

    @Column(name = "ORD_CHIUSO", insertable = false, updatable = false)
    private Boolean closed;

    /** Linked customer order (back-to-back / conto lavoro), when present. */
    @Column(name = "ORD_NUMORC", length = 6, insertable = false, updatable = false)
    private String linkedCustomerOrderNumber;

    @Column(name = "ORD_DATORC", length = 10, insertable = false, updatable = false)
    private String linkedCustomerOrderDate;

    public BigDecimal getTotal() {
        BigDecimal a = taxableAmount == null ? BigDecimal.ZERO : taxableAmount;
        BigDecimal b = taxAmount == null ? BigDecimal.ZERO : taxAmount;
        return a.add(b);
    }

    public String getStatusLabel() {
        return Boolean.TRUE.equals(closed) ? "Chiuso" : "Aperto";
    }
}
