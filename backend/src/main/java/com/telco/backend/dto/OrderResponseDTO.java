package com.telco.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private String bbk;
    private String packageName;
    private int speedMbps;
    private double price;
    private String status;
    private LocalDateTime createdAt;
    private String customerFullName; // KVKK maskelenmiş isim bilgisi ✅
}