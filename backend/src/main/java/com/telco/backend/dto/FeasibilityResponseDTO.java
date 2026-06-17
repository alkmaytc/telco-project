package com.telco.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeasibilityResponseDTO {
    private String bbk;
    private double buildingLat;
    private double buildingLng;

    private String closestNodeName;
    private String infrastructureType; // FIBER veya VDSL
    private double closestNodeLat;
    private double closestNodeLng;

    private double distanceMeters; // PostGIS'ten gelecek metre cinsinden mesafe
    private int maxAvailableSpeedMbps; // Algoritmadan çıkacak maksimum hız
    private boolean hasEmptyPort; // Boş port var mı yok mu?

    private List<InternetPackageDTO> availablePackages; // Bu binaya sunulacak paket listesi

    // İç içe (Inner) DTO: Paket detayları için
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternetPackageDTO {
        private Long id;
        private String packageName;
        private int speedMbps;
        private double price;
    }
}