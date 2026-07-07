package com.telco.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String bbk;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String neighborhood;

    @Column(nullable = false, length = 150)
    private String street;

    // Giriş yapmamış kullanıcılar (anonim) da talep bırakabilsin diye nullable = true
    @Column(nullable = true, length = 100)
    private String customerEmail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestDate;

    // BEKLEMEDE, BILDIRIM_GONDERILDI gibi statüleri tutacak
    @Column(nullable = false, length = 50)
    private String status;

    // 🎯 Veritabanına kaydedilmeden hemen önce otomatik çalışır
    @PrePersist
    protected void onCreate() {
        this.requestDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = "BEKLEMEDE"; // Varsayılan statü
        }
    }
}