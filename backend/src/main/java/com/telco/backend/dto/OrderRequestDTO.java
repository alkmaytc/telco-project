package com.telco.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderRequestDTO {

    // 🎯 Adres Güvenliği: Boş veya tanımsız bir BBK koduyla sipariş oluşturulmasını engelleriz
    @NotBlank(message = "Bina (BBK) kodu boş bırakılamaz.")
    @Size(min = 10, max = 20, message = "Geçersiz BBK formatı.")
    private String bbk;

    @NotBlank(message = "Paket adı boş bırakılamaz.")
    @Size(min = 3, max = 100, message = "Paket adı en az 3 karakter olmalıdır.")
    private String packageName;

    // 🎯 Hız Güvenliği: Sıfır veya negatif internet hızı basılmasını engeller kanka ✅
    @Positive(message = "Paket internet hızı sıfırdan büyük pozitif bir değer olmalıdır.")
    @Min(value = 16, message = "Sistemimizdeki en düşük paket hızı 16 Mbps'dir.")
    private int speedMbps;

    // 🎯 Finansal Güvenliği: Negatif fiyat manipülasyonlarını kapıda engeller kanka ✅
    @Positive(message = "Paket fiyatı sıfırdan büyük pozitif bir değer olmalıdır.")
    private double price;
}