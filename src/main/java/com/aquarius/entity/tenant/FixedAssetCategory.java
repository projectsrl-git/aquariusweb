package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Categoria cespiti — read-only mapping di {@code u_amm_ca}
 * (MENU_AMMCAT "Gestione categorie"). Archivio senza dimensione societa'.
 */
@Entity
@Table(name = "u_amm_ca")
@DynamicUpdate
@Data
public class FixedAssetCategory {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "AMM_CODAMM", length = 10, insertable = false, updatable = false)
    private String code;

    @Column(name = "AMM_DESCR1", length = 50, insertable = false, updatable = false)
    private String description1;

    @Column(name = "AMM_DESCR2", length = 50, insertable = false, updatable = false)
    private String description2;

    @Column(name = "AMM_DESCR3", length = 50, insertable = false, updatable = false)
    private String description3;

    /** Percentuale di ammortamento della categoria. */
    @Column(name = "AMM_PERAMM", insertable = false, updatable = false)
    private BigDecimal depreciationPct;

    @Column(name = "AMM_CONTAB", length = 13, insertable = false, updatable = false)
    private String accountingAccount;

    @Column(name = "AMM_CONPAR", length = 13, insertable = false, updatable = false)
    private String counterpartAccount;
}
