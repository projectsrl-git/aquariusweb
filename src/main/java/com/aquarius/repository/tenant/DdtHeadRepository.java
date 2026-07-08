package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.DdtHead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DdtHeadRepository extends JpaRepository<DdtHead, String> {

    /**
     * Paginated search on the current society + fiscal year. Free text on
     * DDT number / customer code / customer name / reference / carrier.
     * No fixed ORDER BY: sorting comes from the Pageable.
     */
    @Query("""
        SELECT d FROM DdtHead d
        WHERE d.societyCode = :soc AND d.fiscalYear = :anno
          AND (:q IS NULL OR :q = ''
               OR d.ddtNumber              LIKE CONCAT('%', :q, '%')
               OR LOWER(d.customerCode)    LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(d.customerName)    LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(d.reference)       LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(d.carrier)         LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<DdtHead> search(@Param("soc") String soc, @Param("anno") String anno,
                         @Param("q") String q, Pageable pageable);
}
