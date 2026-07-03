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
 * Anagrafica cliente — mapping di {@code U_CLI_AN} (legacy, ~28k record in
 * Impresind). Replica del form VFP {@code MENU_CLI000} (Aggiornamento
 * anagrafica clienti) — 254 colonne totali in DB, qui mappate ~50 colonne
 * principali per i 7 tab attivi della prima iterazione web.
 *
 * <h3>{@code @DynamicUpdate}</h3>
 * Fondamentale: Hibernate genera UPDATE solo per le colonne effettivamente
 * cambiate, NON per tutti i campi @Column. Significa che le 200+ colonne NON
 * mappate qui non vengono toccate da un nostro save — il VFP continua a
 * leggerle e scriverle senza che la web app interferisca.
 *
 * <h3>Read-only / updatable</h3>
 * <ul>
 *   <li>{@code id_unique}, {@code CLI_CODSOC}, {@code CLI_CODCLI}:
 *       chiave/business key — read-only (insertable=false, updatable=false).</li>
 *   <li>Tutti gli altri campi: updatable.</li>
 * </ul>
 *
 * <h3>Concorrenza VFP/Web</h3>
 * Last-write-wins. La tabella legacy non ha colonna version e non possiamo
 * aggiungerne una (regola 1.3). Se il VFP modifica lo stesso record nel
 * frattempo, vince l'ultimo che salva. Per la prima iterazione è accettabile.
 */
