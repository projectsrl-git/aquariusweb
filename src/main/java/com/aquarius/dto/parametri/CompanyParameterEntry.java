package com.aquarius.dto.parametri;

import lombok.Data;

/**
 * Riga del catalogo parametri aziendali
 * ({@code parametri/parametri_aziendali_catalog.csv}) arricchita, a runtime,
 * col valore corrente letto dalle tabelle {@code U_AZI_*} della societa'
 * corrente. Read-only: il viewer non modifica nulla.
 */
@Data
public class CompanyParameterEntry {
    private String group;
    private String objectName;
    private String label;
    private String tableColumn;
    private String type;           // boolean | text | number | list
    private String allowedValues;
    private String purpose;
    private String howItWorks;
    private String usedIn;
    private String confidence;     // ALTA | MEDIA | INCERTO
    private String notes;
    private String column;         // nome colonna AZI_ (per lookup valore)

    // valorizzati a runtime
    private String currentValue;
    private boolean valuePresent;

    public boolean isBoolean() { return "boolean".equals(type); }
    public boolean isUncertain() { return "INCERTO".equals(confidence); }
    public boolean isAbsentColumn() {
        return tableColumn != null && tableColumn.startsWith("(");
    }

    /** Gruppo di primo livello (tab del form) per il raggruppamento UI. */
    public String getTopGroup() {
        if (group == null || group.isBlank()) return "(fuori tab)";
        int i = group.indexOf(" > ");
        return i > 0 ? group.substring(0, i) : group;
    }
}
