package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.ProductionOrderRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductionOrderRefRepository extends JpaRepository<ProductionOrderRef, String> {

    @Query("""
        SELECT o FROM ProductionOrderRef o
        WHERE o.programId = :idprg
        ORDER BY o.orderDate ASC, o.orderNumber ASC
        """)
    List<ProductionOrderRef> findByProgram(@Param("idprg") String idprg);
}
