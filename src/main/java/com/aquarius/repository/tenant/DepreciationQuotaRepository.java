package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.DepreciationQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DepreciationQuotaRepository extends JpaRepository<DepreciationQuota, String> {

    @Query("""
        SELECT q FROM DepreciationQuota q
        WHERE q.assetCode = :code
        ORDER BY q.year ASC
        """)
    List<DepreciationQuota> findByAsset(@Param("code") String code);
}
