package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.ProductionProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductionProgressRepository extends JpaRepository<ProductionProgress, String> {

    @Query("""
        SELECT a FROM ProductionProgress a
        WHERE a.programId = :idprg
        ORDER BY a.phase ASC, a.sequence ASC
        """)
    List<ProductionProgress> findByProgram(@Param("idprg") String idprg);
}
