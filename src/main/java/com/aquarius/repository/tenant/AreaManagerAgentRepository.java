package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.AreaManagerAgent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AreaManagerAgentRepository extends JpaRepository<AreaManagerAgent, String> {

    @Query("""
        SELECT c FROM AreaManagerAgent c
        WHERE c.societyCode = :soc
          AND (:q IS NULL OR :q = ''
               OR LOWER(c.areaManagerCode) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.areaManagerName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.agentCode)       LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.agentName)       LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<AreaManagerAgent> search(@Param("soc") String soc, @Param("q") String q, Pageable pageable);
}
