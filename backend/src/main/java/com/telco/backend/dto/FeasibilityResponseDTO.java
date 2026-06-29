package com.telco.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Altyapı Sorgulama (Fizibilite) Detay ve Simülasyon Yanıt Nesnesi")
public class FeasibilityResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Sorgulanan binanın BBK kodu", example = "1750295558")
    private String bbk;

    @Schema(description = "Binanın (veya müşterinin yedek GPS'inin) Enlem koordinatı", example = "39.788787")
    private double buildingLat;

    @Schema(description = "Binanın (veya müşterinin yedek GPS'inin) Boylam koordinatı", example = "30.507176")
    private double buildingLng;

    @Schema(description = "Eşleşen en yakın saha dolabının ID'si ve Türü", example = "SD-1026 (FIBER) [Alternatif Dağıtım Hattı]")
    private String closestNodeName;

    @Schema(description = "Saha Dolabının Altyapı Türü", example = "FIBER", allowableValues = {"FIBER", "VDSL"})
    private String infrastructureType;

    @Schema(description = "Saha Dolabının Enlem koordinatı", example = "39.789500")
    private double closestNodeLat;

    @Schema(description = "Saha Dolabının Boylam koordinatı", example = "30.508000")
    private double closestNodeLng;

    @Schema(description = "Bina ile Saha Dolabı arasındaki hesaplanan net fiziksel mesafe (Metre)", example = "58.75")
    private double distanceMeters;

    @Schema(description = "Bu mesafe ve sinyal zayıflamasına göre verilebilecek teorik maksimum hız (Mbps)", example = "1000")
    private int maxAvailableSpeedMbps;

    @Schema(description = "Saha dolabında fiziksel olarak müşteriye atanabilecek boş port var mı?", example = "true")
    private boolean hasEmptyPort;

    @Schema(description = "Bu adreste sunulabilecek uygun internet paketleri listesi")
    private List<InternetPackageDTO> availablePackages;

    // YENİ - GELİŞMİŞ TELEKOMÜNİKASYON METRİKLERİ
    @Schema(description = "Kablo mesafesine bağlı fiziksel Hat Zayıflaması (Attenuation - dB). Fiber için sabit 2.1 dB.", example = "2.1")
    private double attenuationDb;

    @Schema(description = "Sinyal Gürültü Oranı (SNR Margin - dB). Yüksek olması bağlantı stabilitesini gösterir.", example = "35.0")
    private double snrMarginDb;

    @Schema(description = "Sinyal metriklerine göre hesaplanan genel Hat Kalite Skoru", example = "100")
    private int lineQualityPercent;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Müşteriye Sunulabilir İnternet Paketi Detayı")
    public static class InternetPackageDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "Sistemdeki Paket ID", example = "5")
        private Long id;

        @Schema(description = "Paketin pazarlama / satış adı", example = "Telco Giga Fiber 1000")
        private String packageName;

        @Schema(description = "Paketin taahhüt edilen indirme (Download) hızı", example = "1000")
        private int speedMbps;

        @Schema(description = "Paketin aylık ücreti (TL)", example = "699.90")
        private double price;
    }
}