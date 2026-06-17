package com.telco.backend.controller;

import com.telco.backend.dto.FeasibilityResponseDTO;
import com.telco.backend.service.FeasibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feasibility")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Frontend (React) uygulamamızın bağlanabilmesi için CORS izni
public class FeasibilityController {

    private final FeasibilityService feasibilityService;

    /**
     * Verilen BBK koduna göre binanın fizibilitesini sorgular.
     * En yakın saha dolabını bulur, mesafeyi ölçer, hızı belirler ve paketleri döner.
     * * Örnek İstek: GET http://localhost:8080/api/v1/feasibility/check?bbk=1017452928
     */
    @GetMapping("/check")
    public ResponseEntity<FeasibilityResponseDTO> checkFeasibility(@RequestParam String bbk) {
        try {
            FeasibilityResponseDTO response = feasibilityService.checkFeasibility(bbk);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}