package com.telco.backend.controller;

import com.telco.backend.dto.FeasibilityResponseDTO;
import com.telco.backend.service.FeasibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feasibility")
@RequiredArgsConstructor
@Tag(name = "📡 Altyapı & Fizibilite Yönetimi (Feasibility API)", description = "BBK tabanlı altyapı sorgulama, sinyal kalite simülasyonu ve akıllı saha dolabı eşleştirme operasyonları")
public class FeasibilityController {

    private final FeasibilityService feasibilityService;

    @Operation(summary = "Adres (BBK) Tabanlı Altyapı Sorgulama", description = "Verilen BBK koduna göre en yakın saha dolabını PostGIS ile bulur. Gerektiğinde 'Crowdsourced Healing' algoritmasını devreye sokarak müşteri GPS verisiyle kurtarma yapar ve hat zayıflaması/SNR metriklerini hesaplar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fizibilite başarıyla hesaplandı ve uygun paketler getirildi"),
            @ApiResponse(responseCode = "400", description = "Geçersiz BBK kodu veya sistemde bina bulunamadı"),
            @ApiResponse(responseCode = "500", description = "Kapsama alanında hiçbir uygun saha dolabı bulunamadı")
    })
    @GetMapping("/bbk")
    public ResponseEntity<FeasibilityResponseDTO> checkFeasibilityByBbk(
            @Parameter(description = "Sorgulanacak binanın 10 haneli benzersiz kimlik kodu (BBK)", example = "1750295558", required = true)
            @RequestParam("code") String code) {
        FeasibilityResponseDTO response = feasibilityService.checkFeasibility(code);
        return ResponseEntity.ok(response);
    }
}