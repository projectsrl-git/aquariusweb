package com.aquarius.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Serve le pagine dell'assistente integrato (porting da CReaM).
 * La chat vera passa per {@link HelpApiController}.
 */
@Controller
@RequestMapping("/help")
public class HelpController {

    @GetMapping
    public String chat() {
        return "help/chat";
    }
}
