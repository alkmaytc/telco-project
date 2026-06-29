package com.telco.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Yeni Sipariş Oluşturma İstek Nesnesi")
public class OrderRequestDTO {

    @Schema(description = "Sipariş verilecek binanın 10 haneli benzersiz kimlik kodu (BBK)", example = "1750295558", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Bina (BBK) kodu boş bırakılamaz.")
    @Size(min = 10, max = 20, message = "Geçersiz BBK formatı.")
    private String bbk;

    @Schema(description = "Satın alınmak istenen internet paketinin tam adı", example = "Telco Giga Fiber 1000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Paket adı boş bırakılamaz.")
    @Size(min = 3, max = 100, message = "Paket adı en az 3 karakter olmalıdır.")
    private String packageName;

    @Schema(description = "Seçilen paketin indirme (Download) hızı", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
    @Positive(message = "Paket internet hızı sıfırdan büyük pozitif bir değer olmalıdır.")
    @Min(value = 16, message = "Sistemimizdeki en düşük paket hızı 16 Mbps'dir.")
    private int speedMbps;

    @Schema(description = "Paketin aylık ücreti (TL cinsinden)", example = "699.90", requiredMode = Schema.RequiredMode.REQUIRED)
    @Positive(message = "Paket fiyatı sıfırdan büyük pozitif bir değer olmalıdır.")
    private double price;
}