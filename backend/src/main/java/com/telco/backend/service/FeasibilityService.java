package com.telco.backend.service;

import com.telco.backend.dto.FeasibilityResponseDTO; // Doğru bağımsız DTO paketi
import com.telco.backend.model.Building;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeasibilityService {

    private final BuildingRepository buildingRepository;
    private final InfrastructureNodeRepository nodeRepository;

    public FeasibilityResponseDTO checkFeasibility(String bbk) {
        // 1. Binayı BBK kodu ile buluyoruz
        Building building = buildingRepository.findAll().stream()
                .filter(b -> b.getBbk().equals(bbk))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Belirtilen BBK koduna ait bina bulunamadı: " + bbk));

        Point buildingLoc = building.getLocation();

        // 2. PostGIS kullanarak bu binaya en yakın saha dolabını buluyoruz
        InfrastructureNode closestNode = nodeRepository.findClosestNode(buildingLoc)
                .orElseThrow(() -> new IllegalStateException("Sistemde binaya yakın hiçbir saha dolabı bulunamadı."));

        Point nodeLoc = closestNode.getLocation();

        // 3. İki nokta arasındaki mesafeyi metre cinsinden hesaplıyoruz
        double distanceMeters = calculateDistance(buildingLoc.getY(), buildingLoc.getX(), nodeLoc.getY(), nodeLoc.getX());

        // 4. Hız Sınırı Algoritması (Altyapı Türü ve Mesafeye Göre)
        int maxSpeed = 0;
        String infraType = closestNode.getNodeType(); // FIBER veya VDSL

        if ("FIBER".equalsIgnoreCase(infraType)) {
            maxSpeed = 1000; // Fiberde mesafe kaybı olmaz, doğrudan 1 Gbps!
        } else { // VDSL Durumu (Bakır kablo mesafe algoritması)
            if (distanceMeters <= 50) {
                maxSpeed = 100;
            } else if (distanceMeters <= 150) {
                maxSpeed = 50;
            } else if (distanceMeters <= 300) {
                maxSpeed = 24;
            } else {
                maxSpeed = 16;
            }
        }

        // 5. Port Durumu Kontrolü
        boolean hasEmptyPort = (closestNode.getTotalPorts() - closestNode.getAllocatedPorts()) > 0;

        // 6. Çıkan Maksimum Hıza Göre Dinamik Telco Paketlerini Hazırlama (Doğru DTO İç Sınıfı İle)
        List<FeasibilityResponseDTO.InternetPackageDTO> availablePackages = new ArrayList<>();
        long packageIdCounter = 1;

        if (maxSpeed >= 16) {
            availablePackages.add(new FeasibilityResponseDTO.InternetPackageDTO(packageIdCounter++, "Telco Giriş Paket", 16, 199.90));
        }
        if (maxSpeed >= 24) {
            availablePackages.add(new FeasibilityResponseDTO.InternetPackageDTO(packageIdCounter++, "Telco Standart Paket", 24, 249.90));
        }
        if (maxSpeed >= 50) {
            availablePackages.add(new FeasibilityResponseDTO.InternetPackageDTO(packageIdCounter++, "Telco Hiper Paket", 50, 329.90));
        }
        if (maxSpeed >= 100) {
            availablePackages.add(new FeasibilityResponseDTO.InternetPackageDTO(packageIdCounter++, "Telco Mega VDSL", 100, 399.90));
        }
        if (maxSpeed >= 1000) {
            availablePackages.add(new FeasibilityResponseDTO.InternetPackageDTO(packageIdCounter++, "Telco Ultra Fiber 200", 200, 499.90));
            availablePackages.add(new FeasibilityResponseDTO.InternetPackageDTO(packageIdCounter++, "Telco Giga Fiber 1000", 1000, 699.90));
        }

        // 7. DTO Nesnesini doldurup geri döndürüyoruz
        return new FeasibilityResponseDTO(
                building.getBbk(),
                buildingLoc.getY(), // Lat
                buildingLoc.getX(), // Lng
                "SD-" + closestNode.getId() + " (" + infraType + ")", // Alan adına bağımlılığı kaldırdık, dinamik etiket ürettik
                infraType,
                nodeLoc.getY(), // Lat
                nodeLoc.getX(), // Lng
                Math.round(distanceMeters * 100.0) / 100.0, // Virgülden sonra 2 basamak
                maxSpeed,
                hasEmptyPort,
                availablePackages
        );
    }

    /**
     * Haversine Formülü: İki koordinat arasındaki küresel mesafeyi metre cinsinden döner.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3; // Dünyanın yarıçapı (metre)
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}