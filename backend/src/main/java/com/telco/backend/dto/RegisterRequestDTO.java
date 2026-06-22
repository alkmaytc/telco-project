package com.telco.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    // 🎯 T.C. Kimlik: Tam 11 hane olmalı ve sadece rakamlardan oluşmalı kanka
    @NotBlank(message = "T.C. Kimlik numarası boş bırakılamaz.")
    @Size(min = 11, max = 11, message = "T.C. Kimlik numarası tam 11 haneli olmalıdır.")
    @Pattern(regexp = "^[0-9]+$", message = "T.C. Kimlik numarası sadece rakamlardan oluşmalıdır.")
    private String identityNumber;

    @NotBlank(message = "İsim alanı boş bırakılamaz.")
    @Size(min = 2, max = 50, message = "İsim en az 2, en fazla 50 karakter olabilir.")
    private String firstName;

    @NotBlank(message = "Soyisim alanı boş bırakılamaz.")
    @Size(min = 2, max = 50, message = "Soyisim en az 2, en fazla 50 karakter olabilir.")
    private String lastName;

    // 🎯 E-posta standardı RFC 5322'ye göre kapıda doğrulanıyor kanka
    @NotBlank(message = "E-posta adresi boş bırakılamaz.")
    @Email(message = "Lütfen geçerli bir e-posta adresi giriniz.")
    private String email;

    @NotBlank(message = "Şifre alanı boş bırakılamaz.")
    @Size(min = 6, max = 100, message = "Şifre güvenlik nedeniyle en az 6 karakter olmalıdır.")
    private String password;

    // 🎯 PostGIS CBS Validasyonu: Türkiye koordinat sınırları dışındaki saçma verileri engeller (-90 ile +90 arası)
    @NotNull(message = "Enlem (Latitude) koordinatı boş bırakılamaz.")
    @Min(value = -90, message = "Geçersiz enlem koordinatı.")
    @Max(value = 90, message = "Geçersiz enlem koordinatı.")
    private Double latitude;

    // Boylam validasyonu (-180 ile +180 arası)
    @NotNull(message = "Boylam (Longitude) koordinatı boş bırakılamaz.")
    @Min(value = -180, message = "Geçersiz boylam koordinatı.")
    @Max(value = 180, message = "Geçersiz boylam koordinatı.")
    private Double longitude;
}