package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Riga documento di carico da fornitore — read-only mapping of
 * {@code U_BFO_DD}. Linked purchase order = ORS_NUMORC/ORS_DATORC (ORC
 * pair, as on the sales DDT rows); MOV_NUMFAT/MOV_DATFAT reference the
 * supplier invoice when recorded.
 */
@Entity
@Table(name = "U_BFO_DD")
@DynamicUpdate
@Data
public class SupplierDdtRow {

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

    @Column(name = "ORD_QTAORD", insertable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "ORD_UM", length = 2, insertable = false, updatable = false)
    private String unit;

    @Column(name = "ORD_PRZNET", insertable = false, updatable = false)
    private BigDecimal netPrice;

    @Column(name = "ORD_VALORE", insertable = false, updatable = false)
    private BigDecimal rowValue;

    @Column(name = "ORD_IVA", length = 3, insertable = false, updatable = false)
    private String vatCode;

    /** Linked purchase order (ORC pair). */
    @Column(name = "ORS_NUMORC", length = 6, insertable = false, updatable = false)
    private String linkedOrderNumber;

    @Column(name = "ORS_DATORC", length = 10, insertable = false, updatable = false)
    private String linkedOrderDate;

    /** Supplier invoice reference when recorded. */
    @Column(name = "MOV_NUMFAT", length = 20, insertable = false, updatable = false)
    private String linkedInvoiceNumber;

    @Column(name = "MOV_DATFAT", length = 10, insertable = false, updatable = false)
    private String linkedInvoiceDate;

    public boolean isCommento() {
        return description != null && description.trim().startsWith("*** COMMENTO ***");
    }
}
