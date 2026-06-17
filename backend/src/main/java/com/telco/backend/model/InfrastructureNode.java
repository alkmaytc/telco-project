package com.telco.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "infrastructure_nodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfrastructureNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KURAL: İki dolabın adı/kodu aynı olamaz
    @Column(name = "node_name", nullable = false, unique = true)
    private String name;

    @Column(name = "node_type")
    private String nodeType; // Örn: SAHA_DOLABI, SANTRAL

    // PostGIS Koordinatı
    @Column(name = "location", columnDefinition = "geometry(Point,4326)")
    private Point location;

    // --- YENİ EKLENEN ADRES HİYERARŞİ ALANLARI ---
    @Column(name = "district")
    private String district; // Örn: Odunpazarı

    @Column(name = "neighborhood")
    private String neighborhood; // Örn: Vişnelik

    @Column(name = "total_ports")
    private Integer totalPorts; // Toplam Port Kapasitesi (Örn: 32 veya 64)

    @Column(name = "allocated_ports")
    private Integer allocatedPorts; // Dolu / Rezerve Edilmiş Port Sayısı
}