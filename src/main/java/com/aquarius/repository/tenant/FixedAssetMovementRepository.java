package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.FixedAssetMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FixedAssetMovementRepository extends JpaRepository<FixedAssetMovement, String> {

    @Query("""
        SELECT m FROM FixedAssetMovement m
        WHERE m.assetCode = :code
        ORDER BY m.sequence ASC
        """)
    List<FixedAssetMovement> findByAsset(@Param("code") String code);
}
