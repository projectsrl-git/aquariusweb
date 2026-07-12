package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Cespite — read-only mapping di {@code u_amm_at} (anagrafica cespiti,
 * MENU_AMMCES / consultazione MENU_stat_cesp "Visualizzazione / stampa
 * cespiti"). NOTA VERIFICATA NEI SORGENTI: l'archivio cespiti NON ha la
 * dimensione societa' (nessuna colonna soc; i form legacy filtrano solo
 * i CONTI per PUB_CODSOC): archivio unico per installazione.
 * Ordine legacy: amm_codcat, amm_codces.
 */
@Entity
@Table(name = "u_amm_at")
@DynamicUpdate
@Data
public class FixedAsset {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "AMM_CODCES", length = 16, insertable = false, updatable = false)
    private String code;

    @Column(name = "AMM_DESCRI", length = 50, insertable = false, updatable = false)
    private String description;

    @Column(name = "AMM_CODCAT", length = 10, insertable = false, updatable = false)
    private String categoryCode;

    @Column(name = "AMM_TIPCAT", length = 2, insertable = false, updatable = false)
    private String categoryType;

    /** Flag "in uso" (usato/nuovo determina la % del primo anno). */
    @Column(name = "AMM_FLGUSO", length = 1, insertable = false, updatable = false)
    private String usedFlag;

    @Column(name = "AMM_DATUTI", length = 10, insertable = false, updatable = false)
    private String inServiceDate;

    /** Numero quote/anni gia' ammortizzati. */
    @Column(name = "AMM_QTEAMM", insertable = false, updatable = false)
    private BigDecimal quotasCount;

    /** Percentuale di ammortamento base. */
    @Column(name = "AMM_PERBAS", insertable = false, updatable = false)
    private BigDecimal basePct;

    @Column(name = "AMM_MATRIC", length = 50, insertable = false, updatable = false)
    private String serialNumber;

    @Lob
    @Column(name = "AMM_NOTE", insertable = false, updatable = false)
    private String notes;

    /** Conto fondo ammortamento. */
    @Column(name = "AMM_CONFON", length = 13, insertable = false, updatable = false)
    private String fundAccount;

    /** Conto cespite. */
    @Column(name = "AMM_CONCES", length = 13, insertable = false, updatable = false)
    private String assetAccount;

    /** Valore storico (costo di acquisto). */
    @Column(name = "AMM_VALSTO", insertable = false, updatable = false)
    private BigDecimal historicalValue;

    /** Valore residuo da ammortizzare. */
    @Column(name = "AMM_VALRES", insertable = false, updatable = false)
    private BigDecimal residualValue;

    /** Totale ammortizzato. */
    @Column(name = "AMM_TOTAMM", insertable = false, updatable = false)
    private BigDecimal totalDepreciated;

    // ─── cessione ───
    @Column(name = "AMM_TIPCES", insertable = false, updatable = false)
    private BigDecimal disposalType;

    @Column(name = "AMM_DATCES", length = 10, insertable = false, updatable = false)
    private String disposalDate;

    @Column(name = "AMM_VALCES", insertable = false, updatable = false)
    private BigDecimal disposalValue;

    /** Plus/minusvalenza da cessione. */
    @Column(name = "AMM_PLUMIN", insertable = false, updatable = false)
    private BigDecimal capitalGainLoss;

    @Column(name = "AMM_DATREG", length = 10, insertable = false, updatable = false)
    private String registrationDate;

    public boolean isDisposed() {
        return disposalDate != null && !disposalDate.trim().isEmpty();
    }
}
