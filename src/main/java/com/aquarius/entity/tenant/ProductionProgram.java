package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Programma di produzione — read-only mapping of {@code PRODUZIONE}.
 * Web counterpart of the VFP form {@code STD_PROGRAMMAZIONE}
 * ("Programmazione della produzione", menu gestioneproduzionestandard).
 * The STANDARD discriminator is VERIFIED in the form's search:
 * {@code PARENT = ''} (root program nodes) {@code AND TIPO = 'STD'}.
 * PRODUZIONE is a tree (IDNODE/PARENT); PROD_ORDINI / PROD_LEGAMI /
 * PROD_AVANZA link to it by {@code IDPRG}. No society/year columns exist
 * on the PROD_* tables.
 */
@Entity
@Table(name = "PRODUZIONE")
@DynamicUpdate
@Data
public class ProductionProgram {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "IDPRG", length = 10, insertable = false, updatable = false)
    private String programId;

    @Column(name = "IDNODE", length = 10, insertable = false, updatable = false)
    private String nodeId;

    @Column(name = "PARENT", length = 10, insertable = false, updatable = false)
    private String parent;

    /** 'STD' = produzione standard (legacy filter, verified). */
    @Column(name = "TIPO", length = 3, insertable = false, updatable = false)
    private String type;

    @Column(name = "NUMPRG", length = 6, insertable = false, updatable = false)
    private String programNumber;

    /** Program date (varchar yyyy/MM/dd). */
    @Column(name = "DATPRG", length = 10, insertable = false, updatable = false)
    private String programDate;

    /** Product article code of the node. */
    @Column(name = "COMP", length = 30, insertable = false, updatable = false)
    private String articleCode;

    @Column(name = "DESCOMP", length = 200, insertable = false, updatable = false)
    private String articleDescription;

    @Column(name = "QTA", insertable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "UM", length = 2, insertable = false, updatable = false)
    private String unit;

    @Column(name = "FASELA", length = 8, insertable = false, updatable = false)
    private String phase;

    @Column(name = "DESFASELA", length = 200, insertable = false, updatable = false)
    private String phaseDescription;

    /** Planned start/end (varchar yyyy/MM/dd). */
    @Column(name = "DTINPR", length = 10, insertable = false, updatable = false)
    private String plannedStart;

    @Column(name = "DTFIPR", length = 10, insertable = false, updatable = false)
    private String plannedEnd;

    /** Actual start/end (varchar yyyy/MM/dd). */
    @Column(name = "DTINEF", length = 10, insertable = false, updatable = false)
    private String actualStart;

    @Column(name = "DTFIEF", length = 10, insertable = false, updatable = false)
    private String actualEnd;

    @Column(name = "TEMPO", insertable = false, updatable = false)
    private BigDecimal time;

    @Column(name = "COMPLETE", insertable = false, updatable = false)
    private Boolean complete;

    @Column(name = "FLGCLOSED", insertable = false, updatable = false)
    private Boolean closed;

    public String getStatusLabel() {
        if (Boolean.TRUE.equals(closed)) return "Chiuso";
        if (Boolean.TRUE.equals(complete)) return "Completato";
        if (actualStart != null && !actualStart.trim().isEmpty()) return "In corso";
        return "Pianificato";
    }
}
