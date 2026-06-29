package com.telco.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sisteme Giriş (Login) İstek Nesnesi")
public class LoginRequestDTO {

    @Schema(description = "Müşterinin sisteme kayıtlı e-posta adresi", example = "ornek@telco.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "E-posta adresi boş bırakılamaz.")
    @Email(message = "Lütfen geçerli bir e-posta adresi giriniz.")
    private String email;

    @Schema(description = "Müşterinin hesap şifresi", example = "GizliSifre123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Şifre alanı boş bırakılamaz.")
    private String password;
}