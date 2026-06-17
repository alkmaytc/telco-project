package com.telco.backend.model;

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

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location; // Binanın Google Maps uyumlu PostGIS koordinatı
}