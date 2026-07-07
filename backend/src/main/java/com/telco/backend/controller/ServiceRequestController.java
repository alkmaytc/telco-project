package com.telco.backend.controller;

import com.telco.backend.dto.ServiceRequestDTO;
import com.telco.backend.dto.ServiceRequestResponseDTO;
import com.telco.backend.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-requests")
@RequiredArgsConstructor
@Tag(name = "Altyapı Talep Yönetimi", description = "Altyapısı olmayan müşterilerin taleplerini toplama ve listeleme işlemleri")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @PostMapping
    @Operation(summary = "Yeni Altyapı Talebi Oluştur", description = "Hem giriş yapmış (Token'lı) hem de anonim (Token'sız) misafirler talep bırakabilir.")
    public ResponseEntity<ServiceRequestResponseDTO> createRequest(
            @Valid @RequestBody ServiceRequestDTO dto,
            Authentication authentication) {

        // 🎯 AKILLI E-POSTA YAKALAYICI: İstek atan kişinin token'ı varsa e-postasını al, yoksa null geç.
        String email = null;
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            email = authentication.getName(); // JWT'deki sub (email) bilgisini çeker
        }

        return ResponseEntity.ok(serviceRequestService.createRequest(dto, email));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Bekleyen Talepleri Getir (Admin)", description = "Sadece ADMIN rolüne sahip yetkililer bekleyen altyapı taleplerini görebilir.")
    public ResponseEntity<List<ServiceRequestResponseDTO>> getPendingRequests() {
        return ResponseEntity.ok(serviceRequestService.getAllPendingRequests());
    }

    // 🎯 YENİ EKLENDİ: Admin paneli üzerinden statü güncelleme (PUT) uç noktası
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Talep Statüsünü Güncelle (Admin)", description = "Sadece ADMIN rolüne sahip yetkililer talep statüsünü (örn: TAMAMLANDI) güncelleyebilir.")
    public ResponseEntity<ServiceRequestResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        ServiceRequestResponseDTO response = serviceRequestService.updateRequestStatus(id, status);
        return ResponseEntity.ok(response);
    }
}