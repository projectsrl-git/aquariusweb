package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.FixedAssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FixedAssetCategoryRepository extends JpaRepository<FixedAssetCategory, String> {

    @Query("SELECT c FROM FixedAssetCategory c ORDER BY c.code ASC")
    List<FixedAssetCategory> findAllOrdered();
}
