package com.aquarius.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Esegue le migrazioni del SYSTEM DB ad init-time del context, PRIMA che venga
 * creato {@code systemEntityManagerFactory} (che è {@code @DependsOn} di questo
 * bean).
 *
 * Le migrazioni stanno in {@code classpath:db/migration/system/V*.sql}.
 * Il tracking è nella tabella {@code system_schema_history} del SYSTEM DB.
 */
@Configuration
public class SystemMigrationsConfig {

    @Bean(name = "systemSchemaMigrator", initMethod = "migrate")
    public SqlMigrationRunner systemSchemaMigrator(
            @Qualifier("systemDataSource") DataSource systemDataSource) {
        return new SqlMigrationRunner(
            systemDataSource,
            "classpath:db/migration/system",
            "system_schema_history",
            "SYSTEM DB"
        );
    }
}
