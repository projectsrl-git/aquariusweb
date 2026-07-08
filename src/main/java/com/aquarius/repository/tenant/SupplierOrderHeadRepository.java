package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.SupplierOrderHead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierOrderHeadRepository extends JpaRepository<SupplierOrderHead, String> {

    @Query("""
        SELECT o FROM SupplierOrderHead o
        WHERE o.societyCode = :soc AND o.fiscalYear = :anno
          AND (:q IS NULL OR :q = ''
               OR o.orderNumber            LIKE CONCAT('%', :q, '%')
               OR LOWER(o.supplierCode)    LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(o.supplierName)    LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(o.reference)       LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<SupplierOrderHead> search(@Param("soc") String soc, @Param("anno") String anno,
                                   @Param("q") String q, Pageable pageable);
}
