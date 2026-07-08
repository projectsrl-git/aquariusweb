package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Ordine cliente collegato a un programma di produzione — read-only mapping
 * of {@code PROD_ORDINI} (link by {@code IDPRG}). {@code GRUPRD} is the
 * production group, decoded via {@code PARA 'PRD'+code → DESCRI}
 * (legacy: seek_para('PRD', ...)).
 */
@Entity
@Table(name = "PROD_ORDINI")
@DynamicUpdate
@Data
public class ProductionOrderRef {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "IDPRG", length = 10, insertable = false, updatable = false)
    private String programId;

    @Column(name = "NUMORD", length = 6, insertable = false, updatable = false)
    private String orderNumber;

    @Column(name = "DATORD", length = 10, insertable = false, updatable = false)
    private String orderDate;

    @Column(name = "CODCLI", length = 10, insertable = false, updatable = false)
    private String customerCode;

    @Column(name = "RAGSOC", length = 100, insertable = false, updatable = false)
    private String customerName;

    @Column(name = "CODART", length = 30, insertable = false, updatable = false)
    private String articleCode;

    @Column(name = "DESART", length = 200, insertable = false, updatable = false)
    private String articleDescription;

    @Column(name = "QTAORD", insertable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "UM", length = 6, insertable = false, updatable = false)
    private String unit;

    /** Requested delivery (varchar yyyy/MM/dd). */
    @Column(name = "DATCON", length = 10, insertable = false, updatable = false)
    private String deliveryDate;

    /** Production group (PARA 'PRD'+code → DESCRI). */
    @Column(name = "GRUPRD", length = 30, insertable = false, updatable = false)
    private String productionGroup;
}