@Entity
@Table(name = "U_CLI_AN")
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    // ─── Chiave logica (immutabile dal web) ────────────────────────────────
    @Column(name = "CLI_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    @Column(name = "CLI_CODCLI", length = 10, insertable = false, updatable = false)
    private String code;

    // ─── Tab "Anagrafica" ──────────────────────────────────────────────────
    @Column(name = "CLI_RAGSOC", length = 150)
    private String businessName;

    @Column(name = "CLI_RAGDUE", length = 80)
    private String businessName2;

    @Column(name = "CLI_PARIVA", length = 11)
    private String vatNumber;

    @Column(name = "CLI_CODFIS", length = 16)
    private String taxCode;

    /** "F" = persona fisica, "G" = persona giuridica (o vuoto). */
    @Column(name = "CLI_PF", length = 1)
    private String personType;

    @Column(name = "CLI_IVACEE", length = 20)
    private String vatCeeNumber;

    // ─── Tab "Indirizzo" ───────────────────────────────────────────────────
    @Column(name = "CLI_INDIR", length = 150)
    private String address;

    @Column(name = "CLI_CAP", length = 15)
    private String zipCode;

    @Column(name = "CLI_LOCALI", length = 150)
    private String city;

    @Column(name = "CLI_PROVIN", length = 2)
    private String province;

    @Column(name = "CLI_NAZION", length = 3)
    private String country;

    @Column(name = "CLI_ZONA", length = 3)
    private String zone;

    // ─── Tab "Contatti" ────────────────────────────────────────────────────
    @Column(name = "CLI_TELEFO", length = 30)
    private String phone;

    @Column(name = "CLI_FAX", length = 30)
    private String fax;

    @Column(name = "CLI_TELEX", length = 30)
    private String telex;

    @Column(name = "CLI_EMAIL", length = 100)
    private String email;

    @Column(name = "CLI_EMAIL1", length = 100)
    private String emailAlt1;

    @Column(name = "CLI_EMAIL2", length = 100)
    private String emailAlt2;

    // ─── Tab "Commerciali" ─────────────────────────────────────────────────
    @Column(name = "CLI_AGE", length = 3)
    private String agent;

    @Column(name = "CLI_CONPAG", length = 3)
    private String paymentTerms;

    @Column(name = "CLI_LISTIN", length = 1)
    private String priceList;

    @Column(name = "CLI_VALUTA", length = 3)
    private String currency;

    @Column(name = "CLI_SCON", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "CLI_CLFIDO", length = 1)
    private String creditLimitEnabled;

    @Column(name = "CLI_IMPFID", precision = 15, scale = 3)
    private BigDecimal creditLimit;

    @Column(name = "CLI_TOTORD", precision = 9, scale = 0)
    private BigDecimal totalOrders;

    // ─── Tab "IVA / Fiscale" ───────────────────────────────────────────────
    @Column(name = "CLI_CODIVA", length = 3)
    private String vatRateCode;

    @Column(name = "CLI_ALRID", length = 3)
    private String alternateVatCode;

    @Column(name = "CLI_UFFIVA", length = 30)
    private String taxOffice;

    @Column(name = "CLI_SOSIMP", length = 10)
    private String suspendedTax;

    /** Codice classificazione cliente esonerato (dich. intento). */
    @Column(name = "CLI_CLIES", length = 24)
    private String exemptionCode;

    @Column(name = "CLI_DTCLES", length = 10)
    private String exemptionDate;

    // ─── Tab "Banca" ───────────────────────────────────────────────────────
    @Column(name = "CLI_BAE", length = 10)
    private String bankCode;

    @Column(name = "CLI_CDIBAN", length = 29)
    private String iban;

    @Column(name = "CLI_CODABI", length = 5)
    private String abi;

    @Column(name = "CLI_CODCAB", length = 5)
    private String cab;

    // ─── Tab "Note" ────────────────────────────────────────────────────────
    @Column(name = "CLI_NOTE", columnDefinition = "text")
    private String notes;

    // ═══════════════════════════════════════════════════════════════
    //  Campi tab estesi (Vettori, Contabili, Posticipi, Fido, Rif.cliente,
    //  Web, Legali, Produzione, Gruppi E-mail, Testi, Fatturare a,
    //  Intrastat, Fattura elettronica) — verificati su schema U_CLI_AN.
    // ═══════════════════════════════════════════════════════════════

    // ── tab-vett ──
    @Column(name = "CLI_MEZZO", length = 3)
    private String shipMethod;
    @Column(name = "CLI_PORTO", length = 3)
    private String shipPort;
    @Column(name = "CLI_VETTO1", length = 4)
    private String carrier1;
    @Column(name = "CLI_VETTO2", length = 4)
    private String carrier2;
    @Column(name = "CLI_VETTO3", length = 4)
    private String carrier3;
    @Column(name = "CLI_CODIMB", length = 3)
    private String packagingCode;
    @Column(name = "CLI_SPINCA", precision = 9, scale = 2)
    private BigDecimal packagingCharge;
    @Column(name = "CLI_CLISPE", length = 1)
    private String shipToSelf;
    @Column(name = "CLI_SPTRAS", precision = 9, scale = 2)
    private BigDecimal transportCharge;
    @Column(name = "CLI_DELDOC", length = 50)
    private String deliveryNote;
    @Column(name = "CLI_ORDDDT")
    private Boolean orderAsDdt;

    // ── tab-contab ──
    @Column(name = "CLI_CONTO1", length = 13)
    private String account1;
    @Column(name = "CLI_CONTO2", length = 13)
    private String account2;
    @Column(name = "CLI_CONTO3", length = 13)
    private String account3;
    @Column(name = "CLI_CONTO4", length = 13)
    private String account4;
    @Column(name = "CLI_CONTO5", length = 13)
    private String account5;
    @Column(name = "CLI_CCOST1", length = 13)
    private String costCenter1;
    @Column(name = "CLI_CCOST2", length = 13)
    private String costCenter2;
    @Column(name = "CLI_CCOST3", length = 13)
    private String costCenter3;
    @Column(name = "CLI_CCOST4", length = 13)
    private String costCenter4;
    @Column(name = "CLI_CCOST5", length = 13)
    private String costCenter5;
    @Column(name = "CLI_PERCON1", precision = 9, scale = 2)
    private BigDecimal accountPct1;
    @Column(name = "CLI_PERCON2", precision = 9, scale = 2)
    private BigDecimal accountPct2;
    @Column(name = "CLI_PERCON3", precision = 9, scale = 2)
    private BigDecimal accountPct3;
    @Column(name = "CLI_PERCON4", precision = 9, scale = 2)
    private BigDecimal accountPct4;
    @Column(name = "CLI_INCASS", length = 13)
    private String collectionAccount;

    // ── tab-postic ──
    @Column(name = "CLI_BDAL", length = 4)
    private String postpone1From;
    @Column(name = "CLI_BAL", length = 4)
    private String postpone1To;
    @Column(name = "CLI_GG1", length = 4)
    private String postpone1Days;
    @Column(name = "CLI_B2DAL", length = 4)
    private String postpone2From;
    @Column(name = "CLI_B2AL", length = 4)
    private String postpone2To;
    @Column(name = "CLI_GG2", length = 4)
    private String postpone2Days;
    @Column(name = "CLI_01DAL", length = 4)
    private String postponeM01From;
    @Column(name = "CLI_01AL", length = 4)
    private String postponeM01To;
    @Column(name = "CLI_01GG", length = 4)
    private String postponeM01Days;
    @Column(name = "CLI_02DAL", length = 4)
    private String postponeM02From;
    @Column(name = "CLI_02AL", length = 4)
    private String postponeM02To;
    @Column(name = "CLI_02GG", length = 4)
    private String postponeM02Days;
    @Column(name = "CLI_03DAL", length = 4)
    private String postponeM03From;
    @Column(name = "CLI_03AL", length = 4)
    private String postponeM03To;
    @Column(name = "CLI_03GG", length = 4)
    private String postponeM03Days;
    @Column(name = "CLI_04DAL", length = 4)
    private String postponeM04From;
    @Column(name = "CLI_04AL", length = 4)
    private String postponeM04To;
    @Column(name = "CLI_04GG", length = 4)
    private String postponeM04Days;
    @Column(name = "CLI_05DAL", length = 4)
    private String postponeM05From;
    @Column(name = "CLI_05AL", length = 4)
    private String postponeM05To;
    @Column(name = "CLI_05GG", length = 4)
    private String postponeM05Days;
    @Column(name = "CLI_06DAL", length = 4)
    private String postponeM06From;
    @Column(name = "CLI_06AL", length = 4)
    private String postponeM06To;
    @Column(name = "CLI_06GG", length = 4)
    private String postponeM06Days;
    @Column(name = "CLI_07DAL", length = 4)
    private String postponeM07From;
    @Column(name = "CLI_07AL", length = 4)
    private String postponeM07To;
    @Column(name = "CLI_07GG", length = 4)
    private String postponeM07Days;
    @Column(name = "CLI_09DAL", length = 4)
    private String postponeM09From;
    @Column(name = "CLI_09AL", length = 4)
    private String postponeM09To;
    @Column(name = "CLI_09GG", length = 4)
    private String postponeM09Days;
    @Column(name = "CLI_10DAL", length = 4)
    private String postponeM10From;
    @Column(name = "CLI_10AL", length = 4)
    private String postponeM10To;
    @Column(name = "CLI_10GG", length = 4)
    private String postponeM10Days;
    @Column(name = "CLI_11DAL", length = 4)
    private String postponeM11From;
    @Column(name = "CLI_11AL", length = 4)
    private String postponeM11To;
    @Column(name = "CLI_11GG", length = 4)
    private String postponeM11Days;

    // ── tab-fido ──
    @Column(name = "CLI_CLFIDO", length = 1)
    private String creditClass;
    @Column(name = "CLI_IMPFID", precision = 9, scale = 2)
    private BigDecimal creditAmount;
    @Column(name = "CLI_CLFID2", length = 1)
    private String creditClass2;
    @Column(name = "CLI_IMPFI2", precision = 9, scale = 2)
    private BigDecimal creditAmount2;
    @Column(name = "CLI_CLASSE", length = 1)
    private String riskClass;
    @Column(name = "CLI_FLGFID")
    private Boolean creditCheckFlag;

    // ── tab-rifcli ──
    @Column(name = "CLI_RECCAT", precision = 5, scale = 2)
    private BigDecimal refCategory;
    @Column(name = "CLI_RECCOG", length = 24)
    private String refLastName;
    @Column(name = "CLI_RECNOM", length = 20)
    private String refFirstName;
    @Column(name = "CLI_RECCDF", length = 16)
    private String refTaxCode;
    @Column(name = "CLI_RECTIP", length = 1)
    private String refType;
    @Column(name = "CLI_RECDES", length = 30)
    private String refDescription;

    // ── tab-web ──
    @Column(name = "CLI_HOMPAG", length = 50)
    private String homePage;
    @Column(name = "CLI_PRIVAC", length = 1)
    private String privacyConsent;
    @Column(name = "CLI_APPPRO", length = 6)
    private String webProfile;

    // ── tab-legali ──
    @Column(name = "CLI_RAGLEG", length = 80)
    private String legalName;
    @Column(name = "CLI_RGLEG2", length = 80)
    private String legalName2;
    @Column(name = "CLI_INDLEG", length = 80)
    private String legalAddress;
    @Column(name = "CLI_LOCLEG", length = 80)
    private String legalCity;
    @Column(name = "CLI_CAPLEG", length = 10)
    private String legalZip;
    @Column(name = "CLI_PROLEG", length = 2)
    private String legalProvince;
    @Column(name = "CLI_NAZLEG", length = 3)
    private String legalCountry;
    @Column(name = "CLI_PIVALG", length = 11)
    private String legalVat;
    @Column(name = "CLI_CFISLG", length = 16)
    private String legalTaxCode;
    @Column(name = "CLI_IVCELG", length = 20)
    private String legalVatCee;
    @Column(name = "CLI_LEGALE")
    private Boolean hasLegalSeat;

    // ── tab-prod ──
    @Column(name = "CLI_FLGAGG")
    private Boolean prodFlagUpd;
    @Column(name = "CLI_FLGRCQ")
    private Boolean prodFlagQc;
    @Column(name = "CLI_FLGABP")
    private Boolean prodFlagAbp;
    @Column(name = "CLI_VENROT")
    private Boolean prodRotation;
    @Column(name = "CLI_DESROT", length = 40)
    private String prodRotationDesc;
    @Column(name = "CLI_FLGETI")
    private Boolean prodFlagLabel;
    @Column(name = "CLI_FLGPES")
    private Boolean prodFlagWeight;
    @Column(name = "CLI_ETINOB", length = 30)
    private String prodLabelNote;
    @Column(name = "CLI_NODISP")
    private Boolean prodNoDisplay;
    @Column(name = "CLI_NOFINI")
    private Boolean prodNoFinished;
    @Column(name = "CLI_PESOORD")
    private Boolean prodWeightOrder;
    @Column(name = "CLI_PESOBOL")
    private Boolean prodWeightDdt;
    @Column(name = "CLI_PESOFAT")
    private Boolean prodWeightInv;
    @Column(name = "CLI_EVATOT")
    private Boolean prodFullDelivery;
    @Column(name = "CLI_GGEVAS", precision = 5, scale = 2)
    private BigDecimal prodDeliveryDays;
    @Column(name = "CLI_FLAVV1")
    private Boolean prodNotifyFlag;

    // ── tab-emails ──
    @Column(name = "CLI_EMAIL", length = 100)
    private String emailGroup;
    @Column(name = "CLI_EMAIL1", length = 100)
    private String emailGroup1;
    @Column(name = "CLI_EMAIL2", length = 100)
    private String emailGroup2;

    // ── tab-testi ──
    @Column(name = "CLI_CODTXT", length = 3)
    private String textCode;
    @Column(name = "CLI_TXTORD", columnDefinition = "text")
    private String textOrder;
    @Column(name = "CLI_TXTBOL", columnDefinition = "text")
    private String textDdt;
    @Column(name = "CLI_TXTFAT", columnDefinition = "text")
    private String textInvoice;
    @Column(name = "CLI_TXTFAP", columnDefinition = "text")
    private String textProforma;
    @Column(name = "CLI_NOTTAG", columnDefinition = "text")
    private String textTag;
    @Column(name = "CLI_NOTSPE", columnDefinition = "text")
    private String textSpecial;

    // ── tab-fattu ──
    @Column(name = "CLI_FATAPR", precision = 9, scale = 2)
    private BigDecimal invoiceToCode;
    @Column(name = "CLI_TIPFAT", length = 3)
    private String invoiceType;

    // ── tab-intra ──
    @Column(name = "CLI_NAZIONA", length = 3)
    private String intraNationality;
    @Column(name = "CLI_ESTNAZ", length = 3)
    private String intraCountry;
    @Column(name = "CLI_ESTNAS", length = 10)
    private String intraBirthPlace;
    @Column(name = "CLI_ESTCOM", length = 40)
    private String intraBirthCity;
    @Column(name = "CLI_ESTDOM", length = 3)
    private String intraDomicile;
    @Column(name = "CLI_PRONAS", length = 2)
    private String intraBirthProv;

    // ── tab-fe ──
    @Column(name = "CLI_POSALL")
    private Boolean feAttachment;
}
