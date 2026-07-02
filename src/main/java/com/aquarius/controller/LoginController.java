package com.aquarius.controller;

import com.aquarius.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


/**
 * Serve la pagina di login con i tre campi: società, operatore, password.
 *
 * Il POST /login è gestito dal {@code TenantAwareAuthenticationFilter}
 * (configurato in {@code SecurityConfig}), non da un controller.
 */
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final TenantService tenantService;

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("tenants", tenantService.listEnabled());
        model.addAttribute("defaultTenantId", tenantService.getDefaultTenantId());
        return "login";
    }
}
