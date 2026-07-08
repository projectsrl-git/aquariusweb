package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.OrderRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRowRepository extends JpaRepository<OrderRow, String> {

    /**
     * Rows of one order via the hook key (legacy SQL joins
     * {@code TT.TAGGANCIO = DD.DAGGANCIO}). Primary lookup.
     */
    @Query("""
        SELECT r FROM OrderRow r
        WHERE r.aggancio = :agg
        ORDER BY r.sequence ASC
        """)
    List<OrderRow> findByAggancio(@Param("agg") String agg);

    /**
     * Fallback for records with empty TAGGANCIO. Legacy TT↔DD link (ristampelib):
     * ORS_DATORD = ORD_DATORD AND ORS_NUMORD = ORD_NUMORD AND ORS_CODCLI = ORD_CODCLI.
     * SQL Server ignores trailing spaces on varchar '=', so legacy padding is
     * a non-issue; params are bound with the header's stored values as-is.
     */
    @Query("""
        SELECT r FROM OrderRow r
        WHERE r.orderDate = :datord AND r.orderNumber = :numord
          AND r.customerCode = :codcli
        ORDER BY r.sequence ASC
        """)
    List<OrderRow> findRows(@Param("datord") String datord,
                            @Param("numord") String numord,
                            @Param("codcli") String codcli);
}
