package com.aquarius.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodo dell'albero di menu rendering-friendly.
 *
 * Discende ricorsivamente: {@code children} contiene i sotto-nodi.
 * I separator (label "\-") sono serializzati come nodi con {@code separator=true}
 * — il frontend li renderizza come divider.
 *
 * I form ancora non portati hanno {@code formName} valorizzato ma {@code formUrl}
 * a null (il frontend li mostra come "(da migrare)" non cliccabili).
 */
@Data
@Builder
public class MenuNode {

    /** Etichetta visualizzata nel menu. */
    private String label;

    /** Icona Bootstrap Icons (es. "bi-people"). */
    private String icon;

    /** Se !=null e non vuoto → voce cliccabile che porta a quest'URL. */
    private String url;

    /** Nome del form VFP originale (per debug / "non implementato"). */
    private String formName;

    /** True se la voce è solo un separator visivo. */
    @Builder.Default
    private boolean separator = false;

    /** Sotto-voci (può essere null o vuoto per le foglie). */
    @Builder.Default
    private List<MenuNode> children = new ArrayList<>();

    /** True se questo nodo (o un suo discendente) ha almeno una voce cliccabile.
     *  Usato per nascondere rami completamente "non migrati". */
    private boolean hasReachableLeaf;
}
