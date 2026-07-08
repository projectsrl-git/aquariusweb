package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.ProformaHead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProformaHeadRepository extends JpaRepository<ProformaHead, String> {

    /**
     * Paginated search on the current society + fiscal year. Free text on
     * invoice number / customer code / name / VAT number / reference.
     * No fixed ORDER BY: sorting comes from the Pageable.
     */
    @Query("""
        SELECT i FROM ProformaHead i
        WHERE i.societyCode = :soc AND i.fiscalYear = :anno
          AND (:q IS NULL OR :q = ''
               OR i.invoiceNumber           LIKE CONCAT('%', :q, '%')
               OR LOWER(i.customerCode)     LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(i.customerName)     LIKE LOWER(CONCAT('%', :q, '%'))
               OR i.vatNumber               LIKE CONCAT('%', :q, '%')
               OR LOWER(i.reference)        LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<ProformaHead> search(@Param("soc") String soc, @Param("anno") String anno,
                             @Param("q") String q, Pageable pageable);
}
