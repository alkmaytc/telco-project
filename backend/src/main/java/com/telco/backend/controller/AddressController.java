package com.telco.backend.controller;

import com.telco.backend.dto.BuildingResponseDTO;
import com.telco.backend.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "📍 Adres Yönetimi (Address API)", description = "İlçe, mahalle, sokak ve bina hiyerarşisi üzerinden adres getirme işlemleri (PostgreSQL destekli)")
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "İlçeleri Getir", description = "Sistemdeki tüm geçerli ilçelerin listesini getirir.")
    @ApiResponse(responseCode = "200", description = "İlçeler başarıyla getirildi")
    @GetMapping("/districts")
    public ResponseEntity<List<String>> getDistricts() {
        return ResponseEntity.ok(addressService.getDistricts());
    }

    @Operation(summary = "Mahalleleri Getir", description = "Seçilen ilçeye ait mahallelerin listesini getirir.")
    @GetMapping("/neighborhoods")
    public ResponseEntity<List<String>> getNeighborhoods(
            @Parameter(description = "Filtrelenecek ilçe adı", example = "Odunpazarı") @RequestParam String district) {
        return ResponseEntity.ok(addressService.getNeighborhoods(district));
    }

    @Operation(summary = "Sokakları Getir", description = "Seçilen ilçe ve mahalleye ait sokakların listesini getirir.")
    @GetMapping("/streets")
    public ResponseEntity<List<String>> getStreets(
            @Parameter(description = "İlçe adı", example = "Odunpazarı") @RequestParam String district,
            @Parameter(description = "Mahalle adı", example = "Vişnelik") @RequestParam String neighborhood) {
        return ResponseEntity.ok(addressService.getStreets(district, neighborhood));
    }

    @Operation(summary = "Binaları Getir (BBK Listesi)", description = "İlçe, mahalle ve sokak bilgisine göre o sokaktaki binaları (kapı no ve BBK) getirir.")
    @GetMapping("/buildings")
    public ResponseEntity<List<BuildingResponseDTO>> getBuildings(
            @Parameter(description = "İlçe adı", example = "Odunpazarı") @RequestParam String district,
            @Parameter(description = "Mahalle adı", example = "Vişnelik") @RequestParam String neighborhood,
            @Parameter(description = "Sokak adı", example = "Karanfil Sokak") @RequestParam String street) {
        return ResponseEntity.ok(addressService.getBuildings(district, neighborhood, street));
    }
}