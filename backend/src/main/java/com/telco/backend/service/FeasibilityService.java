package com.telco.backend.service;

import com.telco.backend.dto.FeasibilityResponseDTO;
import com.telco.backend.model.Building;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.cache.annotation.Cacheable; // Cache kütüphanesi aktif ✅
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeasibilityService {

    private final BuildingRepository buildingRepository;
    private final InfrastructureNodeRepository nodeRepository;

    /**
     * SENARYO A: Geleneksel BBK Tabanlı Fizibilite Sorgusu
     * 🎯 REDIS: Gelen benzersiz BBK koduna göre fizibilite sonucunu RAM'e kilitler kanka ✅
     */
    @Cacheable(value = "feasibility_bbk", key = "#bbk")
    public FeasibilityResponseDTO checkFeasibility(String bbk) {
        Building building = buildingRepository.findByBbk(bbk)
                .orElseThrow(() -> new IllegalArgumentException("Belirtilen BBK koduna ait bina bulunamadı: " + bbk));

        return processFeasibilityLogic(building);
    }

    /**
     * ADVANCED MOTOR: Sinyal Kalite Simülatörü + Akıllı Alternatif Dağıtım Noktası Algoritması
     */
    private FeasibilityResponseDTO processFeasibilityLogic(Building building) {
        Point buildingLoc = building.getLocation();

        // 1. PostGIS kullanarak bu binaya EN YAKIN ilk saha dolabını buluyoruz
        InfrastructureNode targetNode = nodeRepository.findClosestNode(buildingLoc)
                .orElseThrow(() -> new IllegalStateException("Sistemde binaya yakın hiçbir saha dolabı bulunamadı."));

        boolean isAlternativeRouteUsed = false;
        boolean initialNodeHasPort = (targetNode.getTotalPorts() - targetNode.getAllocatedPorts()) > 0;

        // 🚀 ADIM 2: AKILLI YEDEK DOLAP ALGORİTMASI
        if (!initialNodeHasPort) {
            var alternativeNodeOpt = nodeRepository.findClosestNodeWithEmptyPort(buildingLoc);
            if (alternativeNodeOpt.isPresent()) {
                targetNode = alternativeNodeOpt.get();
                isAlternativeRouteUsed = true;
            }
        }

        Point nodeLoc = targetNode.getLocation();

        // 2. Seçilen nihai dolap ile olan mesafeyi metre cinsinden hesaplıyoruz
        double distanceMeters = calculateDistance(buildingLoc.getY(), buildingLoc.getX(), nodeLoc.getY(), nodeLoc.getX());

        // 🚀 ADIM 1: TELEKOM SİNYAL METRİKLERİ HESAPLAMA ALGORİTMASI
        double attenuationDb = 0.0;
        double snrMarginDb = 31.0;
        int lineQualityPercent = 100;
        String infraType = targetNode.getNodeType();

        if ("FIBER".equalsIgnoreCase(infraType)) {
            attenuationDb = 2.1;
            snrMarginDb = 35.0;
            lineQualityPercent = 100;
        } else {
            // VDSL Bakır kablo fiziksel sinyal kaybı formülü (Her 100m'de ~13.8 dB zayıflama)
            attenuationDb = (distanceMeters / 100.0) * 13.8;
            snrMarginDb = 31.0 - ((distanceMeters / 100.0) * 6.5);
            if (snrMarginDb < 6.0) snrMarginDb = 6.0;

            lineQualityPercent = Math.max(0, Math.min(100, (int) (100 - (attenuationDb * 1.5) + (snrMarginDb * 0.5))));
        }

        attenuationDb = Math.round(attenuationDb * 100.0) / 100.0;
        snrMarginDb = Math.round(snrMarginDb * 100.0) / 100.0;

        // 4. Hız Sınırı Algoritması (Nihai dolap mesafesine göre)
        int maxSpeed = 0;
        if ("FIBER".equalsIgnoreCase(infraType)) {
            maxSpeed = 1000;
        } else {
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

        // 6. Dinamik Telco Paketlerini Hazırlama
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

        String nodeDisplayName = "SD-" + targetNode.getId() + " (" + infraType + ")";
        if (isAlternativeRouteUsed) {
            nodeDisplayName += " [Alternatif Dağıtım Hattı]";
        }

        boolean hasEmptyPort = (targetNode.getTotalPorts() - targetNode.getAllocatedPorts()) > 0;

        return new FeasibilityResponseDTO(
                building.getBbk(),
                buildingLoc.getY(),
                buildingLoc.getX(),
                nodeDisplayName,
                infraType,
                nodeLoc.getY(),
                nodeLoc.getX(),
                Math.round(distanceMeters * 100.0) / 100.0,
                maxSpeed,
                hasEmptyPort,
                availablePackages,
                attenuationDb,
                snrMarginDb,
                lineQualityPercent
        );
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3; // Dünya yarıçapı (metre)
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