package com.telco.backend.controller;

import com.telco.backend.dto.FeasibilityResponseDTO;
import com.telco.backend.service.FeasibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feasibility")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Frontend React entegrasyonu için CORS izni
public class FeasibilityController {

    private final FeasibilityService feasibilityService;

    /**
     * SENARYO A: Geleneksel Hiyerarşik Adres Seçimi (BBK Tabanlı)
     * GET http://localhost:8080/api/v1/feasibility/bbk?code=1498423684
     */
    @GetMapping("/bbk")
    public ResponseEntity<FeasibilityResponseDTO> checkFeasibilityByBbk(@RequestParam("code") String code) {
        FeasibilityResponseDTO response = feasibilityService.checkFeasibility(code);
        return ResponseEntity.ok(response);
    }

    /**
     * SENARYO B: Google Maps Üzerinden Serbest Koordinat Seçimi
     * GET http://localhost:8080/api/v1/feasibility/coordinates?lat=39.7685&lng=30.5095
     */
    @GetMapping("/coordinates")
    public ResponseEntity<FeasibilityResponseDTO> checkFeasibilityByCoordinates(
            @RequestParam double lat,
            @RequestParam double lng) {
        FeasibilityResponseDTO response = feasibilityService.checkFeasibilityByCoordinates(lat, lng);
        return ResponseEntity.ok(response);
    }
}