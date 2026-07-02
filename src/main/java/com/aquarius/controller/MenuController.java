package com.aquarius.controller;

import com.aquarius.dto.MenuNode;
import com.aquarius.security.AquariusPrincipal;
import com.aquarius.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint che serve l'albero del menu per l'operatore loggato.
 *
 * Il front-end (sidebar) chiama {@code GET /api/menu/tree} al caricamento
 * della pagina e renderizza l'albero JSON ricevuto.
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public List<MenuNode> tree(@AuthenticationPrincipal AquariusPrincipal principal) {
        return menuService.buildMenuTree(principal.getUsername());
    }
}
