package com.aquarius.entity.system;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Tenant = società/azienda registrata nel sistema.
 *
 * Corrisponde concettualmente a un record di DBSOCIETA.DBF del vecchio Aquarius VFP,
 * con la differenza che qui memorizziamo solo i metadati. La connection string vera
 * sta in application.properties (o, in produzione, in un secret store).
 *
 * Sì, c'è apparente duplicazione fra questa tabella e la sezione
 * {@code aquarius.tenants.*} di application.properties: la tabella esiste per
 * permettere al super-admin di vedere/abilitare/disabilitare i tenant da UI
 * senza ridistribuire un .jar.
 */
@Entity
@Table(name = "tenants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    /** Slug identificativo (es. "impresind", "tremonti"). Coincide con la chiave
     *  in aquarius.tenants.* e con il lookup key del TenantRoutingDataSource. */
    @Id
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    /** Nome esteso visualizzato nella combobox di login. */
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    /** Tipo di database backend (informativo: 'sqlserver', 'h2', 'postgres'). */
    @Column(name = "db_type", length = 40)
    private String dbType;

    /** Tenant abilitato? Se false, non compare nella combobox di login. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /** Path al logo per il branding in app (relativo a /static/img/). */
    @Column(name = "logo_path", length = 250)
    private String logoPath;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
