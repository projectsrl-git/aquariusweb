package com.aquarius.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Singolo tab nel pattern "form-shell" (vertical tabs riusabile per i form
 * di entity in stile Aquarius).
 *
 * <p>Costruito dal controller, consumato dal fragment {@code fragments/form-shell}.</p>
 *
 * <ul>
 *   <li>{@code placeholder = true}: il tab è mostrato come "da migrare" in una
 *       sezione separata della sidebar; il contenuto è un placeholder.</li>
 *   <li>{@code active = true}: questo tab è quello selezionato di default
 *       all'apertura del form (esattamente uno per form).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTab {

    /** ID DOM senza '#' (es. "tab-anag"). Usato per data-bs-target. */
    private String id;

    /** Etichetta del tab nella sidebar (es. "Anagrafica"). */
    private String label;

    /** Classe Bootstrap Icons (es. "bi-card-text"). */
    private String icon;

    @Builder.Default
    private boolean active = false;

    @Builder.Default
    private boolean placeholder = false;
}
