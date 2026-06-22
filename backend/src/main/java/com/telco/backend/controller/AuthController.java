package com.telco.backend.controller;

import com.telco.backend.dto.AuthResponseDTO;
import com.telco.backend.dto.LoginRequestDTO;
import com.telco.backend.dto.RegisterRequestDTO;
import com.telco.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // React ile haberleşirken CORS hatası almamak için
public class AuthController {

    private final AuthService authService;

    // 1. KAYIT OLMA UÇ NOKTASI (POST /api/v1/auth/register)
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // 2. GİRİŞ YAPMA UÇ NOKTASI (POST /api/v1/auth/login)
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }
}