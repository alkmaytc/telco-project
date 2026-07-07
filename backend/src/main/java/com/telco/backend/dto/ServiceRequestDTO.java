package com.telco.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Altyapı Talep Oluşturma Nesnesi")
public class ServiceRequestDTO {

    @NotBlank(message = "BBK kodu boş bırakılamaz!")
    @Schema(description = "Talep bırakılan binanın BBK kodu", example = "REMOTE-001")
    private String bbk;

    @Schema(description = "İlçe", example = "Sivrihisar")
    private String district;

    @Schema(description = "Mahalle", example = "Uzaklar Mahallesi")
    private String neighborhood;

    @Schema(description = "Sokak", example = "Altyapısızlar Sokak")
    private String street;
}