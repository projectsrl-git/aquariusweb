package com.aquarius.multitenancy;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bind di {@code aquarius.tenants.*} da application.properties.
 *
 * Es:
 *   aquarius.tenants.impresind.name=Impresind
 *   aquarius.tenants.impresind.url=jdbc:jtds:sqlserver://...
 *
 * Questi sono i tenant "statici" (configurati in fase di build/deploy).
 * In futuro la lista può essere sovrascritta/integrata da quella nel SYSTEM DB
 * (tabella tenants) — vedi {@code TenantService}.
 */
@Component
@ConfigurationProperties(prefix = "aquarius")
@Data
public class TenantsProperties {

    /** Lista tenant: key = id_tenant (slug), value = config. */
    private Map<String, TenantConfig> tenants = new LinkedHashMap<>();

    /** Tenant preselezionato nella combobox di login. */
    private String defaultTenant;

    @Data
    public static class TenantConfig {
        private String name;
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }
}
