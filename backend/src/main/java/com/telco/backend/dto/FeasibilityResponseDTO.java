package com.telco.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable; // 🎯 Kütüphane ithal edildi kanka ✅
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
// 🎯 MADDE 9: Ağır PostGIS sorgu sonuçlarının Redis NoSQL RAM katmanında saklanabilmesi için Serializable yapıldı ✅
public class FeasibilityResponseDTO implements Serializable {

    // 🎯 Redis için benzersiz nesne kimlik numarası mühürlendi ✅
    private static final long serialVersionUID = 1L;

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

    // YENİ - GELİŞMİŞ TELEKOMÜNİKASYON METRİKLERİ
    private double attenuationDb;      // Hat Zayıflaması (dB)
    private double snrMarginDb;        // Sinyal Gürültü Oranı (dB)
    private int lineQualityPercent;    // Hat Kalite Skoru (%0 - %100)

    // İç içe (Inner) DTO: Paket detayları için
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    // 🎯 Listeyi oluşturan bu alt nesneye de aynı yetenek verildi, yoksa Redis yine patlardı ✅
    public static class InternetPackageDTO implements Serializable {

        private static final long serialVersionUID = 1L; // Alt nesne kimliği

        private Long id;
        private String packageName;
        private int speedMbps;
        private double price;
    }
}