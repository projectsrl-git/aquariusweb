package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Quota di ammortamento per anno — read-only mapping di {@code U_QUO_AM}
 * (generata da MENU_QUO_AM_GEN; QUO_FLGCGE = quota trasferita in
 * contabilita' generale). Archivio senza dimensione societa'.
 */
@Entity
@Table(name = "U_QUO_AM")
@DynamicUpdate
@Data
public class DepreciationQuota {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "QUO_CODCES", length = 16, insertable = false, updatable = false)
    private String assetCode;

    @Column(name = "QUO_DESCES", length = 50, insertable = false, updatable = false)
    private String assetDescription;

    @Column(name = "QUO_CODCAT", length = 10, insertable = false, updatable = false)
    private String categoryCode;

    /** Anno di riferimento della quota. */
    @Column(name = "QUO_ANNRIF", length = 4, insertable = false, updatable = false)
    private String year;

    @Column(name = "QUO_VALSTO", insertable = false, updatable = false)
    private BigDecimal historicalValue;

    // ordinario
    @Column(name = "QUO_PERORD", insertable = false, updatable = false)
    private BigDecimal ordinaryPct;

    @Column(name = "QUO_VALORD", insertable = false, updatable = false)
    private BigDecimal ordinaryAmount;

    @Column(name = "QUO_FONORD", insertable = false, updatable = false)
    private BigDecimal ordinaryFund;

    // anticipato
    @Column(name = "QUO_PERANT", insertable = false, updatable = false)
    private BigDecimal anticipatedPct;

    @Column(name = "QUO_VALANT", insertable = false, updatable = false)
    private BigDecimal anticipatedAmount;

    @Column(name = "QUO_FONANT", insertable = false, updatable = false)
    private BigDecimal anticipatedFund;

    // accelerato
    @Column(name = "QUO_PERACC", insertable = false, updatable = false)
    private BigDecimal acceleratedPct;

    @Column(name = "QUO_VALACC", insertable = false, updatable = false)
    private BigDecimal acceleratedAmount;

    @Column(name = "QUO_FONACC", insertable = false, updatable = false)
    private BigDecimal acceleratedFund;

    @Column(name = "QUO_TOTAMM", insertable = false, updatable = false)
    private BigDecimal totalDepreciated;

    @Column(name = "QUO_VALRES", insertable = false, updatable = false)
    private BigDecimal residualValue;

    /** Quota trasferita in contabilita' generale. */
    @Column(name = "QUO_FLGCGE", insertable = false, updatable = false)
    private Boolean transferredToGl;
}
