package com.aquarius.entity.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Associazione capo area ↔ agente — read-only mapping of {@code U_CAR_AN}.
 * VERIFIED in the sources: MENU_CAR000 caption is "Gestione agenti per capo
 * area" (labels: codice/nome capo area, codice/nome agente, % provvigione
 * "calcolata su imponibile del venduto"). NOT vettori/corrieri despite the
 * table name — the handoff's guess is corrected here; vettori are the PARA
 * 'VET' category (già in /parametri/VET).
 */
@Entity
@Table(name = "U_CAR_AN")
@DynamicUpdate
@Data
public class AreaManagerAgent {

    @Id
    @Column(name = "id_unique", columnDefinition = "uniqueidentifier",
            insertable = false, updatable = false)
    private String id;

    @Column(name = "PRV_CODSOC", length = 2, insertable = false, updatable = false)
    private String societyCode;

    @Column(name = "PRV_CODCAR", length = 3, insertable = false, updatable = false)
    private String areaManagerCode;

    @Column(name = "PRV_DESCAR", length = 35, insertable = false, updatable = false)
    private String areaManagerName;

    @Column(name = "PRV_CODAGE", length = 3, insertable = false, updatable = false)
    private String agentCode;

    @Column(name = "PRV_DESAGE", length = 35, insertable = false, updatable = false)
    private String agentName;

    /** % provvigione del capo area sull'imponibile del venduto dell'agente. */
    @Column(name = "PRV_PERPRO", insertable = false, updatable = false)
    private BigDecimal commissionPct;
}
