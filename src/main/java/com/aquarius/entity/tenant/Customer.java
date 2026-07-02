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
}
