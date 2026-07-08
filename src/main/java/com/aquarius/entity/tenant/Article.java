package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Anagrafica articoli — read-only mapping of {@code U_ART_PR} (legacy).
 * Web counterpart of the VFP form {@code MENU_ART000} ("Anagrafica articoli",
 * 12 tabs). This is a pure consultation slice: every column is mapped
 * {@code insertable=false, updatable=false} and no save path exists.
 * Only the fields meaningful for consultation are mapped (the table has 254
 * columns); all names verified against {@code docs/db_schema/aquarius_schema_full.csv}.
 */
@Entity
@Table(name = "U_ART_PR")
@DynamicUpdate
@Data
public class Article {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    // ─── Logical key ────────────────────────────────────────────────────────
    @Column(name = "ART_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    /** VFP label "Codice articolo" (Page1 Anagrafica). */
    @Column(name = "ART_CODPRI", length = 30, insertable = false, updatable = false)
    private String code;

    /** VFP label "Estensione" — code extension/variant, part of the logical key. */
    @Column(name = "ART_MAGA", length = 6, insertable = false, updatable = false)
    private String extension;

    // ─── Descriptions (Page1 Anagrafica) ────────────────────────────────────
    /** VFP label "Descrizione". */
    @Column(name = "ART_DESCR", length = 254, insertable = false, updatable = false)
    private String description;

    /** Extended description (second description line in the VFP form). */
    @Column(name = "ART_DESEST", length = 254, insertable = false, updatable = false)
    private String extendedDescription;

    /** VFP label "Ridotta / REF". */
    @Column(name = "ART_DESRID", length = 30, insertable = false, updatable = false)
    private String shortDescription;

    // ─── Units of measure ───────────────────────────────────────────────────
    @Column(name = "ART_UNIMIS", length = 2, insertable = false, updatable = false)
    private String unitOfMeasure;

    @Column(name = "ART_UMMAGA", length = 3, insertable = false, updatable = false)
    private String warehouseUnit;

    // ─── Prices (VFP "Prezzi di vendita" 1..5 + "Prezzo di costo standard") ──
    @Column(name = "ART_PRZVEN", insertable = false, updatable = false)
    private BigDecimal salePrice1;

    @Column(name = "ART_PRZVE2", insertable = false, updatable = false)
    private BigDecimal salePrice2;

    @Column(name = "ART_PRZVE3", insertable = false, updatable = false)
    private BigDecimal salePrice3;

    @Column(name = "ART_PRZVE4", insertable = false, updatable = false)
    private BigDecimal salePrice4;

    @Column(name = "ART_PRZVE5", insertable = false, updatable = false)
    private BigDecimal salePrice5;

    @Column(name = "ART_PRZCOS", insertable = false, updatable = false)
    private BigDecimal standardCost;

    // ─── Fiscal / classification ────────────────────────────────────────────
    /** IVA code — PARA lookup with prefix "IVA" (same convention as primanota TOP). */
    @Column(name = "ART_CODIVA", length = 3, insertable = false, updatable = false)
    private String vatCode;

    /** Commodity category ("categoria merceologica"). */
    @Column(name = "ART_CATMER", length = 3, insertable = false, updatable = false)
    private String commodityCategory;

    @Column(name = "ART_CODCAT", length = 2, insertable = false, updatable = false)
    private String categoryCode;

    @Column(name = "ART_TIPO", length = 4, insertable = false, updatable = false)
    private String type;

    @Column(name = "ART_TIPOPR", length = 1, insertable = false, updatable = false)
    private String productType;

    // ─── Supplier / barcode (Page5 "Articoli fornitore", Page4 "Barcode") ───
    @Column(name = "ART_CODFOR", length = 10, insertable = false, updatable = false)
    private String supplierCode;

    @Column(name = "ART_BARCOD", length = 20, insertable = false, updatable = false)
    private String barcode;

    // ─── Warehouse data ──────────────────────────────────────────────────────
    /** VFP label "Scaffale" — physical location. */
    @Column(name = "ART_COORD", length = 50, insertable = false, updatable = false)
    private String location;

    @Column(name = "ART_SCORMI", insertable = false, updatable = false)
    private BigDecimal minStock;

    @Column(name = "ART_SCORMA", insertable = false, updatable = false)
    private BigDecimal maxStock;

    @Column(name = "ART_PESOKG", insertable = false, updatable = false)
    private BigDecimal weightKg;

    // ─── Status ──────────────────────────────────────────────────────────────
    /** VFP label "Obsoleto" — 'S' = obsolete article. */
    @Column(name = "ART_OBSOL", length = 1, insertable = false, updatable = false)
    private String obsolete;

    // ─── Notes (Page6 "Note") ────────────────────────────────────────────────
    @Column(name = "ART_NOTA1", length = 100, insertable = false, updatable = false)
    private String note1;

    @Column(name = "ART_NOTA2", length = 100, insertable = false, updatable = false)
    private String note2;

    @Column(name = "ART_NOTA3", length = 100, insertable = false, updatable = false)
    private String note3;

    /** True when the article is flagged obsolete (trims legacy space padding). */
    public boolean isObsolete() {
        return obsolete != null && "S".equalsIgnoreCase(obsolete.trim());
    }
}
