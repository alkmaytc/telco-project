package com.telco.backend.controller;

import com.telco.backend.dto.AuthResponseDTO;
import com.telco.backend.dto.LoginRequestDTO;
import com.telco.backend.dto.RegisterRequestDTO;
import com.telco.backend.service.AuthService;
import jakarta.validation.Valid; // Validasyon tetikleyicisi import edildi kanka
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
// 🎯 MADDE 8 TEMİZLİĞİ: @CrossOrigin annotation'ını buradan kaldırdık, global SecurityConfig'den yöneteceğiz. ✅
public class AuthController {

    private final AuthService authService;

    // 1. KAYIT OLMA UÇ NOKTASI (POST /api/v1/auth/register)
    @PostMapping("/register")
    // 🎯 MADDE 10 DOĞRULAMA: @Valid enjekte ederek RegisterRequestDTO içindeki JSR-380 zırhını aktif ettik! ✅
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // 2. GİRİŞ YAPMA UÇ NOKTASI (POST /api/v1/auth/login)
    @PostMapping("/login")
    // 🎯 Login taleplerinin de boş string gitmesini engellemek için validasyon bağladık kanka ✅
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }
}