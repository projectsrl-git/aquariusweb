package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Movimento di magazzino — read-only mapping of {@code U_MAG_MO}.
 * Web counterpart of the VFP form {@code menu_movimenti_mag}
 * ("Movimenti magazzino"). The movement causale is {@code MOV_TOP},
 * decoded via {@code PARA 'TOP'+MOV_TOP → DESCRI} (same TOP rule as
 * primanota and the documents dashboard — verified in the form's query).
 * Legacy dates are varchar {@code yyyy/MM/dd}; compared as strings only
 * (MOV_DTREGI contains dirty values on old records — never convert the
 * column, see CLAUDE.md).
 */
@Entity
@Table(name = "U_MAG_MO")
@DynamicUpdate
@Data
public class WarehouseMovement {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "MOV_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    @Column(name = "MOV_ANNO", length = 4, insertable = false, updatable = false)
    private String fiscalYear;

    /** Registration date (varchar yyyy/MM/dd — may be dirty on old records). */
    @Column(name = "MOV_DTREGI", length = 10, insertable = false, updatable = false)
    private String registrationDate;

    @Column(name = "MOV_ORAMOV", length = 8, insertable = false, updatable = false)
    private String movementTime;

    @Column(name = "MOV_DTDOCU", length = 10, insertable = false, updatable = false)
    private String documentDate;

    @Column(name = "MOV_NUMDOC", length = 20, insertable = false, updatable = false)
    private String documentNumber;

    @Column(name = "MOV_CODMAG", length = 6, insertable = false, updatable = false)
    private String warehouseCode;

    /** Article registry key (same key used by the FIFO valuation). */
    @Column(name = "MOV_ANAART", length = 30, insertable = false, updatable = false)
    private String articleCode;

    @Column(name = "MOV_DESART", length = 100, insertable = false, updatable = false)
    private String articleDescription;

    /** '+' carico / '-' scarico. */
    @Column(name = "MOV_SEGNO", length = 1, insertable = false, updatable = false)
    private String sign;

    /** Causale/tipo operazione (PARA 'TOP'+code → DESCRI). */
    @Column(name = "MOV_TOP", length = 10, insertable = false, updatable = false)
    private String top;

    @Column(name = "MOV_QTAMOV", insertable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "MOV_UM", length = 2, insertable = false, updatable = false)
    private String unit;

    @Column(name = "MOV_PREACQ", insertable = false, updatable = false)
    private BigDecimal purchasePrice;

    @Column(name = "MOV_VALACQ", insertable = false, updatable = false)
    private BigDecimal purchaseValue;

    @Column(name = "MOV_PREVEN", insertable = false, updatable = false)
    private BigDecimal salePrice;

    @Column(name = "MOV_VALVEN", insertable = false, updatable = false)
    private BigDecimal saleValue;

    @Column(name = "MOV_VALUTA", length = 3, insertable = false, updatable = false)
    private String currency;

    @Column(name = "MOV_FORNIT", length = 10, insertable = false, updatable = false)
    private String supplierCode;

    @Column(name = "MOV_DESFOR", length = 50, insertable = false, updatable = false)
    private String supplierName;

    @Column(name = "MOV_CODCLI", length = 10, insertable = false, updatable = false)
    private String customerCode;

    @Column(name = "MOV_DESCLI", length = 50, insertable = false, updatable = false)
    private String customerName;

    @Column(name = "MOV_NOMOPE", length = 50, insertable = false, updatable = false)
    private String operatorName;

    /** True for a '+' (carico) movement. */
    public boolean isInbound() {
        return sign != null && "+".equals(sign.trim());
    }
}
