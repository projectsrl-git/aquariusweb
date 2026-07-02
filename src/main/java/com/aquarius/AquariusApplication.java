package com.aquarius;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;

/**
 * AquariusWeb entry point.
 *
 * <p>Packaged as a WAR and deployed under an external Tomcat 9
 * ({@link SpringBootServletInitializer} bootstraps the app when the servlet
 * container starts). The {@code main()} method is kept so that
 * {@code mvn spring-boot:run} still works for local development — the
 * embedded Tomcat is on the classpath with {@code provided} scope.</p>
 *
 * <p>Excluded auto-configurations:</p>
 * <ul>
 *   <li>{@link DataSourceAutoConfiguration} + {@link HibernateJpaAutoConfiguration}:
 *       we define TWO EntityManagers manually (system H2 + tenant routing
 *       SQL Server) in their respective {@code @Configuration} classes.</li>
 * </ul>
 *
 * <p>Schema migrations are handled by a small custom runner
 * (see {@code config/SqlMigrationRunner}), NOT Flyway: Flyway Community
 * dropped SQL Server 2008/2012 support from v7 onward, and 2012 is what
 * runs in production.</p>
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@EnableCaching
public class AquariusApplication extends SpringBootServletInitializer {

    /** WAR bootstrap: called by the external servlet container (Tomcat 9). */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AquariusApplication.class);
    }

    /** Dev bootstrap: {@code mvn spring-boot:run} (embedded Tomcat, provided scope). */
    public static void main(String[] args) {
        SpringApplication.run(AquariusApplication.class, args);
    }
}
