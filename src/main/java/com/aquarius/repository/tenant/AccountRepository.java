package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Proiezione leggera per la costruzione dell'albero: solo le 7 colonne
     * necessarie invece delle ~40 mappate sull'entity. Su 7000 record la
     * differenza è sostanziale (niente hydration completa Hibernate).
     */
    interface TreeRow {
        String getId();
        String getCode();
        String getDescription();
        String getAccountType();
        String getBalancePosition();
        Boolean getIsParent();
        String getParentCode();
    }

    @Query("""
        SELECT a.id AS id, a.code AS code, a.description AS description,
               a.accountType AS accountType, a.balancePosition AS balancePosition,
               a.isParent AS isParent, a.parentCode AS parentCode
        FROM Account a
        WHERE a.fiscalYear = :year
          AND a.societyCode = :society
        ORDER BY a.code ASC
        """)
    List<TreeRow> findTreeRowsByYearAndSociety(@Param("year") String year,
                                                @Param("society") String society);

    /**
     * Legge la configurazione della struttura del piano dei conti dalla
     * anagrafica azienda {@code U_AZI_AN} — la stessa fonte usata dal VFP
     * per popolare le variabili globali {@code PUB_MASTRO} e {@code PUB_SOTTOG}
     * (vedi APPLILIB: {@code PUB_MASTRO = AZI_MASTRO + 1}).
     *
     * @return righe [AZI_MASTRO, AZI_SOTTOG] come Number, vuoto se assente.
     */
    @Query(value = "SELECT AZI_MASTRO, AZI_SOTTOG FROM U_AZI_AN WHERE AZI_CODSOC = :society",
           nativeQuery = true)
    List<Object[]> findPdcStructureRaw(@Param("society") String society);

    // ── Variante "default" (no filtering) — sconsigliata in produzione ──
    // Usata solo per debug/diagnostica. Per le pagine reali usare i metodi
    // ByYearAndSociety filtrati per anno contabile + società corrente.

    /**
     * Tutti i conti dell'anno+società indicati. Ordinati per code.
     */
    @Query("""
        SELECT a FROM Account a
        WHERE a.fiscalYear = :year
          AND a.societyCode = :society
        ORDER BY a.code ASC
        """)
    List<Account> findAllForTreeByYearAndSociety(@Param("year") String year,
                                                  @Param("society") String society);

    /**
     * Lista paginata con search filtrata per anno+società. Cerca su codice,
     * descrizione e partita IVA.
     */
    @Query("""
        SELECT a FROM Account a
        WHERE a.fiscalYear = :year
          AND a.societyCode = :society
          AND (:q IS NULL OR :q = ''
               OR LOWER(a.code) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))
               OR a.vatNumber LIKE CONCAT('%', :q, '%'))
        ORDER BY a.code ASC
        """)
    Page<Account> searchByYearAndSociety(@Param("year") String year,
                                          @Param("society") String society,
                                          @Param("q") String q,
                                          Pageable pageable);

    /** Conti figli diretti per anno+società (per lazy expand del tree). */
    @Query("""
        SELECT a FROM Account a
        WHERE a.fiscalYear = :year
          AND a.societyCode = :society
          AND a.parentCode = :parentCode
        ORDER BY a.code ASC
        """)
    List<Account> findByParentCodeByYearAndSociety(@Param("year") String year,
                                                    @Param("society") String society,
                                                    @Param("parentCode") String parentCode);

    /** Conti root per anno+società. */
    @Query("""
        SELECT a FROM Account a
        WHERE a.fiscalYear = :year
          AND a.societyCode = :society
          AND (a.parentCode IS NULL OR a.parentCode = '')
        ORDER BY a.code ASC
        """)
    List<Account> findRootsByYearAndSociety(@Param("year") String year,
                                             @Param("society") String society);

    // ── Metodi legacy (no filter) — mantenuti per backward compatibility ─

    /**
     * @deprecated usa {@link #findAllForTreeByYearAndSociety}
     */
    @Deprecated
    @Query("SELECT a FROM Account a ORDER BY a.code ASC")
    List<Account> findAllForTree();

    /**
     * @deprecated usa {@link #searchByYearAndSociety}
     */
    @Deprecated
    @Query("""
        SELECT a FROM Account a
        WHERE :q IS NULL OR :q = ''
           OR LOWER(a.code) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))
           OR a.vatNumber LIKE CONCAT('%', :q, '%')
        ORDER BY a.code ASC
        """)
    Page<Account> search(@Param("q") String q, Pageable pageable);

    /** Lookup per codice business (CON_CONTO). */
    Optional<Account> findFirstByCode(String code);

    /** Conti per tipo (C=Clienti, F=Fornitori, ecc.) — utile per dropdown. */
    @Query("""
        SELECT a FROM Account a
        WHERE a.accountType = :type
          AND a.fiscalYear = :year
          AND a.societyCode = :society
          AND (a.isParent IS NULL OR a.isParent = false)
        ORDER BY a.code ASC
        """)
    List<Account> findLeavesByTypeAndYear(@Param("type") String type,
                                           @Param("year") String year,
                                           @Param("society") String society);
}
