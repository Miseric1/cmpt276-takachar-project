package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }
}
