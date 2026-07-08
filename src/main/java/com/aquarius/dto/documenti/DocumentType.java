package com.aquarius.dto.documenti;

/**
 * Catalogo tipi documento del cruscotto "Ristampa documenti" — porting del
 * CASE su {@code _V_TIPDOC} del form VFP {@code MENU_RISTAMPA_DOC} (array
 * {@code A_SCELTE} + mappa {@code _V_ARCH_TT}/{@code _V_ARCH_DD}).
 *
 * Table names are CONTROLLED IDENTIFIERS (same safety argument as
 * {@code WarehouseValuationDao.DateBase}): they are substituted into SQL text
 * from this enum only — user input never reaches an identifier position.
 *
 * Legacy types NOT included here (tracked in the migration CSV):
 * RFI/ORT/REC/REF/BOF/BCL share a table with another type WITHOUT a
 * discriminator visible in the sources (NEEDS_DOMAIN); FAF maps to U_FAF_TT
 * which does not exist in the Impresind database (NOT_APPLICABLE).
 */
public enum DocumentType {

    ORD("Ordini clienti",                    "U_ORD_TT",   "U_ORD_DD",   'C', null,              "/ordini"),
    OFF("Preventivi clienti",                "U_OFF_TT",   "U_OFF_DD",   'C', null,              null),
    ORF("Ordini fornitori",                  "U_ORF_TT",   "U_ORF_DD",   'F', null,              null),
    BOL("Documenti di consegna a clienti",   "U_BOL_TT",   "U_BOL_DD",   'C', null,              "/ddt"),
    BFO("Documenti di carico da fornitore",  "U_BFO_TT",   "U_BFO_DD",   'F', null,              null),
    RDC("Resi da clienti",                   "U_BFO_TT",   "U_BFO_DD",   'C', "ORD_TIPO = 9",    null),
    FAP("Fatture proforma",                  "U_FAP_TT",   "U_FAP_DD",   'C', null,              "/proforma"),
    FAT("Fatture clienti / N.C. clienti",    "U_FAT_TT",   "U_FAT_DD",   'C', null,              "/fatture"),
    INR("Interventi tecnici / riparazioni",  "VENDITE_TT", "VENDITE_DD", 'C', null,              null);

    private final String label;
    private final String ttTable;
    private final String ddTable;
    /** 'C' = cliente, 'F' = fornitore (legacy {@code _V_CLIFOR}). */
    private final char party;
    /** Extra WHERE fragment from the legacy (e.g. RDC: {@code ord_tipo = 9}), or null. */
    private final String extraWhere;
    /** Route of the dedicated consultation module, or null → generic detail only. */
    private final String moduleRoute;

    DocumentType(String label, String ttTable, String ddTable, char party,
                 String extraWhere, String moduleRoute) {
        this.label = label;
        this.ttTable = ttTable;
        this.ddTable = ddTable;
        this.party = party;
        this.extraWhere = extraWhere;
        this.moduleRoute = moduleRoute;
    }

    public String label()       { return label; }
    public String ttTable()     { return ttTable; }
    public String ddTable()     { return ddTable; }
    public char party()         { return party; }
    public String extraWhere()  { return extraWhere; }
    public String moduleRoute() { return moduleRoute; }

    public String partyLabel() { return party == 'F' ? "Fornitore" : "Cliente"; }

    /** Case-insensitive, null-safe parse; defaults to ORD (first legacy choice with data). */
    public static DocumentType from(String s) {
        if (s == null) return ORD;
        try {
            return DocumentType.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ORD;
        }
    }
}
