package com.aquarius;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Entry point dell'applicazione web AquariusWeb.
 *
 * Le auto-configurazioni escluse:
 *
 *  - {@link DataSourceAutoConfiguration} + {@link HibernateJpaAutoConfiguration}:
 *    definiamo manualmente DUE EntityManager (system, tenant routing) nei
 *    rispettivi @Configuration class.
 *
 * Le migrazioni di schema sono gestite da un mini-migratore custom
 * (vedi {@code config/SqlMigrationRunner}), NON Flyway. Motivo: Flyway
 * Community (da 7.0 in poi) ha rimosso il supporto a SQL Server 2008/2012,
 * che è quello che il cliente ha in produzione.
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@org.springframework.cache.annotation.EnableCaching
public class AquariusApplication {

    public static void main(String[] args) {
        SpringApplication.run(AquariusApplication.class, args);
    }
}
