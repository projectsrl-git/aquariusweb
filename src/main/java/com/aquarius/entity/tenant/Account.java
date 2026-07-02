package com.aquarius.entity.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * Entity per la tabella legacy {@code CONTI} (Piano dei conti).
 *
 * <p>Replica web del form VFP {@code MENU_PDC000.scx}. La tabella ha 111 colonne
 * totali, ma il form ne usa una trentina (campi anagrafica + flag IVA/CEE/RAP
 * + centri di costo + ammortamenti). Le 50+ colonne dei saldi mensili
 * ({@code CON_DA01..12}, {@code CON_AV01..12}, {@code PRE_DA01..12}, ecc.) sono
 * calcolate dal VFP e NON le mappiamo (regola 1.3 plug&play:
 * {@code @DynamicUpdate} garantisce che Hibernate non le tocchi).</p>
 *
 * <h3>Gerarchia ad albero</h3>
 * <p>Il piano dei conti è gerarchico tramite:</p>
 * <ul>
 *   <li>{@code CON_PADRE} (bit): 1 = il record è un nodo intermedio (mastro/sottomastro),
 *       0 = foglia (conto effettivo movimentabile)</li>
 *   <li>{@code CON_CODPADRE} (varchar 20): codice del nodo padre — null/vuoto
 *       per i nodi root</li>
 * </ul>
 * <p>Esempio classico:</p>
 * <pre>
 *   "1"           (PADRE=1, CODPADRE='') Stato Patrimoniale
 *   "1.01"        (PADRE=1, CODPADRE='1') Attivo
 *   "1.01.001"    (PADRE=0, CODPADRE='1.01') Cassa
 * </pre>
 */
