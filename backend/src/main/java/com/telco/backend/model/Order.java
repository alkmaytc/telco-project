package com.telco.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bbk", nullable = false)
    private String bbk;

    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "speed_mbps")
    private Integer speedMbps;

    @Column(name = "price")
    private Double price;

    @Column(name = "status", nullable = false)
    private String status; // ONAYLANDI, PORT_BEKLENIYOR, IPTAL_EDILDI

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // --- GÜVENLİK VE DETAYLI OPERASYON İÇİN EKLENEN MÜŞTERİ İLİŞKİSİ ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = true)
    private Customer customer;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}