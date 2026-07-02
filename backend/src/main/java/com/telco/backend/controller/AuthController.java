package com.telco.backend.controller;

import com.telco.backend.dto.AuthResponseDTO;
import com.telco.backend.dto.LoginRequestDTO;
import com.telco.backend.dto.RegisterRequestDTO;
import com.telco.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
// 🎯 DEĞİŞİKLİK BURADA: Tag ismini 2 numara yaptık. KİLİT YOK!
@Tag(name = "2. 🔐 Kimlik Doğrulama (Auth) API", description = "Kullanıcı kaydı, sisteme giriş ve JWT token üretim servisleri.")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Yeni Müşteri Kaydı (Register)", description = "Sisteme yeni bir müşteri kaydeder. T.C. Kimlik, E-posta ve Koordinat (PostGIS) doğrulamalarından geçirir.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kayıt başarılı, JWT Token üretildi"),
            @ApiResponse(responseCode = "400", description = "Validasyon hatası (Eksik/Hatalı veri formatı)"),
            @ApiResponse(responseCode = "409", description = "Bu E-posta veya T.C. Kimlik numarası zaten sistemde kayıtlı")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Sisteme Giriş Yap (Login)", description = "Mevcut müşterinin e-posta ve şifresini doğrular. Başarılı olursa Bearer formatında JWT Token döner.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Giriş başarılı, JWT Token alındı"),
            @ApiResponse(responseCode = "400", description = "E-posta veya şifre formatı hatalı"),
            @ApiResponse(responseCode = "401", description = "Hatalı şifre veya bulunamayan hesap (Unauthorized)")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }
}