@Entity
@Table(name = "CONTI")
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    // ── Identificazione (PK business) ───────────────────────────────────
    /** Codice società (es. "00" per default). */
    @Column(name = "CON_SOC", length = 2)
    private String societyCode;

    /** Anno esercizio (es. "2026"). */
    @Column(name = "CON_ANNO", length = 4)
    private String fiscalYear;

    /** Codice conto. Es. "01.01.001". */
    @Column(name = "CON_CONTO", length = 13)
    private String code;

    @Column(name = "CON_DESCR", length = 80)
    private String description;

    // ── Gerarchia tree-view ─────────────────────────────────────────────
    /** True se è un mastro/sottomastro (nodo intermedio), false se foglia. */
    @Column(name = "CON_PADRE")
    private Boolean isParent;

    /** Codice del nodo padre nella gerarchia. Null/vuoto = root. */
    @Column(name = "CON_CODPADRE", length = 20)
    private String parentCode;

    // ── Classificazione ─────────────────────────────────────────────────
    /**
     * Tipo conto (radio button form):
     * C=Clienti, F=Fornitori, I=Iva, O=Conti d'ordine,
     * P=Corrispettivi, A=Altro.
     */
    @Column(name = "CON_TIPOCO", length = 1)
    private String accountType;

    /**
     * Tipo conto secondario (clienti/fornitori, ecc. — distinto da CON_TIPOCO).
     */
    @Column(name = "CON_TIPO_CONTO", length = 1)
    private String accountSubType;

    /** Posizione di bilancio: P=Patrimoniale, E=Economico. */
    @Column(name = "CON_POSBIL", length = 1)
    private String balancePosition;

    /** Conto IVA: 1 = sì, 0 = no. */
    @Column(name = "CON_IVASN")
    private Boolean hasVat;

    /** Partita IVA (per conti clienti/fornitori). */
    @Column(name = "CON_PARIVA", length = 11)
    private String vatNumber;

    /** Valuta del conto (es. "EUR"). */
    @Column(name = "CON_VALUTA", length = 3)
    private String currency;

    /** Flag abilitato. */
    @Column(name = "CON_ABILIT", length = 1)
    private String enabled;

    // ── Centri di costo ─────────────────────────────────────────────────
    /** Gestione centri di costo attiva. */
    @Column(name = "CON_FLGCCO")
    private Boolean costCenterEnabled;

    @Column(name = "CON_CCO",  length = 13)  private String costCenter0;
    @Column(name = "CON_CCO1", length = 13)  private String costCenter1;
    @Column(name = "CON_CCO2", length = 13)  private String costCenter2;
    @Column(name = "CON_CCO3", length = 13)  private String costCenter3;
    @Column(name = "CON_CCO4", length = 13)  private String costCenter4;

    @Column(name = "CON_PERCO1")  private BigDecimal costCenterPercent1;
    @Column(name = "CON_PERCO2")  private BigDecimal costCenterPercent2;
    @Column(name = "CON_PERCO3")  private BigDecimal costCenterPercent3;
    @Column(name = "CON_PERCO4")  private BigDecimal costCenterPercent4;
    @Column(name = "CON_PERCO5")  private BigDecimal costCenterPercent5;

    // ── Bilancio CEE / Raggruppamenti ───────────────────────────────────
    /** Codice riga dare CEE (raggruppamento bilancio). */
    @Column(name = "CON_CODRAG", length = 8)
    private String ceeDareCode;

    @Column(name = "CON_DESRAG", length = 50)
    private String ceeDareDescription;

    /** Codice riga avere CEE. */
    @Column(name = "CON_CODCON", length = 8)
    private String ceeAvereCode;

    @Column(name = "CON_DESCON", length = 50)
    private String ceeAvereDescription;

    /** Raggruppamento bilancio Sole 24 Ore — dare. */
    @Column(name = "CON_CODRA2", length = 8)  private String sole24DareCode;
    @Column(name = "CON_DESRA2", length = 50) private String sole24DareDescription;
    @Column(name = "CON_CODCO2", length = 8)  private String sole24AvereCode;
    @Column(name = "CON_DESCO2", length = 50) private String sole24AvereDescription;

    /** Codice raggruppamento Aquarius (CLASS9). */
    @Column(name = "CON_CLASS9", length = 13)
    private String aquariusGroup;

    // ── R.A.P. / I.N.P.S. (checkbox del form) ───────────────────────────
    @Column(name = "CON_ANTICIPAZIONI")  private Boolean isAdvanceAccount;
    @Column(name = "CON_PREVIDENZA")     private Boolean isWelfareAccount;
    @Column(name = "CON_BOLLI")          private Boolean isStampDutyAccount;
    @Column(name = "CON_SOGGETTORAP")    private Boolean isRapSubject;
    @Column(name = "CON_RAP")            private Boolean isRapAccount;
    @Column(name = "CON_SOGGETTOINPS")   private Boolean isInpsSubject;
    @Column(name = "CON_INPS")           private Boolean isInpsAccount;

    // ── Ammortamenti / Cespiti ──────────────────────────────────────────
    @Column(name = "CON_CODAMM", length = 10) private String amortizationCategory;
    @Column(name = "CON_CODCES", length = 16) private String assetRegistryCode;
    @Column(name = "CON_CATEG",  length = 6)  private String generalCategory;

    // ── Altri flag form ─────────────────────────────────────────────────
    /** Controlla presenza numero commessa in prima nota. */
    @Column(name = "con_nocomme")
    private Boolean noOrderNumberCheck;

    @Column(name = "CON_NUMPAR", length = 13)
    private String partitionNumber;

    // ── Saldi (read-only, valori calcolati dal VFP) ─────────────────────
    /** Saldo iniziale (apertura esercizio). */
    @Column(name = "CON_SALDOI", insertable = false, updatable = false)
    private BigDecimal initialBalance;

    /** Totale dare progressivo. */
    @Column(name = "CON_IMP_D", insertable = false, updatable = false)
    private BigDecimal totalDebit;

    /** Totale avere progressivo. */
    @Column(name = "CON_IMP_A", insertable = false, updatable = false)
    private BigDecimal totalCredit;

    // ── Helpers ─────────────────────────────────────────────────────────

    /** True se è un nodo intermedio (mastro), false se foglia movimentabile. */
    public boolean isHierarchyNode() {
        return Boolean.TRUE.equals(isParent);
    }

    /** True se è un nodo root della gerarchia. */
    public boolean isRoot() {
        return parentCode == null || parentCode.trim().isEmpty();
    }

    /**
     * Livello gerarchico stimato dal numero di "." nel codice.
     * Es. "1" → 0, "1.01" → 1, "1.01.001" → 2.
     */
    public int depth() {
        if (code == null) return 0;
        int dots = 0;
        for (char c : code.toCharArray()) if (c == '.') dots++;
        return dots;
    }
}
