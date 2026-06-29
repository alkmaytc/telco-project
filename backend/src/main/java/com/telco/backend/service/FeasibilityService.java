package com.telco.backend.service;

import com.telco.backend.dto.FeasibilityResponseDTO;
import com.telco.backend.model.Building;
import com.telco.backend.model.Customer;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.CustomerRepository; // 🎯 EKLENDİ
import com.telco.backend.repository.InfrastructureNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🎯 EKLENDİ
import org.locationtech.jts.geom.Point;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder; // 🎯 EKLENDİ
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // 🎯 Loglama için eklendi
public class FeasibilityService {

    private final BuildingRepository buildingRepository;
    private final InfrastructureNodeRepository nodeRepository;
    private final CustomerRepository customerRepository; // 🎯 Müşteri lokasyonuna ulaşmak için eklendi

    @Cacheable(value = "feasibility_bbk", key = "#bbk")
    public FeasibilityResponseDTO checkFeasibility(String bbk) {
        Building building = buildingRepository.findByBbk(bbk)
                .orElseThrow(() -> new IllegalArgumentException("Belirtilen BBK koduna ait bina bulunamadı: " + bbk));

        return processFeasibilityLogic(building);
    }

    /**
     * 🔐 YARDIMCI METOT: Güvenli bir şekilde aktif oturumdaki müşteriyi alır
     */
    private Customer getCurrentCustomerSafely() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                return customerRepository.findByEmail(auth.getName()).orElse(null);
            }
        } catch (Exception e) {
            log.warn("Fizibilite sorgusu anonim olarak yapılıyor, fallback devre dışı.");
        }
        return null;
    }

    /**
     * ADVANCED MOTOR: Sinyal Kalite Simülatörü + Akıllı Alternatif Dağıtım Noktası Algoritması
     */
    private FeasibilityResponseDTO processFeasibilityLogic(Building building) {
        Point bestLoc = building.getLocation();

        // 1. PostGIS kullanarak bu binaya EN YAKIN ilk saha dolabını buluyoruz
        InfrastructureNode targetNode = nodeRepository.findClosestNode(bestLoc).orElse(null);

        double distanceMeters = (targetNode != null) ?
                calculateDistance(bestLoc.getY(), bestLoc.getX(), targetNode.getLocation().getY(), targetNode.getLocation().getX()) : Double.MAX_VALUE;

        // 🚀 ADIM 2: DEAD FIELD CANLANDIRMA (MÜŞTERİ LOKASYONU FALLBACK ALGORİTMASI)
        if (targetNode == null || distanceMeters > 500.0) {
            log.info("🚨 [DEBUG-1] Fallback bloğuna başarıyla girildi! Bina-Dolap Mesafesi: {} metre", distanceMeters);

            Customer customer = getCurrentCustomerSafely();

            if (customer == null) {
                log.warn("🚨 [DEBUG-2] HATA: Oturum açmış müşteri bulunamadı!");
            } else if (customer.getLocation() == null) {
                log.warn("🚨 [DEBUG-3] HATA: Müşteri bulundu (ID:{}) ama veritabanındaki GPS 'location' alanı NULL (Bomboş)!", customer.getId());
            } else {
                log.info("🚨 [DEBUG-4] Müşteri GPS'i DB'den çekildi: {}", customer.getLocation());

                InfrastructureNode fallbackNode = nodeRepository.findClosestNode(customer.getLocation()).orElse(null);

                if (fallbackNode != null) {
                    double customerDistance = calculateDistance(customer.getLocation().getY(), customer.getLocation().getX(), fallbackNode.getLocation().getY(), fallbackNode.getLocation().getX());
                    log.info("🚨 [DEBUG-5] Müşterinin dolaba olan uzaklığı hesaplandı: {} metre", customerDistance);

                    if (customerDistance < distanceMeters) {
                        bestLoc = customer.getLocation();
                        targetNode = fallbackNode;
                        distanceMeters = customerDistance;
                        log.info("🌟 [CROWDSOURCED HEALING] Bina (BBK:{}) koordinatı zayıftı. Müşterinin (ID:{}) şahsi GPS verisi ile daha yakın bir dolap ({}m) bulundu!",
                                building.getBbk(), customer.getId(), Math.round(distanceMeters));
                    } else {
                        log.warn("🚨 [DEBUG-6] İPTAL: Müşterinin koordinatı ({}m), binanın koordinatından ({}m) daha yakın DEĞİL!", customerDistance, distanceMeters);
                    }
                } else {
                    log.warn("🚨 [DEBUG-7] HATA: Müşterinin koordinatına yakın hiçbir saha dolabı bulunamadı!");
                }
            }
        }
        boolean isAlternativeRouteUsed = false;
        boolean initialNodeHasPort = (targetNode.getTotalPorts() - targetNode.getAllocatedPorts()) > 0;

        // 🚀 ADIM 3: AKILLI YEDEK DOLAP ALGORİTMASI (Boş port yoksa)
        if (!initialNodeHasPort) {
            var alternativeNodeOpt = nodeRepository.findClosestNodeWithEmptyPort(bestLoc);
            if (alternativeNodeOpt.isPresent()) {
                targetNode = alternativeNodeOpt.get();
                isAlternativeRouteUsed = true;
                // Yeni seçilen yedek dolaba göre mesafeyi güncelliyoruz
                distanceMeters = calculateDistance(bestLoc.getY(), bestLoc.getX(), targetNode.getLocation().getY(), targetNode.getLocation().getX());
            }
        }

        Point nodeLoc = targetNode.getLocation();

        // 🚀 ADIM 4: TELEKOM SİNYAL METRİKLERİ HESAPLAMA ALGORİTMASI
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

        // 🚀 ADIM 5: Hız Sınırı Algoritması (Nihai dolap mesafesine göre)
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

        // 🚀 ADIM 6: Dinamik Telco Paketlerini Hazırlama
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
                bestLoc.getY(), // 🎯 Eğer müşteri lokasyonu kullanıldıysa güncel koordinatı döner
                bestLoc.getX(),
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