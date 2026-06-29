package com.telco.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore; // 🎯 ÇÖZÜM: Jackson'ın bu alanı es geçmesini sağlayan kütüphane eklendi!
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "buildings")
@Getter
@Setter
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bbk", unique = true, nullable = false)
    private String bbk; // 10 haneli benzersiz Bina Kimlik Kodu (Fizibilite için en kritik alan)

    @Column(name = "bina_no")
    private String buildingNumber;

    @Column(name = "district")
    private String district; // İlçe (Örn: Odunpazarı)

    @Column(name = "neighborhood")
    private String neighborhood; // Mahalle (Örn: Vişnelik)

    @Column(name = "street")
    private String street; // Sokak (Örn: Karanfil Sokak)

    @JsonIgnore // 🎯 ÇÖZÜM: Redis'e kaydedilirken bu alan es geçilecek ve Serialization hatası mermer gibi yok olacak! ✅
    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location; // Binanın Google Maps uyumlu PostGIS koordinatı
}