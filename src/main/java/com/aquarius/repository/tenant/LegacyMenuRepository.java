package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.LegacyMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LegacyMenuRepository extends JpaRepository<LegacyMenu, String> {

    /**
     * Voci di TOP-LEVEL (menubar): "Clienti", "Fornitori", ecc.
     * In Impresind ce ne sono ~10. Nessun filtro UTENTI: i top-level sono
     * container, non foglie.
     */
    @Query("""
        SELECT m FROM LegacyMenu m
        WHERE m.livelloMenu = 1
        ORDER BY m.ordine ASC
        """)
    List<LegacyMenu> findAllTopLevel();

    /**
     * Voci di SECONDO LIVELLO (popup): voci nei popup dei top-level.
     * Nessun filtro UTENTI: questi sono anch'essi container o foglie di
     * comando diretto, l'autorizzazione è sulle foglie L0.
     * Tipicamente ~200 record in totale.
     */
    @Query("""
        SELECT m FROM LegacyMenu m
        WHERE m.livelloMenu = 2
        ORDER BY m.ordine ASC
        """)
    List<LegacyMenu> findAllSecondLevel();

    /**
     * Voci FOGLIA (LIVELLO_MENU=0) visibili allo specifico operatore.
     * Filtro UTENTI a DB-side: solo le righe in cui il codice operatore è
     * presente nella stringa UTENTI (formato ".CODE1.CODE2....").
     */
    @Query("""
        SELECT m FROM LegacyMenu m
        WHERE m.livelloMenu = 0
        AND m.utenti LIKE CONCAT('%.', :operatorCode, '.%')
        ORDER BY m.menu ASC, m.ordine ASC
        """)
    List<LegacyMenu> findVisibleLeavesFor(@Param("operatorCode") String operatorCode);
}
