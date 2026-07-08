package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Distinta base (testata) — read-only mapping of {@code U_DIS_TT}.
 * A BOM belongs to an ARTICLE: {@code DIT_GRUPPO} is the parent article
 * code (legacy joins {@code U_DIS_TT.DIT_GRUPPO = U_ART_PR.ART_CODPRI},
 * see DISTINTA_BASE_LIB.PRG). Header↔rows link: TAGGANCIO = DAGGANCIO.
 */
@Entity
@Table(name = "U_DIS_TT")
@DynamicUpdate
@Data
public class BomHead {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "TAGGANCIO", length = 10, insertable = false, updatable = false)
    private String aggancio;

    @Column(name = "DIT_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    /** Parent article code (the product this BOM builds). */
    @Column(name = "DIT_GRUPPO", length = 30, insertable = false, updatable = false)
    private String parentArticleCode;

    @Column(name = "DIT_DESCRI", length = 254, insertable = false, updatable = false)
    private String description;

    @Column(name = "DIT_UM", length = 2, insertable = false, updatable = false)
    private String unit;

    @Column(name = "DIT_COSTO", insertable = false, updatable = false)
    private BigDecimal cost;

    @Column(name = "DIT_PREZZO", insertable = false, updatable = false)
    private BigDecimal price;

    @Column(name = "DIT_VALUTA", length = 3, insertable = false, updatable = false)
    private String currency;

    /** Optional customer the BOM variant belongs to. */
    @Column(name = "DIT_CODCLI", length = 10, insertable = false, updatable = false)
    private String customerCode;

    @Column(name = "DIT_RAGSOC", length = 40, insertable = false, updatable = false)
    private String customerName;
}
