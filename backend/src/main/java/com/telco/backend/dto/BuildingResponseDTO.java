package com.telco.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bina ve BBK Yanıt Nesnesi")
public class BuildingResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Sistemdeki Bina ID", example = "150")
    private Long id;

    @Schema(description = "Binanın Dış Kapı Numarası", example = "14/A")
    private String buildingNumber;

    @Schema(description = "10 Haneli Benzersiz Bina Kimlik Kodu", example = "1750295558")
    private String bbk;
}