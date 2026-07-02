package com.aquarius.service;

import com.aquarius.entity.system.Tenant;
import com.aquarius.multitenancy.TenantsProperties;
import com.aquarius.repository.system.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Espone la lista dei tenant abilitati al login.
 *
 * Sorgente: tabella {@code tenants} del SYSTEM DB. Al primo avvio la tabella è
 * vuota: il {@link com.aquarius.config.DataSeeder} la popola usando
 * {@link TenantsProperties} (cioè il blocco {@code aquarius.tenants.*} di
 * application.properties).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantsProperties tenantsProperties;

    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<Tenant> listEnabled() {
        return tenantRepository.findByEnabledTrueOrderByDisplayNameAsc();
    }

    public String getDefaultTenantId() {
        return tenantsProperties.getDefaultTenant();
    }
}
