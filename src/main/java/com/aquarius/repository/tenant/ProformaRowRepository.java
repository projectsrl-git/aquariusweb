package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.ProformaRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProformaRowRepository extends JpaRepository<ProformaRow, String> {

    /** Rows of one proforma via the hook key ({@code TT.TAGGANCIO = DD.DAGGANCIO}). */
    @Query("""
        SELECT r FROM ProformaRow r
        WHERE r.aggancio = :agg
        ORDER BY r.sequence ASC
        """)
    List<ProformaRow> findByAggancio(@Param("agg") String agg);
}
