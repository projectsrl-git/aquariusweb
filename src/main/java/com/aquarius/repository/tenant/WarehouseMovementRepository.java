package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.WarehouseMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarehouseMovementRepository extends JpaRepository<WarehouseMovement, String> {

    /**
     * Paginated search on the current society + fiscal year. Free text on
     * article code/description, document number, customer/supplier names.
     * No fixed ORDER BY: sorting comes from the Pageable (whitelisted).
     * Dates stay strings — never converted (dirty MOV_DTREGI on old rows).
     */
    @Query("""
        SELECT m FROM WarehouseMovement m
        WHERE m.societyCode = :soc AND m.fiscalYear = :anno
          AND (:q IS NULL OR :q = ''
               OR LOWER(m.articleCode)        LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(m.articleDescription) LIKE LOWER(CONCAT('%', :q, '%'))
               OR m.documentNumber            LIKE CONCAT('%', :q, '%')
               OR LOWER(m.supplierName)       LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(m.customerName)       LIKE LOWER(CONCAT('%', :q, '%'))
               OR m.top                       LIKE CONCAT('%', :q, '%'))
        """)
    Page<WarehouseMovement> search(@Param("soc") String soc, @Param("anno") String anno,
                                   @Param("q") String q, Pageable pageable);
}
