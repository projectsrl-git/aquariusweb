package com.aquarius.config;

import com.aquarius.multitenancy.TenantContext;
import com.aquarius.multitenancy.TenantsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Esegue le migrazioni dei TENANT DB.
 *
 * Per ogni tenant configurato in {@link TenantsProperties}:
 *  1. Setta {@link TenantContext} sul tenant → il {@code tenantDataSource}
 *     (un AbstractRoutingDataSource) instrada le connessioni a quel pool.
 *  2. Crea un {@link SqlMigrationRunner} che applica
 *     {@code classpath:db/migration/tenant/V*.sql}.
 *  3. Tracking in {@code aq_web_schema_history} (prefisso aq_web_ coerente
 *     con la strategia 1.3: tutte le nostre tabelle hanno questo prefisso).
 *
 * Eseguito ad {@code @Order(0)} cioè prima di qualunque altro CommandLineRunner —
 * il resto può assumere che le tabelle aq_web_* esistano.
 *
 * NB: se il caso "due tenant puntano allo stesso DB fisico" (configurazione di
 * test in cui Tremonti punta a IMPRESIND_TEST), il secondo ciclo troverà tutto
 * già applicato dal primo (stessa history table, stesso DB) e non farà nulla.
 * Comportamento corretto.
 */
@Component
@Order(0)
@Slf4j
public class TenantMigrationsRunner implements CommandLineRunner {

    private final TenantsProperties tenantsProperties;
    private final DataSource tenantDataSource;

    public TenantMigrationsRunner(
            TenantsProperties tenantsProperties,
            @Qualifier("tenantDataSource") DataSource tenantDataSource) {
        this.tenantsProperties = tenantsProperties;
        this.tenantDataSource = tenantDataSource;
    }

    @Override
    public void run(String... args) {
        for (String tenantId : tenantsProperties.getTenants().keySet()) {
            migrateTenant(tenantId);
        }
    }

    private void migrateTenant(String tenantId) {
        try {
            TenantContext.set(tenantId);
            SqlMigrationRunner runner = new SqlMigrationRunner(
                tenantDataSource,
                "classpath:db/migration/tenant",
                "aq_web_schema_history",
                "TENANT '" + tenantId + "'"
            );
            runner.migrate();
        } finally {
            TenantContext.clear();
        }
    }
}
