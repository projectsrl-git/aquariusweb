package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.OrderHead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderHeadRepository extends JpaRepository<OrderHead, String> {

    /**
     * Paginated search on the current society + fiscal year (legacy PUB_ANNO
     * behaviour). Free text on number / customer code / customer name /
     * reference. No fixed ORDER BY: sorting comes from the Pageable.
     */
    @Query("""
        SELECT o FROM OrderHead o
        WHERE o.societyCode = :soc AND o.fiscalYear = :anno
          AND (:q IS NULL OR :q = ''
               OR o.orderNumber              LIKE CONCAT('%', :q, '%')
               OR LOWER(o.customerCode)      LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(o.customerName)      LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(o.reference)         LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<OrderHead> search(@Param("soc") String soc, @Param("anno") String anno,
                           @Param("q") String q, Pageable pageable);
}
