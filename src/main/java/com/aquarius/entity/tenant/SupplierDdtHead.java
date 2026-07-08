package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Documento di carico da fornitore (testata) — read-only mapping of
 * {@code U_BFO_TT}. Web counterpart of MENU_BFO000 ("Carico da fornitore",
 * entrata merce). NOTE on archives: the handoff mentioned U_BOF_TT, but
 * the sources show U_BOF_* is the "bollette fiscali" flow towards CUSTOMERS
 * (BOFCONSE opens U_CLI_AN; menu labels "Scarico con bolletta fiscale");
 * the supplier-inbound archive is U_BFO_* — used here.
 * ORD_NUMORD/ORD_DATORD are the document's OWN number/date (Ristampa
 * convention); the linked purchase order is the ORC pair. ORD_TIPO = 9
 * marks "resi da clienti" (verified in the Ristampa catalog).
 */
@Entity
@Table(name = "U_BFO_TT")
@DynamicUpdate
@Data
public class SupplierDdtHead {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "TAGGANCIO", length = 10, insertable = false, updatable = false)
    private String aggancio;

    @Column(name = "ORD_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    @Column(name = "ORD_ANNO", length = 4, insertable = false, updatable = false)
    private String fiscalYear;

    /** Document's own number/date (Ristampa convention). */
    @Column(name = "ORD_NUMORD", length = 6, insertable = false, updatable = false)
    private String documentNumber;

    @Column(name = "ORD_DATORD", length = 10, insertable = false, updatable = false)
    private String documentDate;

    /** Supplier code (legacy reuses ORD_CODCLI on purchase docs). */
    @Column(name = "ORD_CODCLI", length = 10, insertable = false, updatable = false)
    private String supplierCode;

    @Column(name = "ORD_RAGSOC", length = 40, insertable = false, updatable = false)
    private String supplierName;

    @Column(name = "ORD_CAUS", length = 10, insertable = false, updatable = false)
    private String causale;

    @Column(name = "ORD_RIFERI", length = 30, insertable = false, updatable = false)
    private String reference;

    @Column(name = "ORD_VALORE", insertable = false, updatable = false)
    private BigDecimal documentValue;

    @Column(name = "ORD_VALUTA", length = 3, insertable = false, updatable = false)
    private String currency;

    /** Linked purchase order (ORC pair). */
    @Column(name = "ORD_NUMORC", length = 6, insertable = false, updatable = false)
    private String linkedOrderNumber;

    @Column(name = "ORD_DATORC", length = 10, insertable = false, updatable = false)
    private String linkedOrderDate;

    /** 9 = reso da cliente (verified discriminator, Ristampa RDC). */
    @Column(name = "ORD_TIPO", insertable = false, updatable = false)
    private BigDecimal documentType;

    public boolean isCustomerReturn() {
        return documentType != null && documentType.intValue() == 9;
    }
}
