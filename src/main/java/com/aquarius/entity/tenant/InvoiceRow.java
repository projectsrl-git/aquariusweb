package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Riga fattura di vendita — read-only mapping of {@code U_FAT_DD}.
 * Row→header hook: {@code DAGGANCIO = TT.TAGGANCIO}.
 */
@Entity
@Table(name = "U_FAT_DD")
@DynamicUpdate
@Data
public class InvoiceRow {

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

    @Column(name = "ORD_DESART", length = 254, insertable = false, updatable = false)
    private String description;

    @Column(name = "ORD_DES2", length = 254, insertable = false, updatable = false)
    private String description2;

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

    /** True when the row is a comment line (legacy convention on ORD_DESART). */
    public boolean isComment() {
        return description != null && description.trim().startsWith("*** COMMENTO ***");
    }
}
