package com.telco.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
@Data
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "note", length = 500)
    private String note;

    // Hibernate için boş constructor
    public OrderStatusHistory() {}

    // Kolaylık sağlamak için dolu constructor
    public OrderStatusHistory(Long orderId, String status, String note) {
        this.orderId = orderId;
        this.status = status;
        this.changedAt = LocalDateTime.now();
        this.note = note;
    }
}