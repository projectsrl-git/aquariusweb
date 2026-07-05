package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Anagrafica fornitore — mapping di {@code U_FOR_AN} (legacy). Replica del
 * form VFP {@code MENU_FOR000}. Stessa impostazione di {@link Customer}:
 * chiave logica immutabile dal web, {@code @DynamicUpdate} + whitelist di
 * copia nel controller. Campi verificati sullo schema U_FOR_AN.
 */
@Entity
@Table(name = "U_FOR_AN")
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    // ─── Chiave logica (immutabile dal web) ────────────────────────────────
    @Column(name = "FOR_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    @Column(name = "FOR_CODCON", length = 10, insertable = false, updatable = false)
    private String code;


    // ─── tab-anag ───
    @Column(name = "FOR_RAGSOC", length = 80)
    private String businessName;
    @Column(name = "FOR_RAGDUE", length = 80)
    private String businessName2;
    @Column(name = "FOR_PARIVA", length = 11)
    private String vatNumber;
    @Column(name = "FOR_CODFIS", length = 16)
    private String taxCode;
    @Column(name = "FOR_IVACEE", length = 20)
    private String vatCee;
    @Column(name = "FOR_RICERC", length = 10)
    private String searchKey;
    @Column(name = "FOR_ZONA", length = 3)
    private String zone;

    // ─── tab-indir ───
    @Column(name = "FOR_INDIR", length = 70)
    private String address;
    @Column(name = "FOR_LOCALI", length = 80)
    private String city;
    @Column(name = "FOR_CAP", length = 5)
    private String zipCode;
    @Column(name = "FOR_PROVIN", length = 2)
    private String province;
    @Column(name = "FOR_NAZION", length = 3)
    private String country;
    @Column(name = "FOR_TELEFO", length = 30)
    private String phone;
    @Column(name = "FOR_FAX", length = 30)
    private String fax;
    @Column(name = "FOR_TELEX", length = 30)
    private String telex;
    @Column(name = "FOR_EMAIL", length = 100)
    private String email;

    // ─── tab-iva ───
    @Column(name = "FOR_CLICEE", length = 10)
    private String ceeCustomer;

    // ─── tab-contab ───
    @Column(name = "FOR_PERCON1", precision = 9, scale = 2)
    private BigDecimal accountPct1;
    @Column(name = "FOR_PERCON2", precision = 9, scale = 2)
    private BigDecimal accountPct2;
    @Column(name = "FOR_PERCON3", precision = 9, scale = 2)
    private BigDecimal accountPct3;
    @Column(name = "FOR_PERCON4", precision = 9, scale = 2)
    private BigDecimal accountPct4;
    @Column(name = "FOR_PERCON5", precision = 9, scale = 2)
    private BigDecimal accountPct5;

    // ─── tab-banca ───
    @Column(name = "FOR_NUMCOC", length = 20)
    private String bankAccountNo;
    @Column(name = "FOR_CODABI", length = 5)
    private String abi;
    @Column(name = "FOR_CODCAB", length = 5)
    private String cab;
    @Column(name = "FOR_CODCIN", length = 2)
    private String cin;
    @Column(name = "FOR_CDIBAN", length = 29)
    private String iban;
    @Column(name = "FOR_BA2CC", length = 20)
    private String bank2AccountNo;
    @Column(name = "FOR_BA2ABI", length = 5)
    private String bank2Abi;
    @Column(name = "FOR_BA2CAB", length = 5)
    private String bank2Cab;
    @Column(name = "FOR_BA2IBA", length = 29)
    private String bank2Iban;

    // ─── tab-comm ───
    @Column(name = "FOR_VALUTA", length = 3)
    private String currency;
    @Column(name = "FOR_GGFORN", precision = 5, scale = 2)
    private BigDecimal supplyDays;

    // ─── tab-rifforn ───
    @Column(name = "FOR_CODCLI", length = 10)
    private String ourCustomerCode;

    // ─── tab-contab ───
    @Column(name = "FOR_CONTO1", length = 13)
    private String account1;
    @Column(name = "FOR_CCOST1", length = 13)
    private String costCenter1;
    @Column(name = "FOR_CONTO2", length = 13)
    private String account2;
    @Column(name = "FOR_CCOST2", length = 13)
    private String costCenter2;
    @Column(name = "FOR_CONTO3", length = 13)
    private String account3;
    @Column(name = "FOR_CCOST3", length = 13)
    private String costCenter3;
    @Column(name = "FOR_CONTO4", length = 13)
    private String account4;
    @Column(name = "FOR_CCOST4", length = 13)
    private String costCenter4;
    @Column(name = "FOR_CONTO5", length = 13)
    private String account5;
    @Column(name = "FOR_CCOST5", length = 13)
    private String costCenter5;
}
