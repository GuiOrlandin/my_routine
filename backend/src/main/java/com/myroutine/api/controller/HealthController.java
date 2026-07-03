package com.myroutine.api.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
//STATELESS - não armazena estado na sessão
//STATEFUL - armazena estado na sessão
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
        
    }
}
