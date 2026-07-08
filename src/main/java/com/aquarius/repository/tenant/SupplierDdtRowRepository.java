package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.SupplierDdtRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierDdtRowRepository extends JpaRepository<SupplierDdtRow, String> {

    @Query("""
        SELECT r FROM SupplierDdtRow r
        WHERE r.aggancio = :agg
        ORDER BY r.sequence ASC
        """)
    List<SupplierDdtRow> findByAggancio(@Param("agg") String agg);
}
