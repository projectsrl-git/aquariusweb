package com.aquarius.controller;

import com.aquarius.service.AccountTreeService;
import com.aquarius.service.AccountTreeService.TreePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API JSON del piano dei conti. Alimenta il componente client-side
 * {@code aq-tree.js} nella pagina {@code /conti/tree}, ed è pensata per
 * essere riusata da altri consumatori futuri:
 * <ul>
 *   <li>il "conto picker" del form prima nota (lookup conto con typeahead)</li>
 *   <li>dashboard / report che necessitano della struttura del PDC</li>
 * </ul>
 *
 * <p>Il payload è già filtrato per anno contabile + società della sessione
 * (FiscalContext) — il client non deve passare nulla.</p>
 */
@RestController
@RequestMapping("/conti")
@RequiredArgsConstructor
public class AccountTreeApiController {

    private final AccountTreeService treeService;

    @GetMapping("/tree-data")
    public TreePayload treeData() {
        return treeService.buildTreePayload();
    }
}
