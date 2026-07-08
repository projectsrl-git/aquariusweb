package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.ProductionProgram;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionProgramRepository extends JpaRepository<ProductionProgram, String> {

    /**
     * Programmi di produzione STANDARD: {@code PARENT = '' AND TIPO = 'STD'}
     * — the verified legacy filter of STD_PROGRAMMAZIONE (root nodes of the
     * program tree). SQL Server '=' ignores trailing spaces, so the empty
     * PARENT comparison matches space-padded values as in the legacy.
     * No fixed ORDER BY: sorting comes from the Pageable (whitelisted).
     */
    @Query("""
        SELECT p FROM ProductionProgram p
        WHERE p.parent = '' AND p.type = 'STD'
          AND (:q IS NULL OR :q = ''
               OR p.programNumber              LIKE CONCAT('%', :q, '%')
               OR LOWER(p.articleCode)         LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.articleDescription)  LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<ProductionProgram> searchStandard(@Param("q") String q, Pageable pageable);
}
