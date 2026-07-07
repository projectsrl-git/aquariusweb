package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.MovContabile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovContabileRepository extends JpaRepository<MovContabile, String> {


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


    /**
     * PRIMANOTA raggruppata — numeri di registrazione DISTINTI dell'anno,
     * paginati e ordinabili. La lista mostra una riga per registrazione.
     * L'ordinamento è sulla data/numero della testata (min per gruppo).
     */
    /**
     * PRIMANOTA raggruppata — testate (numeri registrazione distinti), paginate.
     * L'ordinamento è dato da :orderCol / :asc via CASE, così l'ORDER BY resta
     * su espressioni compatibili col GROUP BY (evita il Sort del Pageable, che
     * su query aggregate genererebbe ORDER BY su property non aggregate).
     *   orderCol: 1=registrationDate, 2=registrationNo, 3=documentNo
     */
    @Query(value = """
        SELECT m.registrationNo AS registrationNo,
               MAX(m.registrationDate) AS registrationDate,
               MAX(m.documentNo) AS documentNo,
               MAX(m.operationType) AS operationType,
               SUM(CASE WHEN m.movementType = 'D' THEN m.amount ELSE 0 END) AS totDare
        FROM MovContabile m
        WHERE m.societyCode = :soc AND m.fiscalYear = :anno
          AND (:q IS NULL OR :q = ''
               OR m.registrationNo LIKE CONCAT('%', :q, '%')
               OR LOWER(m.description) LIKE LOWER(CONCAT('%', :q, '%'))
               OR m.documentNo LIKE CONCAT('%', :q, '%'))
        GROUP BY m.registrationNo
        ORDER BY
          CASE WHEN :orderCol = 2 AND :asc = true  THEN m.registrationNo END ASC,
          CASE WHEN :orderCol = 2 AND :asc = false THEN m.registrationNo END DESC,
          CASE WHEN :orderCol = 3 AND :asc = true  THEN MAX(m.documentNo) END ASC,
          CASE WHEN :orderCol = 3 AND :asc = false THEN MAX(m.documentNo) END DESC,
          CASE WHEN :orderCol = 1 AND :asc = true  THEN MAX(m.registrationDate) END ASC,
          CASE WHEN :orderCol = 1 AND :asc = false THEN MAX(m.registrationDate) END DESC,
          MAX(m.registrationDate) DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT m.registrationNo)
        FROM MovContabile m
        WHERE m.societyCode = :soc AND m.fiscalYear = :anno
          AND (:q IS NULL OR :q = ''
               OR m.registrationNo LIKE CONCAT('%', :q, '%')
               OR LOWER(m.description) LIKE LOWER(CONCAT('%', :q, '%'))
               OR m.documentNo LIKE CONCAT('%', :q, '%'))
        """)
    Page<RegHead> searchRegistrationHeads(@Param("soc") String soc,
                                          @Param("anno") String anno,
                                          @Param("q") String q,
                                          @Param("orderCol") int orderCol,
                                          @Param("asc") boolean asc,
                                          Pageable pageable);

    /** Righe MOV_CONT di un insieme di registrazioni (per costruire le testate). */
    @Query("""
        SELECT m FROM MovContabile m
        WHERE m.societyCode = :soc AND m.fiscalYear = :anno
          AND m.registrationNo IN :nregs
        ORDER BY m.registrationDate DESC, m.registrationNo DESC, m.movementType DESC
        """)
    List<MovContabile> findRowsForRegistrations(@Param("soc") String soc,
                                                @Param("anno") String anno,
                                                @Param("nregs") List<String> nregs);

    /**
     * METRICHE — TOP conti cliente/fornitore per movimentato.
     * Cliente/fornitore si identificano dal TIPO CONTO: CONTI.CON_TIPOCO = 'C'
     * (clienti) / 'F' (fornitori) — NON da MOV_CCLI/MOV_CFOR, quasi sempre vuoti.
     * Join MovContabile↔Account sul codice conto (SQL Server ignora gli spazi
     * di coda nel confronto varchar, quindi il padding legacy non è un problema).
     */
    @Query("""
        SELECT m.account AS label,
               SUM(m.amount) AS total
        FROM MovContabile m, Account a
        WHERE m.societyCode = :soc AND m.fiscalYear = :anno
          AND a.societyCode = :soc AND a.fiscalYear = :anno
          AND a.code = m.account
          AND a.accountType IN ('C', 'F')
        GROUP BY m.account
        ORDER BY SUM(m.amount) DESC
        """)
    List<MetricRow> topCustomerAccounts(@Param("soc") String soc,
                                        @Param("anno") String anno,
                                        Pageable pageable);

    /** METRICHE — TOP tipi operazione per importo. */
    @Query("""
        SELECT m.operationType AS label,
               SUM(CASE WHEN m.movementType = 'D' THEN m.amount ELSE 0 END) AS total
        FROM MovContabile m
        WHERE m.societyCode = :soc AND m.fiscalYear = :anno
          AND m.operationType IS NOT NULL AND m.operationType <> ''
        GROUP BY m.operationType
        ORDER BY SUM(CASE WHEN m.movementType = 'D' THEN m.amount ELSE 0 END) DESC
        """)
    List<MetricRow> topOperationTypes(@Param("soc") String soc,
                                      @Param("anno") String anno,
                                      Pageable pageable);

    /** METRICHE — importo per periodo (mese, dai primi 7 char di yyyy/MM/dd). */
    @Query("""
        SELECT SUBSTRING(m.registrationDate, 1, 7) AS label,
               SUM(CASE WHEN m.movementType = 'D' THEN m.amount ELSE 0 END) AS total
        FROM MovContabile m
        WHERE m.societyCode = :soc AND m.fiscalYear = :anno
          AND m.registrationDate IS NOT NULL
        GROUP BY SUBSTRING(m.registrationDate, 1, 7)
        ORDER BY SUBSTRING(m.registrationDate, 1, 7) ASC
        """)
    List<MetricRow> amountByPeriod(@Param("soc") String soc,
                                   @Param("anno") String anno);

    /** Proiezione testata registrazione (lista primanota). */
    interface RegHead {
        String getRegistrationNo();
        String getRegistrationDate();
        String getDocumentNo();
        String getOperationType();
        java.math.BigDecimal getTotDare();
    }

    /** Proiezione riga metrica (label + totale). */
    interface MetricRow {
        String getLabel();
        java.math.BigDecimal getTotal();
    }

    /** Proiezione per la riga di bilancio. */
    interface BilancioRow {
        String getAccount();
        java.math.BigDecimal getTotDare();
        java.math.BigDecimal getTotAvere();
    }
}
