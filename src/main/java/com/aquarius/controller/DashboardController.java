package com.aquarius.controller;

import com.aquarius.security.AquariusPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AquariusPrincipal principal, Model model) {
        model.addAttribute("principal", principal);
        return "dashboard";
    }
}
