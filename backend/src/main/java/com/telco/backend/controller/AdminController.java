package com.telco.backend.controller;

import com.telco.backend.dto.AdminDashboardDTO;
import com.telco.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "🛡️ Yönetim Paneli (Admin API)", description = "Sistem istatistikleri, BSS (Business Support Systems) verileri, saha dolabı yönetimi ve log takibi")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Dashboard Verilerini Getir", description = "Admin paneli için gerekli olan ciro, aktif aboneler, RabbitMQ kuyruk durumu ve saha dolabı istatistiklerini tek seferde getirir.")
    @ApiResponse(responseCode = "200", description = "Dashboard istatistikleri başarıyla yüklendi")
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardData());
    }
}