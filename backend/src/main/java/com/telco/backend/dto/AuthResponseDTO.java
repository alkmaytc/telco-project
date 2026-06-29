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
@Schema(description = "Giriş/Kayıt Başarılı Yanıtı (JWT Token içerir)")
public class AuthResponseDTO {

    @Schema(description = "Bearer formatında kullanılacak güvenlik token'ı", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJvcm5layJ9...")
    private String token;

    @Schema(description = "Kullanıcının sistemdeki rolü", example = "ROLE_CUSTOMER")
    private String role;

    @Schema(description = "Arayüzde gösterilecek ad soyad", example = "Ahmet Yılmaz")
    private String fullName;
}