package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgentRepository extends JpaRepository<Agent, String> {

    @Query("""
        SELECT a FROM Agent a
        WHERE a.societyCode = :soc
          AND (:q IS NULL OR :q = ''
               OR LOWER(a.code)   LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.name)   LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.city)   LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Agent> search(@Param("soc") String soc, @Param("q") String q, Pageable pageable);
}
