package com.aquarius.entity.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Singola riga della tabella legacy {@code PARA} — il "tabellone parametri"
 * che alimenta combo box e lookup di tutto Aquarius.
 *
 * <p>Il {@code CODICE} ha sempre il formato {@code <PREFISSO><VALORE>} dove:
 * <ul>
 *   <li>PREFISSO (es. "TOP", "IVA", "CPA", "CVE") = identifica la categoria
 *       (vedi {@link com.aquarius.service.ParameterCategoryCatalog})</li>
 *   <li>VALORE = il codice business vero e proprio (es. "0001" per TOP,
 *       "001" per CPA)</li>
 * </ul>
 *
 * <p>La colonna {@code LIBERA} è varchar(250) usata in modo molto creativo
 * dal VFP: per la categoria TOP contiene 20+ flag posizionali
 * (SUBSTRING(LIBERA,9,1) = 'S' indica "stampa bollati IVA"), per altre categorie
 * è un campo libero. Per ora la trattiamo come stringa raw.</p>
 *
 * <p>{@code DISATTIVO} (bit) = 1 → parametro disabilitato (non appare nei
 * combo, ma resta in DB per integrità referenziale).</p>
 */
@Entity
@Table(name = "PARA")
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterItem {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    /** Codice completo (prefisso + valore). Es. "TOP0001", "IVA001". */
    @Column(name = "CODICE", length = 50, nullable = false)
    private String codice;

    @Column(name = "DESCRI", length = 200)
    private String descri;

    @Column(name = "LIBERA", length = 250)
    private String libera;

    /** 1 = disattivato, 0 = attivo. */
    @Column(name = "DISATTIVO", columnDefinition = "bit")
    private Boolean disattivo;
}
