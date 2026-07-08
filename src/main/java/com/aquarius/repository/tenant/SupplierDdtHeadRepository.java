package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.SupplierDdtHead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierDdtHeadRepository extends JpaRepository<SupplierDdtHead, String> {

    @Query("""
        SELECT d FROM SupplierDdtHead d
        WHERE d.societyCode = :soc AND d.fiscalYear = :anno
          AND (:q IS NULL OR :q = ''
               OR d.documentNumber          LIKE CONCAT('%', :q, '%')
               OR LOWER(d.supplierCode)     LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(d.supplierName)     LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(d.reference)        LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<SupplierDdtHead> search(@Param("soc") String soc, @Param("anno") String anno,
                                 @Param("q") String q, Pageable pageable);
}
