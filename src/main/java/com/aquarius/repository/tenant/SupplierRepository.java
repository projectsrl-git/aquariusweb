package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, String> {

    /** Ricerca paginata su ragione sociale / codice / partita IVA. */
    @Query("""
        SELECT s FROM Supplier s
        WHERE (:q IS NULL OR :q = ''
               OR LOWER(s.businessName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(s.code)         LIKE LOWER(CONCAT('%', :q, '%'))
               OR s.vatNumber           LIKE CONCAT('%', :q, '%'))
        """)
    Page<Supplier> search(@Param("q") String q, Pageable pageable);
}
