package com.aquarius.dto;

import lombok.Getter;

/** Una riga del migration tracker (.scx ↔ web). Vedi resources/migration/README.md. */
@Getter
public class MigrationRow {
    private final String formFile;
    private final String objectName;
    private final String objectType;
    private final String baseClass;
    private final String member;
    private final String memberKind;
    private final String purpose;
    private final String sourceTables;
    private final String status;       // MIGRATED | PARTIAL | NOT_MIGRATED | NOT_APPLICABLE
    private final String newLocation;
    private final String reasonCode;
    private final String reasonDetail;

    public MigrationRow(String[] c) {
        this.formFile     = at(c, 0);
        this.objectName   = at(c, 1);
        this.objectType   = at(c, 2);
        this.baseClass    = at(c, 3);
        this.member       = at(c, 4);
        this.memberKind   = at(c, 5);
        this.purpose      = at(c, 6);
        this.sourceTables = at(c, 7);
        this.status       = at(c, 8);
        this.newLocation  = at(c, 9);
        this.reasonCode   = at(c, 10);
        this.reasonDetail = at(c, 11);
    }

    private static String at(String[] a, int i) {
        return (a != null && i < a.length && a[i] != null) ? a[i].trim() : "";
    }

    public boolean isMigrated() { return "MIGRATED".equals(status); }
}
