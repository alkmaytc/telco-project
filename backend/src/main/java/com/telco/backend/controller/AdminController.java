package com.telco.backend.controller;

import com.telco.backend.dto.AdminDashboardDTO;
import com.telco.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // 🎯 KİLİT İÇİN İMPORT EKLENDİ
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
// 🎯 DEĞİŞİKLİK BURADA: Tag ismini 4 numara yaptık ve KİLİT ekledik!
@Tag(name = "4. 🛡️ Yönetim Paneli (Admin API)", description = "Yalnızca ADMIN rolü gerektirir. Sistem istatistikleri, BSS (Business Support Systems) verileri, saha dolabı yönetimi ve log takibi")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Dashboard Verilerini Getir", description = "Admin paneli için gerekli olan ciro, aktif aboneler, RabbitMQ kuyruk durumu ve saha dolabı istatistiklerini tek seferde getirir.")
    @ApiResponse(responseCode = "200", description = "Dashboard istatistikleri başarıyla yüklendi")
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardData());
    }
}