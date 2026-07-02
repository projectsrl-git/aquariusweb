package com.aquarius.entity.system;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Account super-admin: globale, non tenant-scoped.
 *
 * Serve esclusivamente per gestire i tenant da UI (creazione, abilitazione,
 * configurazione connection string). Gli utenti finali NON sono super-admin:
 * loggano come operatori di un tenant specifico (vedi {@code OperatorUser}).
 */
@Entity
@Table(name = "super_admins")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    /** BCrypt hash. */
    @Column(nullable = false, length = 200)
    private String passwordHash;

    @Column(length = 200)
    private String email;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
