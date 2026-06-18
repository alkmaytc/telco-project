package com.telco.backend.controller;

import com.telco.backend.dto.FeasibilityResponseDTO; // Doğru paket yolu burası olmalı
import com.telco.backend.service.FeasibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feasibility")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FeasibilityController {

    private final FeasibilityService feasibilityService;

    /**
     * Akıllı Altyapı Fizibilite ve Paket Öneri Endpoint'i
     * GET http://localhost:8080/api/v1/feasibility?bbk=1498423684
     */
    @GetMapping
    public ResponseEntity<FeasibilityResponseDTO> checkFeasibility(@RequestParam String bbk) {
        FeasibilityResponseDTO response = feasibilityService.checkFeasibility(bbk);
        return ResponseEntity.ok(response);
    }
}