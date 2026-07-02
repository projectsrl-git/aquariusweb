package com.aquarius.entity.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Credenziali "web" dell'operatore Aquarius — tabella NUOVA, tenant-scoped.
 *
 * Sta nel DB del tenant accanto a {@code res_oper} (con prefisso {@code aq_web_}
 * per non confondersi con tabelle legacy). Aquarius web la possiede in
 * esclusiva — il VFP non la vede né la tocca.
 *
 * Chiave logica: {@code operatorCode = res_oper.CODICE}. Volutamente non c'è
 * foreign key fisica verso res_oper.id_unique perché:
 *   - res_oper resta gestita dal VFP, vogliamo zero accoppiamento DDL
 *   - se un utente legacy viene cancellato dal VFP, qui rimane uno "stranded"
 *     che è OK: si pulirà manualmente o con un job di housekeeping
 *
 * Strategia dati: vedi docs/STRATEGIA_DATI.md.
 */
@Entity
@Table(name = "aq_web_user_credentials",
       uniqueConstraints = @UniqueConstraint(name = "uk_aq_web_credentials_op_code",
                                             columnNames = "operator_code"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebUserCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** CODICE dell'operatore in res_oper (logical FK). */
    @Column(name = "operator_code", length = 20, nullable = false)
    private String operatorCode;

    /** Hash BCrypt della password. */
    @Column(name = "password_hash", length = 200, nullable = false)
    private String passwordHash;

    /**
     * Se true, al prossimo login l'utente è forzato al cambio password
     * prima di poter usare l'app (es. dopo un reset del super-admin, o al primo
     * accesso post-creazione delle credenziali).
     */
    @Column(name = "must_reset_password", nullable = false)
    private boolean mustResetPassword;

    /** Token monouso per reset password via email. Valido se non scaduto. */
    @Column(name = "reset_token", length = 100)
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    /** Timestamp ultimo login riuscito. */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
