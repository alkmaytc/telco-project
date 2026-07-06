package com.telco.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeni Müşteri Kayıt (Register) İstek Nesnesi")
public class RegisterRequestDTO {

    // 🎯 T.C. KİMLİK NUMARASI ALANI VE VALİDASYONLARI BURADAN TAMAMEN SİLİNDİ!

    @Schema(description = "Müşterinin Adı", example = "Ahmet", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "İsim alanı boş bırakılamaz.")
    @Size(min = 2, max = 50, message = "İsim en az 2, en fazla 50 karakter olabilir.")
    private String firstName;

    @Schema(description = "Müşterinin Soyadı", example = "Yılmaz", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Soyisim alanı boş bırakılamaz.")
    @Size(min = 2, max = 50, message = "Soyisim en az 2, en fazla 50 karakter olabilir.")
    private String lastName;

    @Schema(description = "Geçerli e-posta adresi (RFC 5322 uyumlu)", example = "ahmet.yilmaz@telco.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "E-posta adresi boş bırakılamaz.")
    @Email(message = "Lütfen geçerli bir e-posta adresi giriniz.")
    private String email;

    @Schema(description = "Hesap şifresi (Min 6 karakter)", example = "GizliSifre123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Şifre alanı boş bırakılamaz.")
    @Size(min = 6, max = 100, message = "Şifre güvenlik nedeniyle en az 6 karakter olmalıdır.")
    private String password;

    @Schema(description = "Müşterinin GPS Enlem (Latitude) koordinatı. PostGIS yedekleme algoritması (Crowdsourced Healing) için kullanılır.", example = "39.7731", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Enlem (Latitude) koordinatı boş bırakılamaz.")
    @Min(value = -90, message = "Geçersiz enlem koordinatı.")
    @Max(value = 90, message = "Geçersiz enlem koordinatı.")
    private Double latitude;

    @Schema(description = "Müşterinin GPS Boylam (Longitude) koordinatı. PostGIS yedekleme algoritması (Crowdsourced Healing) için kullanılır.", example = "30.5206", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Boylam (Longitude) koordinatı boş bırakılamaz.")
    @Min(value = -180, message = "Geçersiz boylam koordinatı.")
    @Max(value = 180, message = "Geçersiz boylam koordinatı.")
    private Double longitude;
}