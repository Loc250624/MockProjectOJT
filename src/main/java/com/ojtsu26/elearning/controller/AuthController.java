package com.ojtsu26.elearning.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @GetMapping("/oauth-login")
    public String oauthLoginPage() {
        return "auth/oauth-login";
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login() {
        return ResponseEntity.ok(Map.of("message", "Login action placeholder - not yet implemented"));
    }

    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<?> register() {
        return ResponseEntity.ok(Map.of("message", "Register action placeholder - not yet implemented"));
    }
}
