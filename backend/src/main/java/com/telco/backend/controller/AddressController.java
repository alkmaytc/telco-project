package com.telco.backend.controller;

import com.telco.backend.dto.BuildingResponseDTO;
import com.telco.backend.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/districts")
    public ResponseEntity<List<String>> getDistricts() {
        return ResponseEntity.ok(addressService.getDistricts());
    }

    @GetMapping("/neighborhoods")
    public ResponseEntity<List<String>> getNeighborhoods(@RequestParam String district) {
        return ResponseEntity.ok(addressService.getNeighborhoods(district));
    }

    @GetMapping("/streets")
    public ResponseEntity<List<String>> getStreets(
            @RequestParam String district,
            @RequestParam String neighborhood) {
        return ResponseEntity.ok(addressService.getStreets(district, neighborhood));
    }

    @GetMapping("/buildings")
    public ResponseEntity<List<BuildingResponseDTO>> getBuildings(
            @RequestParam String district,
            @RequestParam String neighborhood,
            @RequestParam String street) {
        return ResponseEntity.ok(addressService.getBuildings(district, neighborhood, street));
    }
}