package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Movimento contabile — riga di partita doppia della tabella {@code MOV_CONT}
 * (legacy, READ-ONLY dal web). Ogni scrittura contabile è un gruppo di righe
 * che condividono {@code MOV_SOC + MOV_ANNO + MOV_NREGIS}; ciascuna riga è in
 * Dare ({@code MOV_TMOV = 'D'}) o Avere ({@code MOV_TMOV = 'A'}).
 *
 * <p>Usata in sola lettura da Primanota, Storico contabile e Bilancio. La
 * scrittura (data-entry primanota) sarà una slice separata: qui NON si mappa
 * nulla come modificabile.</p>
 *
 * <p>Le date ({@code MOV_DTREG}, {@code MOV_DTDOC}) sono varchar nel legacy;
 * si trattano come stringhe e si filtra per intervallo come stringhe.</p>
 */
@Entity
@Table(name = "MOV_CONT")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovContabile {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "MOV_SOC", length = 2)
    private String societyCode;

    @Column(name = "MOV_ANNO", length = 4)
    private String fiscalYear;

    @Column(name = "MOV_NREGIS", length = 10)
    private String registrationNo;

    @Column(name = "MOV_DTREG", length = 10)
    private String registrationDate;

    @Column(name = "MOV_DTDOC", length = 10)
    private String documentDate;

    @Column(name = "MOV_NDOC", length = 20)
    private String documentNo;

    @Column(name = "MOV_CONTO", length = 13)
    private String account;

    @Column(name = "MOV_TMOV", length = 1)
    private String movementType;   // 'D' = Dare, 'A' = Avere

    @Column(name = "MOV_IMP")
    private BigDecimal amount;

    @Column(name = "MOV_TOP", length = 10)
    private String operationType;

    @Column(name = "MOV_DESMOV", length = 254)
    private String description;

    @Column(name = "MOV_DESCR", length = 40)
    private String shortDescription;

    @Column(name = "MOV_CCLI", length = 10)
    private String customerCode;

    @Column(name = "MOV_CFOR", length = 10)
    private String supplierCode;

    @Column(name = "MOV_NPART")
    private BigDecimal partitaNo;

    @Column(name = "MOV_IVA", length = 3)
    private String vatCode;

    @Column(name = "MOV_IMPOST")
    private BigDecimal vatAmount;
}
