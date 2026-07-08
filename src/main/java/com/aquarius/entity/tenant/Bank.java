package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Banca — read-only mapping (minimal) of {@code U_BAN_AN}.
 * Web counterpart of MENU_BAN000.
 */
@Entity
@Table(name = "U_BAN_AN")
@DynamicUpdate
@Data
public class Bank {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "BAN_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    @Column(name = "BAN_CODBAN", length = 10, insertable = false, updatable = false)
    private String code;

    @Column(name = "BAN_DESBAN", length = 80, insertable = false, updatable = false)
    private String name;

    @Column(name = "BAN_INDIR", length = 50, insertable = false, updatable = false)
    private String address;

    @Column(name = "BAN_CAP", length = 5, insertable = false, updatable = false)
    private String zip;

    @Column(name = "BAN_LOCA", length = 30, insertable = false, updatable = false)
    private String city;

    @Column(name = "BAN_PROVIN", length = 2, insertable = false, updatable = false)
    private String province;

    @Column(name = "BAN_CODABI", length = 5, insertable = false, updatable = false)
    private String abi;

    @Column(name = "BAN_CODCAB", length = 5, insertable = false, updatable = false)
    private String cab;

    @Column(name = "BAN_CIN", length = 3, insertable = false, updatable = false)
    private String cin;

    @Column(name = "ban_iban", length = 29, insertable = false, updatable = false)
    private String iban;

    @Column(name = "ban_swift", length = 11, insertable = false, updatable = false)
    private String swift;

    @Column(name = "BAN_NOTE1", length = 40, insertable = false, updatable = false)
    private String note1;

    @Column(name = "BAN_NOTE2", length = 40, insertable = false, updatable = false)
    private String note2;
}
