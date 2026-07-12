package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.PartitaFornitore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartitaFornitoreRepository extends JpaRepository<PartitaFornitore, String> {

    /** Elenco partite di un anno con ricerca su codice/ragione sociale/fattura. */
    @Query("""
        SELECT p FROM PartitaFornitore p
        WHERE p.societyCode = :soc AND p.fiscalYear = :anno
          AND (:q IS NULL OR :q = ''
               OR p.partyCode LIKE CONCAT('%', :q, '%')
               OR LOWER(p.partyName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR p.invoiceNo LIKE CONCAT('%', :q, '%'))
        """)
    Page<PartitaFornitore> search(@Param("soc") String soc,
                      @Param("anno") String anno,
                      @Param("q") String q,
                      Pageable pageable);

    /** Tutte le partite di una singola anagrafica in un anno (estratto conto). */
    @Query("""
        SELECT p FROM PartitaFornitore p
        WHERE p.societyCode = :soc AND p.fiscalYear = :anno
          AND p.partyCode = :code
        ORDER BY p.dueDate ASC
        """)
    List<PartitaFornitore> findByParty(@Param("soc") String soc,
                           @Param("anno") String anno,
                           @Param("code") String code);

    /** Partite APERTE (totale <> pagato) dell'anno, per lo scadenziario/aging. */
    @Query("""
        SELECT p FROM PartitaFornitore p
        WHERE p.societyCode = :soc AND p.fiscalYear = :anno
          AND COALESCE(p.totalAmount, 0) <> COALESCE(p.paidAmount, 0)
        ORDER BY p.partyName ASC, p.dueDate ASC
        """)
    List<PartitaFornitore> findAperte(@Param("soc") String soc,
                                      @Param("anno") String anno);
}
