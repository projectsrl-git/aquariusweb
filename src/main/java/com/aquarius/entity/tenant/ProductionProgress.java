package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Avanzamento di produzione — read-only mapping of {@code PROD_AVANZA}
 * (link by {@code IDPRG}; {@code FASELA} = working phase).
 * Consultation only: recording progress stays on the VFP client
 * (STD_AVANZA / AQ_AVANZA_STD).
 */
@Entity
@Table(name = "PROD_AVANZA")
@DynamicUpdate
@Data
public class ProductionProgress {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "IDPRG", length = 10, insertable = false, updatable = false)
    private String programId;

    @Column(name = "FASELA", length = 6, insertable = false, updatable = false)
    private String phase;

    @Column(name = "SEQUEN", insertable = false, updatable = false)
    private BigDecimal sequence;

    @Column(name = "COMP", length = 30, insertable = false, updatable = false)
    private String articleCode;

    @Column(name = "DESCOMP", length = 200, insertable = false, updatable = false)
    private String articleDescription;

    /** Planned quantity for the phase. */
    @Column(name = "QTA", insertable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "UM", length = 6, insertable = false, updatable = false)
    private String unit;

    /** Produced quantity recorded so far. */
    @Column(name = "QTAPROD", insertable = false, updatable = false)
    private BigDecimal producedQuantity;
}
