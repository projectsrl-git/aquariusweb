package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.ProductionComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductionComponentRepository extends JpaRepository<ProductionComponent, String> {

    @Query("""
        SELECT c FROM ProductionComponent c
        WHERE c.programId = :idprg
        ORDER BY c.sequence ASC
        """)
    List<ProductionComponent> findByProgram(@Param("idprg") String idprg);
}
