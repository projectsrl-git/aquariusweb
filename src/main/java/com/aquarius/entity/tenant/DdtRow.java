package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Riga DDT — read-only mapping of {@code U_BOL_DD}. Column names reuse the
 * {@code ORD_} prefix (legacy convention on all document tables). Row→header
 * hook: {@code DAGGANCIO = TT.TAGGANCIO}. The delivered quantity is
 * {@code ORD_QTAORD} (summed by APPLILIB when computing order fulfilment).
 */
@Entity
@Table(name = "U_BOL_DD")
@DynamicUpdate
@Data
public class DdtRow {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "DAGGANCIO", length = 10, insertable = false, updatable = false)
    private String aggancio;

    @Column(name = "ORD_SEQUEN", insertable = false, updatable = false)
    private BigDecimal sequence;

    @Column(name = "ORD_CODART", length = 30, insertable = false, updatable = false)
    private String articleCode;

    @Column(name = "ORD_MAGA", length = 6, insertable = false, updatable = false)
    private String articleExtension;

    @Column(name = "ORD_DESART", length = 100, insertable = false, updatable = false)
    private String description;

    @Column(name = "ORD_DES2", length = 100, insertable = false, updatable = false)
    private String description2;

    /** Delivered quantity. */
    @Column(name = "ORD_QTAORD", insertable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "ORD_PREZZO", insertable = false, updatable = false)
    private BigDecimal price;

    @Column(name = "ORD_PRZNET", insertable = false, updatable = false)
    private BigDecimal netPrice;

    @Column(name = "ORD_VALORE", insertable = false, updatable = false)
    private BigDecimal rowValue;

    @Column(name = "ORD_IVA", length = 3, insertable = false, updatable = false)
    private String vatCode;

    // ─── Linked documents on the row ─────────────────────────────────────────
    /** Linked customer order (denormalized). */
    @Column(name = "ORS_NUMORD", length = 6, insertable = false, updatable = false)
    private String linkedOrderNumber;

    @Column(name = "ORS_DATORD", length = 10, insertable = false, updatable = false)
    private String linkedOrderDate;

    /** Linked invoice (populated once the row is invoiced). */
    @Column(name = "MOV_NUMFAT", length = 6, insertable = false, updatable = false)
    private String linkedInvoiceNumber;

    @Column(name = "MOV_DATFAT", length = 10, insertable = false, updatable = false)
    private String linkedInvoiceDate;

    /** True when the row is a comment line (legacy convention on ORD_DESART). */
    public boolean isComment() {
        return description != null && description.trim().startsWith("*** COMMENTO ***");
    }
}
