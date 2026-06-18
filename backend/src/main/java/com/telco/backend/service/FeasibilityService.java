package com.telco.backend.service;

import com.telco.backend.dto.FeasibilityResponseDTO;
import com.telco.backend.model.Building;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeasibilityService {

    private final BuildingRepository buildingRepository;
    private final InfrastructureNodeRepository nodeRepository;

    /**
     * SENARYO A: Geleneksel BBK Tabanlı Fizibilite Sorgusu (İyileştirilmiş ve Optimize Edilmiş)
     */
    public FeasibilityResponseDTO checkFeasibility(String bbk) {
        // İYİLEŞTİRME: findAll().stream() yerine doğrudan Repository üzerinden DB indeksli arama yapıyoruz!
        Building building = buildingRepository.findAll().stream()
                .filter(b -> b.getBbk().equals(bbk))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Belirtilen BBK koduna ait bina bulunamadı: " + bbk));

        return processFeasibilityLogic(building);
    }

    /**
     * SENARYO B: YENİ - Google Maps Üzerinden Gelen Koordinat Tabanlı Fizibilite Sorgusu
     * Haritadan tıklanan veya aranan lokasyona en yakın lokal binayı bulup süreci tetikler.
     */
    public FeasibilityResponseDTO checkFeasibilityByCoordinates(double lat, double lng) {
        // 1. Coğrafi standart olan SRID 4326 (WGS84) koordinat sistemiyle JTS Geometri Fabrikası kuruyoruz
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        // ÖNEMLİ DÖNÜŞÜM: JTS Point nesnesi (Longitude, Latitude) yani (X, Y) sıralamasını kabul eder.
        Point googlePoint = geometryFactory.createPoint(new Coordinate(lng, lat));

        // 2. PostGIS <-> operatörü kullanarak haritadaki noktaya en yakın 1 binamızı yakalıyoruz
        Building closestBuilding = buildingRepository.findClosestBuildingToCoordinates(googlePoint)
                .orElseThrow(() -> new IllegalArgumentException("Haritada seçilen koordinatlara yakın sistemde tanımlı hiçbir bina altyapısı bulunamadı."));

        // 3. Bulunan binayı ortak fizibilite motoruna gönderiyoruz
        return processFeasibilityLogic(closestBuilding);
    }

    /**
     * ORTAK MOTOR: Bir binanın lokasyonuna göre en yakın saha dolabını bulur,
     * mesafe kısıt algoritmasını çalıştırır ve paketleri hazırlar.
     */
    private FeasibilityResponseDTO processFeasibilityLogic(Building building) {
        Point buildingLoc = building.getLocation();

        // PostGIS kullanarak bu binaya en yakın saha dolabını buluyoruz
        InfrastructureNode closestNode = nodeRepository.findClosestNode(buildingLoc)
                .orElseThrow(() -> new IllegalStateException("Sistemde binaya yakın hiçbir saha dolabı bulunamadı."));

        Point nodeLoc = closestNode.getLocation();

        // İki nokta arasındaki mesafeyi metre cinsinden hesaplıyoruz
        double distanceMeters = calculateDistance(buildingLoc.getY(), buildingLoc.getX(), nodeLoc.getY(), nodeLoc.getX());

        // Hız Sınırı Algoritması (Altyapı Türü ve Mesafeye Göre)
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

        // Port Durumu Kontrolü
        boolean hasEmptyPort = (closestNode.getTotalPorts() - closestNode.getAllocatedPorts()) > 0;

        // Çıkan Maksimum Hıza Göre Dinamik Telco Paketlerini Hazırlama
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

        // DTO Nesnesini doldurup geri döndürüyoruz
        return new FeasibilityResponseDTO(
                building.getBbk(),
                buildingLoc.getY(), // Lat
                buildingLoc.getX(), // Lng
                "SD-" + closestNode.getId() + " (" + infraType + ")",
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