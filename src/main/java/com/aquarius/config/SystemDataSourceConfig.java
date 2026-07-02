package com.aquarius.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * EntityManager #1: SYSTEM DB.
 *
 * Contiene metadata multi-tenant: tabella {@code tenants} (lista società)
 * e {@code super_admins} (account globali per gestire i tenant).
 * Entity in package {@code com.aquarius.entity.system}, repository in
 * {@code com.aquarius.repository.system}.
 *
 * In dev: H2 in-memory. In prod: spostare su SQL Server / Postgres.
 *
 * Annotato {@code @Primary} perché è il datasource "di default" se nessun
 * tenant è ancora stato selezionato (es. lookup della lista tenant prima del login).
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.aquarius.repository.system",
    entityManagerFactoryRef = "systemEntityManagerFactory",
    transactionManagerRef   = "systemTransactionManager"
)
public class SystemDataSourceConfig {

    @Bean(name = "systemDataSourceProperties")
    @Primary
    @ConfigurationProperties("aquarius.system.datasource")
    public DataSourceProperties systemDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "systemDataSource")
    @Primary
    public DataSource systemDataSource(
            @Qualifier("systemDataSourceProperties") DataSourceProperties props) {
        // initializeDataSourceBuilder() converte automaticamente "url" → setJdbcUrl()
        // sulle implementazioni che lo richiedono (HikariCP).
        return props.initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
    }

    @Bean(name = "systemEntityManagerFactory")
    @Primary
    @org.springframework.context.annotation.DependsOn("systemSchemaMigrator")
    public LocalContainerEntityManagerFactoryBean systemEntityManagerFactory(
            @Qualifier("systemDataSource") DataSource ds) {

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(ds);
        emf.setPackagesToScan("com.aquarius.entity.system");
        emf.setPersistenceUnitName("system");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        // "none": Flyway è la sola autorità sullo schema del SYSTEM DB
        // (vedi SystemFlywayConfig). Hibernate non crea, non aggiorna, non valida.
        props.put("hibernate.hbm2ddl.auto", "none");
        props.put("hibernate.physical_naming_strategy",
                  "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy");
        emf.setJpaPropertyMap(props);

        return emf;
    }

    @Bean(name = "systemTransactionManager")
    @Primary
    public PlatformTransactionManager systemTransactionManager(
            @Qualifier("systemEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
