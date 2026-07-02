package com.aquarius.repository.system;

import com.aquarius.entity.system.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, String> {

    List<Tenant> findByEnabledTrueOrderByDisplayNameAsc();
}
