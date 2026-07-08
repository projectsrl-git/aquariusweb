package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.DdtRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DdtRowRepository extends JpaRepository<DdtRow, String> {

    /** Rows of one DDT via the hook key ({@code TT.TAGGANCIO = DD.DAGGANCIO}). */
    @Query("""
        SELECT r FROM DdtRow r
        WHERE r.aggancio = :agg
        ORDER BY r.sequence ASC
        """)
    List<DdtRow> findByAggancio(@Param("agg") String agg);
}
