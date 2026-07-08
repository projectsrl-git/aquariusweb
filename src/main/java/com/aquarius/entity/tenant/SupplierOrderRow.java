package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Riga ordine a fornitore — read-only mapping of {@code U_ORF_DD}.
 * Mirror of {@link OrderRow} (U_ORD_DD). ORS_* columns are the denormalized
 * header keys (fallback join, as on the sales side).
 */
@Entity
@Table(name = "U_ORF_DD")
@DynamicUpdate
@Data
public class SupplierOrderRow {

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
    private BigDecimal orderedQuantity;

    /** Evasa (received) quantity — ORF uses ORD_QTAEV. */
    @Column(name = "ORD_QTAEV", insertable = false, updatable = false)
    private BigDecimal receivedQuantity;

    @Column(name = "ORD_PREZZO", insertable = false, updatable = false)
    private BigDecimal grossPrice;

    @Column(name = "ORD_PRZNET", insertable = false, updatable = false)
    private BigDecimal netPrice;

    @Column(name = "ORD_VALORE", insertable = false, updatable = false)
    private BigDecimal rowValue;

    @Column(name = "ORD_IVA", length = 3, insertable = false, updatable = false)
    private String vatCode;

    @Column(name = "ORD_DATCON", length = 10, insertable = false, updatable = false)
    private String deliveryDate;

    @Column(name = "ORD_CHIUSO", insertable = false, updatable = false)
    private Boolean closed;

    public boolean isCommento() {
        return description != null && description.trim().startsWith("*** COMMENTO ***");
    }
}
