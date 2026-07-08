package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.SupplierOrderRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierOrderRowRepository extends JpaRepository<SupplierOrderRow, String> {

    @Query("""
        SELECT r FROM SupplierOrderRow r
        WHERE r.aggancio = :agg
        ORDER BY r.sequence ASC
        """)
    List<SupplierOrderRow> findByAggancio(@Param("agg") String agg);
}
