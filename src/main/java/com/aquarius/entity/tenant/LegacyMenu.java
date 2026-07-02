package com.aquarius.entity.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Mappa la tabella legacy {@code tbl_menu} del DB tenant — READ ONLY.
 *
 * La tabella contiene 1.527 voci di menu (in Impresind production). Ogni riga
 * è una "voce" che vive sotto un menu parente identificato dalla colonna
 * {@code MENU}. La voce può essere:
 *
 *  - una foglia (esegue un form): {@code COMANDO = "do form form\<nome>"}
 *  - un sotto-menu: {@code COMANDO = "do form form\submenu NAME LIST# with 'subkey',...'}
 *    in tal caso la 'subkey' diventa il valore di MENU dei figli
 *
 * Autorizzazioni: la colonna UTENTI è una stringa di codici operatore
 * separati da punto: {@code ".AMB.SER.GEN.FAB."}. Voce visibile a un utente X
 * se la sua stringa contiene {@code .X.}.
 *
 * Strategia 1.3: ZERO modifiche a questa tabella, solo SELECT.
 */
@Entity
@Table(name = "tbl_menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegacyMenu {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "ORDINE", insertable = false, updatable = false)
    private Integer ordine;

    /** Chiave del menu parente. Es: "clienti", "contabilit", "gestioneprimanota". */
    @Column(name = "MENU", length = 50, insertable = false, updatable = false)
    private String menu;

    /** Testo visualizzato. Es: "Prima nota", "Anagrafica clienti". */
    @Column(name = "LABEL", length = 50, insertable = false, updatable = false)
    private String label;

    /** Comando VFP da eseguire: "do form form\xxx" oppure "do form form\submenu ...". */
    @Column(name = "COMANDO", length = 1000, insertable = false, updatable = false)
    private String comando;

    @Column(name = "DESCRIZION", length = 200, insertable = false, updatable = false)
    private String descrizione;

    /**
     * Lista codici operatore autorizzati, separati da punto.
     * Esempio: ".AMB.SER.GEN.FAB."
     */
    @Column(name = "UTENTI", length = 1000, insertable = false, updatable = false)
    private String utenti;

    @Column(name = "AUTORIZZAZIONE", insertable = false, updatable = false)
    private Boolean autorizzazione;

    @Column(name = "ICONA", length = 200, insertable = false, updatable = false)
    private String icona;

    /**
     * Livello strutturale del menu (vedi prg/WINMAIN_LIB.PRG / CREA_WINMAIN):
     *  - 1 = TOP-LEVEL (voce della menubar, es. "Clienti", "Fornitori"). MENU
     *        è il nome del popup associato.
     *  - 2 = SECONDO LIVELLO (voce nel popup di un top-level). MENU è il nome
     *        del popup di appartenenza (= MENU di una voce L1). Se COMANDO
     *        contiene "do form form\submenu ... with 'X'", apre un sub-submenu X.
     *  - 0 = FOGLIA (voce concreta nei sub-submenu). MENU è il nome del
     *        sub-submenu (es. "gestioneclienti"). UTENTI filtra l'accesso.
     */
    @Column(name = "LIVELLO_MENU", insertable = false, updatable = false)
    private Integer livelloMenu;

    @Column(name = "VISIBILE_SE", length = 200, insertable = false, updatable = false)
    private String visibileSe;

    /** True se la voce è visibile all'utente con codice {@code operatorCode}. */
    @Transient
    public boolean isVisibleTo(String operatorCode) {
        if (utenti == null || utenti.isBlank()) return false;
        if (operatorCode == null || operatorCode.isBlank()) return false;
        return utenti.contains("." + operatorCode.trim() + ".");
    }

    /** True se questa voce è un separator (label "\-"). */
    @Transient
    public boolean isSeparator() {
        return label != null && "\\-".equals(label.trim());
    }
}
