package com.telco.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Global Hata Yönetimi Yanıt Nesnesi")
public class ErrorResponseDTO {

    @Schema(description = "Hatanın gerçekleştiği an", example = "2026-06-29T14:15:30")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP Hata Kodu", example = "400")
    private int status;

    @Schema(description = "Hata Türü", example = "Bad Request")
    private String error;

    @Schema(description = "Kullanıcıya gösterilecek okunabilir mesaj", example = "Bina (BBK) kodu boş bırakılamaz.")
    private String message;

    @Schema(description = "Hatanın meydana geldiği endpoint rotası", example = "/api/v1/orders")
    private String path;
}