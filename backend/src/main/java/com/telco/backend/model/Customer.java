package com.telco.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KURAL: Bir kişinin TC/Abone numarası eşsiz olmalıdır
    @Column(name = "identity_number", nullable = false, unique = true)
    private String identityNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    // Müşterinin evinin/binasının harita üzerindeki konumu
    @Column(name = "location", columnDefinition = "geometry(Point,4326)")
    private Point location;

    // İLİŞKİ ve KURAL: Bir port sadece BİR müşteriye verilebilir (OneToOne)
    @OneToOne
    @JoinColumn(name = "port_id", unique = true)
    private com.telco.backend.model.Port port;
}