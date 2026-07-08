package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Componente/legame di un programma di produzione — read-only mapping of
 * {@code PROD_LEGAMI} (link by {@code IDPRG}).
 */
@Entity
@Table(name = "PROD_LEGAMI")
@DynamicUpdate
@Data
public class ProductionComponent {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "IDPRG", length = 10, insertable = false, updatable = false)
    private String programId;

    @Column(name = "SEQUEN", insertable = false, updatable = false)
    private BigDecimal sequence;

    @Column(name = "CODART", length = 30, insertable = false, updatable = false)
    private String articleCode;

    @Column(name = "DESART", length = 200, insertable = false, updatable = false)
    private String articleDescription;

    @Column(name = "QTA", insertable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "UM", length = 6, insertable = false, updatable = false)
    private String unit;

    /** True when the row is the finished product ("prodotto finito"). */
    @Column(name = "PRDFIN", insertable = false, updatable = false)
    private Boolean finishedProduct;
}
