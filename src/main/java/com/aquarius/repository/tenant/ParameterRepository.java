package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.ParameterItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParameterRepository extends JpaRepository<ParameterItem, String> {

    /**
     * Tutti i parametri di una categoria, ordinati per codice. Il filtro è
     * sul prefisso del CODICE.
     */
    @Query("""
        SELECT p FROM ParameterItem p
        WHERE p.codice LIKE CONCAT(:prefix, '%')
        ORDER BY p.codice ASC
        """)
    List<ParameterItem> findByPrefix(@Param("prefix") String prefix);

    /**
     * Variante paginata + searchable. Filtra per prefisso e poi sulla
     * descrizione (case-insensitive). Per categorie con migliaia di parametri
     * (es. anni contabili, conti).
     */
    @Query("""
        SELECT p FROM ParameterItem p
        WHERE p.codice LIKE CONCAT(:prefix, '%')
          AND (:q IS NULL OR :q = ''
               OR LOWER(p.descri) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.codice) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY p.codice ASC
        """)
    Page<ParameterItem> searchByPrefix(@Param("prefix") String prefix,
                                       @Param("q") String q,
                                       Pageable pageable);

    /** Count rapido per la overview (mostra # parametri per categoria). */
    @Query("SELECT COUNT(p) FROM ParameterItem p WHERE p.codice LIKE CONCAT(:prefix, '%')")
    long countByPrefix(@Param("prefix") String prefix);
}
