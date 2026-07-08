package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Agente — read-only mapping (minimal) of {@code U_AGE_AN}.
 * Web counterpart of MENU_AGE000. The AGE code is also referenced by
 * documents (ORD_AGE) and decoded elsewhere via PARA 'AGE'.
 */
@Entity
@Table(name = "U_AGE_AN")
@DynamicUpdate
@Data
public class Agent {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "AGE_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    @Column(name = "AGE_CODICE", length = 3, insertable = false, updatable = false)
    private String code;

    @Column(name = "AGE_RAGSOC", length = 30, insertable = false, updatable = false)
    private String name;

    @Column(name = "AGE_INDIRI", length = 30, insertable = false, updatable = false)
    private String address;

    @Column(name = "AGE_CAP", length = 5, insertable = false, updatable = false)
    private String zip;

    @Column(name = "AGE_LOCALI", length = 30, insertable = false, updatable = false)
    private String city;

    @Column(name = "AGE_PROVIN", length = 2, insertable = false, updatable = false)
    private String province;

    @Column(name = "AGE_CF", length = 16, insertable = false, updatable = false)
    private String fiscalCode;

    @Column(name = "AGE_PI", length = 11, insertable = false, updatable = false)
    private String vatNumber;

    /** Percentuale provvigione base. */
    @Column(name = "AGE_PROVVI", insertable = false, updatable = false)
    private BigDecimal commissionPct;

    @Column(name = "AGE_VALUTA", length = 3, insertable = false, updatable = false)
    private String currency;

    @Column(name = "AGE_DATINS", length = 10, insertable = false, updatable = false)
    private String insertDate;
}
