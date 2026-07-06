package com.telco.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Müşteri Profil Detay Nesnesi")
public class UserProfileDTO {

    @Schema(description = "Müşterinin Adı", example = "Ahmet")
    private String firstName;

    @Schema(description = "Müşterinin Soyadı", example = "Yılmaz")
    private String lastName;

    @Schema(description = "Müşterinin e-posta adresi", example = "ahmet.yilmaz@telco.com")
    private String email;
}