package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, String> {

    /** Ricerca paginata su ragione sociale / codice / partita IVA. */
    @Query("""
        SELECT c FROM Customer c
        WHERE (:q IS NULL OR :q = ''
               OR LOWER(c.businessName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.code)         LIKE LOWER(CONCAT('%', :q, '%'))
               OR c.vatNumber           LIKE CONCAT('%', :q, '%'))
        """)
    Page<Customer> search(@Param("q") String q, Pageable pageable);

    /** Tutti i clienti che hanno città valorizzata — usato dalla mappa. */
    @Query("""
        SELECT c FROM Customer c
        WHERE c.city IS NOT NULL AND TRIM(c.city) <> ''
        ORDER BY c.businessName ASC
        """)
    List<Customer> findAllWithCity();
}
