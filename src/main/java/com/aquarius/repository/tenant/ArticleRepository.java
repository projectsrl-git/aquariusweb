package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, String> {

    /**
     * Paginated free-text search on code / description / barcode / supplier
     * code. No fixed ORDER BY: sorting comes from the Pageable (whitelisted
     * in the controller via ListParams).
     */
    @Query("""
        SELECT a FROM Article a
        WHERE (:q IS NULL OR :q = ''
               OR LOWER(a.code)                LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.description)         LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.extendedDescription) LIKE LOWER(CONCAT('%', :q, '%'))
               OR a.barcode                    LIKE CONCAT('%', :q, '%')
               OR LOWER(a.supplierCode)        LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Article> search(@Param("q") String q, Pageable pageable);
}
