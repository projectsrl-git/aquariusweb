package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.Bank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankRepository extends JpaRepository<Bank, String> {

    @Query("""
        SELECT b FROM Bank b
        WHERE b.societyCode = :soc
          AND (:q IS NULL OR :q = ''
               OR LOWER(b.code)  LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(b.name)  LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(b.city)  LIKE LOWER(CONCAT('%', :q, '%'))
               OR b.abi          LIKE CONCAT('%', :q, '%')
               OR LOWER(b.iban)  LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Bank> search(@Param("soc") String soc, @Param("q") String q, Pageable pageable);
}
