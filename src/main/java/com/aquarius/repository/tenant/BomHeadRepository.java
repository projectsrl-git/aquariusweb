package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.BomHead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BomHeadRepository extends JpaRepository<BomHead, String> {

    /**
     * Paginated search on parent article / description / customer.
     * No fixed ORDER BY: sorting comes from the Pageable (whitelisted).
     */
    @Query("""
        SELECT b FROM BomHead b
        WHERE b.societyCode = :soc
          AND (:q IS NULL OR :q = ''
               OR LOWER(b.parentArticleCode) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(b.description)       LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(b.customerName)      LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<BomHead> search(@Param("soc") String soc, @Param("q") String q,
                         Pageable pageable);

    /** Lookup by parent article code (legacy: dit_gruppo = code) for sub-BOM navigation. */
    @Query("""
        SELECT b FROM BomHead b
        WHERE b.societyCode = :soc AND b.parentArticleCode = :code
        """)
    List<BomHead> findByParentArticle(@Param("soc") String soc, @Param("code") String code);
}
