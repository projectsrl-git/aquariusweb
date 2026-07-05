package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.MovContabile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovContabileRepository extends JpaRepository<MovContabile, String> {

    /**
     * PRIMANOTA — elenco delle testate di registrazione (distinte per numero
     * registrazione) di un anno, con ricerca su numero/descrizione. Ogni testata
     * è rappresentata dalla prima riga del gruppo MOV_NREGIS.
     * Ordinamento per data registrazione (stringa yyyy/MM/dd → ordinabile).
     */
    @Query("""
        SELECT m FROM MovContabile m
        WHERE m.societyCode = :soc AND m.fiscalYear = :anno
          AND (:q IS NULL OR :q = ''
               OR m.registrationNo LIKE CONCAT('%', :q, '%')
               OR LOWER(m.description) LIKE LOWER(CONCAT('%', :q, '%'))
               OR m.documentNo LIKE CONCAT('%', :q, '%'))
        ORDER BY m.registrationDate DESC, m.registrationNo DESC
        """)
    Page<MovContabile> searchRegistrations(@Param("soc") String soc,
                                           @Param("anno") String anno,
                                           @Param("q") String q,
                                           Pageable pageable);

    /** PRIMANOTA — tutte le righe di una registrazione (Dare/Avere). */
    @Query("""
        SELECT m FROM MovContabile m
        WHERE m.societyCode = :soc AND m.fiscalYear = :anno
          AND m.registrationNo = :nreg
        ORDER BY m.movementType DESC, m.id ASC
        """)
    List<MovContabile> findRegistrationRows(@Param("soc") String soc,
                                            @Param("anno") String anno,
                                            @Param("nreg") String nreg);

    /**
     * STORICO CONTABILE (mastrino) — tutti i movimenti di un conto in un anno,
     * in ordine di data, per ricostruire lo scalare Dare/Avere/saldo.
     */
    @Query("""
        SELECT m FROM MovContabile m
        WHERE m.societyCode = :soc AND m.fiscalYear = :anno
          AND m.account = :conto
        ORDER BY m.registrationDate ASC, m.registrationNo ASC
        """)
    List<MovContabile> findLedger(@Param("soc") String soc,
                                  @Param("anno") String anno,
                                  @Param("conto") String conto);

    /**
     * BILANCIO — totali Dare e Avere aggregati per conto in un anno.
     * Restituisce righe [conto, totaleDare, totaleAvere].
     */
    @Query("""
        SELECT m.account AS account,
               SUM(CASE WHEN m.movementType = 'D' THEN m.amount ELSE 0 END) AS totDare,
               SUM(CASE WHEN m.movementType = 'A' THEN m.amount ELSE 0 END) AS totAvere
        FROM MovContabile m
        WHERE m.societyCode = :soc AND m.fiscalYear = :anno
        GROUP BY m.account
        ORDER BY m.account ASC
        """)
    List<BilancioRow> findBilancio(@Param("soc") String soc,
                                   @Param("anno") String anno);

    /** Proiezione per la riga di bilancio. */
    interface BilancioRow {
        String getAccount();
        java.math.BigDecimal getTotDare();
        java.math.BigDecimal getTotAvere();
    }
}
