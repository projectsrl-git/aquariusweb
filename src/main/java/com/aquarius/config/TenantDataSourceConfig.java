package com.aquarius.config;

import com.aquarius.multitenancy.TenantRoutingDataSource;
import com.aquarius.multitenancy.TenantsProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * EntityManager #2: TENANT DBs.
 *
 * Costruisce un {@link TenantRoutingDataSource} che, ad ogni connessione,
 * sceglie il connection pool del tenant indicato dal {@code TenantContext}.
 *
 * I target sono creati al bootstrap leggendo {@link TenantsProperties}
 * (cioè {@code aquarius.tenants.*} in application.properties).
 *
 * Entity in {@code com.aquarius.entity.tenant}, repository in
 * {@code com.aquarius.repository.tenant}.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.aquarius.repository.tenant",
    entityManagerFactoryRef = "tenantEntityManagerFactory",
    transactionManagerRef   = "tenantTransactionManager"
)
@RequiredArgsConstructor
@Slf4j
public class TenantDataSourceConfig {

    private final TenantsProperties tenantsProperties;

    @Value("${aquarius.tenant.jpa.database-platform:org.hibernate.dialect.SQLServer2012Dialect}")
    private String defaultDialect;

    @Value("${aquarius.tenant.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    @Value("${aquarius.tenant.jpa.show-sql:false}")
    private boolean showSql;

    @Bean(name = "tenantDataSource")
    public DataSource tenantDataSource() {
        TenantRoutingDataSource routing = new TenantRoutingDataSource();

        Map<Object, Object> targets = new HashMap<>();
        DataSource defaultDs = null;

        for (Map.Entry<String, TenantsProperties.TenantConfig> e : tenantsProperties.getTenants().entrySet()) {
            String id = e.getKey();
            TenantsProperties.TenantConfig cfg = e.getValue();

            HikariConfig hc = new HikariConfig();
            hc.setJdbcUrl(cfg.getUrl());
            hc.setUsername(cfg.getUsername());
            hc.setPassword(cfg.getPassword());
            if (cfg.getDriverClassName() != null && !cfg.getDriverClassName().isBlank()) {
                hc.setDriverClassName(cfg.getDriverClassName());
            }
            hc.setPoolName("hikari-tenant-" + id);
            hc.setMaximumPoolSize(10);
            hc.setMinimumIdle(0);                    // niente connessioni mantenute aperte
            hc.setConnectionTimeout(5_000);          // fail-fast a 5s invece di 30s default
            // ★ Best-effort all'avvio: NON tentare validazione iniziale del pool.
            //   Se il DB del tenant è irraggiungibile, il bean si crea ugualmente.
            //   Le query future falliranno alla getConnection() del momento, ma
            //   gli altri tenant continuano a funzionare.
            hc.setInitializationFailTimeout(-1);
            // SQL Server via jTDS preferisce una query di test esplicita rispetto a isValid()
            hc.setConnectionTestQuery("SELECT 1");

            HikariDataSource ds;
            try {
                ds = new HikariDataSource(hc);
            } catch (Exception ex) {
                // initializationFailTimeout=-1 dovrebbe evitare questa eccezione,
                // ma per sicurezza catturiamo tutto e proseguiamo col prossimo tenant.
                log.error("Impossibile inizializzare pool per tenant '{}' (url={}): {}. " +
                          "Tenant escluso dal routing.", id, cfg.getUrl(), ex.getMessage());
                continue;
            }
            targets.put(id, ds);
            log.info("Configurato tenant DataSource id='{}' name='{}' url='{}'",
                     id, cfg.getName(), cfg.getUrl());

            if (id.equals(tenantsProperties.getDefaultTenant())) {
                defaultDs = ds;
            }
        }

        if (targets.isEmpty()) {
            throw new IllegalStateException(
                "Nessun tenant configurato. Definire almeno aquarius.tenants.<id>.* in application.properties");
        }

        routing.setTargetDataSources(targets);
        if (defaultDs != null) {
            routing.setDefaultTargetDataSource(defaultDs);
        } else {
            // Se aquarius.default-tenant non è valorizzato, usa il primo configurato
            routing.setDefaultTargetDataSource(targets.values().iterator().next());
        }
        routing.afterPropertiesSet();
        return routing;
    }

    @Bean(name = "tenantEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            @Qualifier("tenantDataSource") DataSource ds) {

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(ds);
        emf.setPackagesToScan("com.aquarius.entity.tenant");
        emf.setPersistenceUnitName("tenant");

        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", defaultDialect);
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        props.put("hibernate.show_sql", showSql);
        props.put("hibernate.format_sql", true);
        // Importante: ogni connection request passa dal routing,
        // così la connection è del tenant corrente.
        props.put("hibernate.physical_naming_strategy",
                  "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy");
        emf.setJpaPropertyMap(props);

        return emf;
    }

    @Bean(name = "tenantTransactionManager")
    public PlatformTransactionManager tenantTransactionManager(
            @Qualifier("tenantEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
