package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.BomRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BomRowRepository extends JpaRepository<BomRow, String> {

    /** First-level components of one BOM (TT.TAGGANCIO = DD.DAGGANCIO). */
    @Query("""
        SELECT r FROM BomRow r
        WHERE r.aggancio = :agg
        ORDER BY r.sequence ASC
        """)
    List<BomRow> findByAggancio(@Param("agg") String agg);
}
