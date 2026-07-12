package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.FixedAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FixedAssetRepository extends JpaRepository<FixedAsset, String> {

    /**
     * stato: '' = tutti, 'attivi' = senza data cessione, 'ceduti' = con data.
     * L'archivio non ha la dimensione societa' (verificato nei sorgenti).
     */
    @Query("""
        SELECT a FROM FixedAsset a
        WHERE (:cat = '' OR a.categoryCode = :cat)
          AND (:stato = ''
               OR (:stato = 'attivi' AND (a.disposalDate IS NULL OR TRIM(a.disposalDate) = ''))
               OR (:stato = 'ceduti' AND a.disposalDate IS NOT NULL AND TRIM(a.disposalDate) <> ''))
          AND (:q = ''
               OR LOWER(a.code)          LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.description)   LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.serialNumber)  LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<FixedAsset> search(@Param("q") String q, @Param("cat") String cat,
                            @Param("stato") String stato, Pageable pageable);

    /**
     * Riepilogo per categoria: numero cespiti e somme di valore storico,
     * ammortizzato e residuo (somme semplici delle colonne anagrafiche,
     * nessuna logica fiscale). Righe: [categoryCode, count, sumValsto,
     * sumTotamm, sumValres].
     */
    @Query("""
        SELECT a.categoryCode, COUNT(a),
               COALESCE(SUM(a.historicalValue), 0),
               COALESCE(SUM(a.totalDepreciated), 0),
               COALESCE(SUM(a.residualValue), 0)
        FROM FixedAsset a
        GROUP BY a.categoryCode
        """)
    java.util.List<Object[]> summaryByCategory();
}
