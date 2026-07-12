package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Movimento/documento collegato al cespite — read-only mapping di
 * {@code u_amm_ad} (dettagli alimentati da MENU_AGG_DETT_CESPITI a
 * partire dai movimenti contabili). AMD_NREGIS/AMD_ANNO permettono il
 * drill verso la registrazione di prima nota.
 */
@Entity
@Table(name = "u_amm_ad")
@DynamicUpdate
@Data
public class FixedAssetMovement {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "AMD_CODCES", length = 16, insertable = false, updatable = false)
    private String assetCode;

    @Column(name = "AMD_SEQUEN", insertable = false, updatable = false)
    private BigDecimal sequence;

    @Column(name = "AMD_TIPDOC", length = 1, insertable = false, updatable = false)
    private String documentType;

    @Column(name = "AMD_NUMDOC", length = 10, insertable = false, updatable = false)
    private String documentNumber;

    @Column(name = "AMD_DATDOC", length = 10, insertable = false, updatable = false)
    private String documentDate;

    @Column(name = "AMD_VALORE", insertable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "AMD_CONTO", length = 13, insertable = false, updatable = false)
    private String account;

    @Column(name = "AMD_TOP", length = 10, insertable = false, updatable = false)
    private String operationType;

    @Column(name = "AMD_DESTOP", length = 50, insertable = false, updatable = false)
    private String operationDescription;

    /** Numero registrazione di prima nota (drill). */
    @Column(name = "AMD_NREGIS", length = 10, insertable = false, updatable = false)
    private String registrationNumber;

    @Column(name = "AMD_ANNO", length = 4, insertable = false, updatable = false)
    private String year;

    @Column(name = "AMD_DTREG", length = 10, insertable = false, updatable = false)
    private String registrationDate;
}
