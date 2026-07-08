package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Riga ordine cliente — read-only mapping of {@code U_ORD_DD} (legacy).
 * Row value in the legacy is {@code ORD_VALORE = ORD_PRZNET × ORD_QTAORD}
 * (see APPLILIB {@code CALCOLA_VALORE_RIGA_DOCUMENTO}). The header link
 * columns carry the {@code ORS_} prefix (denormalized from U_ORD_TT).
 */
@Entity
@Table(name = "U_ORD_DD")
@DynamicUpdate
@Data
public class OrderRow {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    /** Row→header hook key ({@code DAGGANCIO = TT.TAGGANCIO}). */
    @Column(name = "DAGGANCIO", length = 10, insertable = false, updatable = false)
    private String aggancio;

    // ─── Link to header (ORS_* = denormalized U_ORD_TT keys, legacy fallback) ─
    @Column(name = "ORS_NUMORD", length = 6, insertable = false, updatable = false)
    private String orderNumber;

    @Column(name = "ORS_DATORD", length = 10, insertable = false, updatable = false)
    private String orderDate;

    @Column(name = "ORS_CODCLI", length = 10, insertable = false, updatable = false)
    private String customerCode;

    // ─── Row data ────────────────────────────────────────────────────────────
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

    /** Ordered quantity (the quantity used by the legacy row-value formula). */
    @Column(name = "ORD_QTAORD", insertable = false, updatable = false)
    private BigDecimal quantity;

    /** Gross unit price. */
    @Column(name = "ORD_PREZZO", insertable = false, updatable = false)
    private BigDecimal price;

    /** Net unit price (after discounts). */
    @Column(name = "ORD_PRZNET", insertable = false, updatable = false)
    private BigDecimal netPrice;

    /** Row value = netPrice × quantity (stored by the legacy). */
    @Column(name = "ORD_VALORE", insertable = false, updatable = false)
    private BigDecimal rowValue;

    @Column(name = "ORD_IVA", length = 3, insertable = false, updatable = false)
    private String vatCode;

    /** Row fulfilled flag ("evasione" riga). */
    @Column(name = "ORD_EVASEL", insertable = false, updatable = false)
    private Boolean fulfilled;

    /** True when the row is a comment line (legacy convention on ORD_DESART). */
    public boolean isComment() {
        return description != null && description.trim().startsWith("*** COMMENTO ***");
    }
}
