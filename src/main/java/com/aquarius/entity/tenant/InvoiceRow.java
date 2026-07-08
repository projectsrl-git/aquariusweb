package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Riga fattura di vendita — read-only mapping of {@code U_FAT_DD}.
 * Row→header hook: {@code DAGGANCIO = TT.TAGGANCIO}.
 */
@Entity
@Table(name = "U_FAT_DD")
@DynamicUpdate
@Data
public class InvoiceRow {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "DAGGANCIO", length = 10, insertable = false, updatable = false)
    private String aggancio;

    @Column(name = "ORD_SEQUEN", insertable = false, updatable = false)
    private BigDecimal sequence;

    @Column(name = "ORD_CODART", length = 30, insertable = false, updatable = false)
    private String articleCode;

    @Column(name = "ORD_MAGA", length = 6, insertable = false, updatable = false)
    private String articleExtension;

    @Column(name = "ORD_DESART", length = 254, insertable = false, updatable = false)
    private String description;

    @Column(name = "ORD_DES2", length = 254, insertable = false, updatable = false)
    private String description2;

    @Column(name = "ORD_QTAORD", insertable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "ORD_PREZZO", insertable = false, updatable = false)
    private BigDecimal price;

    @Column(name = "ORD_PRZNET", insertable = false, updatable = false)
    private BigDecimal netPrice;

    @Column(name = "ORD_VALORE", insertable = false, updatable = false)
    private BigDecimal rowValue;

    @Column(name = "ORD_IVA", length = 3, insertable = false, updatable = false)
    private String vatCode;

    // ─── Campi di dettaglio riga (mostrati nell'espansione) ─────────────
    @Column(name = "ORD_UM", length = 2, insertable = false, updatable = false)
    private String um;

    @Column(name = "ORD_UMFIN", length = 3, insertable = false, updatable = false)
    private String umFinal;

    @Column(name = "ORD_PESOUM", insertable = false, updatable = false)
    private BigDecimal weightPerUm;

    @Column(name = "ORD_SPESSO", insertable = false, updatable = false)
    private BigDecimal thickness;

    @Column(name = "ORD_ALTEZZ", insertable = false, updatable = false)
    private BigDecimal height;

    @Column(name = "ORD_LUNGHE", insertable = false, updatable = false)
    private BigDecimal length;

    @Column(name = "ORS_NUMORC", length = 6, insertable = false, updatable = false)
    private String sourceOrderNo;

    @Column(name = "ORS_DATORD", length = 10, insertable = false, updatable = false)
    private String sourceOrderDate;

    @Column(name = "ORS_DESTIN", length = 70, insertable = false, updatable = false)
    private String destination;

    /**
     * Nota/commento di riga (CLOB legacy {@code ORD_NOTE}, tipo SQL Server text).
     * Per le righe COMMENTO qui c'è il testo esploso del commento.
     */
    @Column(name = "ORD_NOTE", columnDefinition = "text", insertable = false, updatable = false)
    private String note;

    @Column(name = "ORD_NOTEBO", columnDefinition = "text", insertable = false, updatable = false)
    private String noteInternal;

    /** True when the row is a comment line (legacy convention on ORD_DESART). */
    public boolean isComment() {
        return description != null && description.trim().startsWith("*** COMMENTO ***");
    }

    /**
     * Testo da mostrare per una riga COMMENTO: il corpo reale sta in ORD_NOTE
     * (vedi FATTURAZIONE_ELETTRONICA_LIB: ORD_DESART='*** COMMENTO ***' →
     * SUBSTRING(ORD_NOTE,1,1000)). Se ORD_NOTE è vuoto, ripiega sul placeholder.
     */
    public String getCommentText() {
        if (note != null && !note.trim().isEmpty()) return note.trim();
        return description;
    }

    /** True se la riga ha campi di dettaglio valorizzati (per mostrare l'espansione). */
    public boolean isHasDetail() {
        return notBlank(um) || notBlank(umFinal) || nz(weightPerUm) || nz(thickness)
            || nz(height) || nz(length) || notBlank(sourceOrderNo) || notBlank(destination)
            || notBlank(noteInternal)
            || (!isComment() && notBlank(note));
    }

    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }
    private static boolean nz(BigDecimal b) { return b != null && b.signum() != 0; }
}
