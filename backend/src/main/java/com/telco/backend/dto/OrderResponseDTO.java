package com.telco.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sipariş Yanıt / Detay Nesnesi (Siber Güvenlik Korumalı)")
public class OrderResponseDTO {

    @Schema(description = "Sistem tarafından atanan benzersiz Sipariş ID", example = "10")
    private Long id;

    @Schema(description = "Siparişin bağlandığı adresin BBK kodu", example = "1750295558")
    private String bbk;

    @Schema(description = "Aktif olan internet paketinin adı", example = "Telco Giga Fiber 1000")
    private String packageName;

    @Schema(description = "Paket hızı (Mbps)", example = "1000")
    private int speedMbps;

    @Schema(description = "Paket fiyatı (Aylık TL)", example = "699.90")
    private double price;

    @Schema(description = "Siparişin anlık durumu", example = "ONAYLANDI")
    private String status;

    @Schema(description = "Siparişin sisteme düştüğü tarih ve saat", example = "2026-06-29T13:45:00")
    private LocalDateTime createdAt;

    @Schema(description = "KVKK kapsamında maskelenmiş müşteri adı soyadı", example = "Alkim A***")
    private String customerFullName;
}