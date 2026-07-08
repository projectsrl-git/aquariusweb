package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.InvoiceRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRowRepository extends JpaRepository<InvoiceRow, String> {

    /** Rows of one invoice via the hook key ({@code TT.TAGGANCIO = DD.DAGGANCIO}). */
    @Query("""
        SELECT r FROM InvoiceRow r
        WHERE r.aggancio = :agg
        ORDER BY r.sequence ASC
        """)
    List<InvoiceRow> findByAggancio(@Param("agg") String agg);
}
