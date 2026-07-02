package com.aquarius.entity.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Operatore Aquarius — mapping READ-ONLY (concettualmente) della tabella
 * legacy {@code res_oper} del DB del tenant.
 *
 * Strategia dati: vedi docs/STRATEGIA_DATI.md.
 *
 * Questa entity mappa SOLO le colonne legacy che ci servono per autenticazione
 * e autorizzazione. La tabella reale ha molte più colonne (64 ALTER nel tempo)
 * che qui non vediamo: Hibernate con {@code ddl-auto=validate} accetta un
 * subset, basta che le colonne mappate ESISTANO nella tabella.
 *
 * NESSUNA colonna nuova viene aggiunta a res_oper. Le informazioni
 * "web-only" (BCrypt hash, last_login, must_reset_password) stanno nella
 * tabella separata {@code aq_web_user_credentials} → vedi {@link WebUserCredentials}.
 *
 * Schema legacy minimo presupposto (da sc_sql/res_oper.SQL del repo VFP):
 *   id_unique  uniqueidentifier (PK)
 *   CODICE     varchar(20)   -- username
 *   DESCRI     varchar(25)   -- nome esteso
 *   PASSWORD   varchar(20)   -- offuscata DECODE() VFP
 *   IN_USO     bit
 *   RES_SOSPESO bit          -- flag sospensione (aggiunto da AGG_RES_OPER_*)
 *   RES_EMAIL  varchar(...)  -- email per reset (aggiunto da AGG_RES_OPER_*)
 *   SOCIETA    varchar(2)
 */
@Entity
@Table(name = "res_oper")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorUser {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier")
    private String id;

    /** Username (legacy: CODICE). */
    @Column(name = "CODICE", length = 20, nullable = false)
    private String code;

    /** Nome esteso (legacy: DESCRI, varchar(50) nello schema reale). */
    @Column(name = "DESCRI", length = 50)
    private String fullName;

    /**
     * Password offuscata legacy (DECODE-able), letta solo per fallback
     * di autenticazione finché il VFP è in uso in parallelo.
     * NON viene MAI scritta da Aquarius web (il VFP è proprietario della colonna).
     */
    @Column(name = "PASSWORD", length = 20, insertable = false, updatable = false)
    private String legacyPassword;

    @Column(name = "IN_USO", insertable = false, updatable = false)
    private Boolean inUso;

    /** Flag sospensione legacy. Read-only dalla web app. */
    @Column(name = "RES_SOSPESO", insertable = false, updatable = false)
    private Boolean suspended;

    /** Email per reset password (legacy: RES_EMAIL, varchar(100) nello schema reale). */
    @Column(name = "RES_EMAIL", length = 100, insertable = false, updatable = false)
    private String email;

    @Column(name = "SOCIETA", length = 2, insertable = false, updatable = false)
    private String societyCode;

    /**
     * Helper: l'utente è abilitato al login?
     * Replica della logica di PASS.SCX (utente trovato AND !RES_SOSPESO).
     */
    @Transient
    public boolean isEnabled() {
        return suspended == null || !suspended;
    }
}
