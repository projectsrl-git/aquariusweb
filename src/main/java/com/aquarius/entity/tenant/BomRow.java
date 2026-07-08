package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Componente di distinta base (primo livello) — read-only mapping of
 * {@code U_DIS_DD}. {@code DIS_ESPLOD = 'X'} marks the component as an
 * explodable sub-BOM (its article has a BOM of its own — legacy convention
 * in DISTINTA_BASE_LIB.PRG). Multi-level explosion and costing are NOT
 * done here (reserved to a future slice).
 */
@Entity
@Table(name = "U_DIS_DD")
@DynamicUpdate
@Data
public class BomRow {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "DAGGANCIO", length = 10, insertable = false, updatable = false)
    private String aggancio;

    @Column(name = "DIS_SEQUEN", insertable = false, updatable = false)
    private BigDecimal sequence;

    /** Component article code. */
    @Column(name = "DIS_CODART", length = 30, insertable = false, updatable = false)
    private String articleCode;

    @Column(name = "DIS_MAGA", length = 6, insertable = false, updatable = false)
    private String articleExtension;

    @Column(name = "DIS_DESCRI", length = 254, insertable = false, updatable = false)
    private String description;

    @Column(name = "DIS_QTA", insertable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "DIS_UM", length = 2, insertable = false, updatable = false)
    private String unit;

    @Column(name = "DIS_COSTO", insertable = false, updatable = false)
    private BigDecimal cost;

    @Column(name = "DIS_PREZZO", insertable = false, updatable = false)
    private BigDecimal price;

    @Column(name = "DIS_TIPO", length = 1, insertable = false, updatable = false)
    private String type;

    /** 'X' = the component is itself an explodable sub-BOM. */
    @Column(name = "DIS_ESPLOD", length = 1, insertable = false, updatable = false)
    private String explodeFlag;

    @Column(name = "DIS_CODFOR", length = 10, insertable = false, updatable = false)
    private String supplierCode;

    @Column(name = "DIS_DESFOR", length = 80, insertable = false, updatable = false)
    private String supplierName;

    public boolean isSubBom() {
        return explodeFlag != null && "X".equalsIgnoreCase(explodeFlag.trim());
    }
}
