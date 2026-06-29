package com.telco.backend.controller;

import com.telco.backend.dto.UserProfileDTO;
import com.telco.backend.model.Customer;
import com.telco.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "👤 Müşteri Profili (User API)", description = "Kullanıcı profil bilgilerini güvenli bir şekilde görüntüleme ve güncelleme işlemleri")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Kendi Profilimi Getir", description = "Aktif oturumu olan kullanıcının bilgilerini (IDOR korumalı olarak) getirir.")
    @ApiResponse(responseCode = "200", description = "Profil bilgileri getirildi")
    @GetMapping("/me")
    public UserProfileDTO getMyProfile(@AuthenticationPrincipal Customer currentCustomer) {
        return userService.getProfile(currentCustomer);
    }

    @Operation(summary = "Kendi Profilimi Güncelle", description = "Müşterinin kişisel bilgilerini günceller.")
    @ApiResponse(responseCode = "200", description = "Profil başarıyla güncellendi")
    @PutMapping("/me")
    public UserProfileDTO updateMyProfile(
            @AuthenticationPrincipal Customer currentCustomer,
            @RequestBody UserProfileDTO dto) {
        return userService.updateProfile(currentCustomer, dto);
    }
}