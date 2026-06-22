package com.telco.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {
    private String identityNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    // CBS (PostGIS) için haritadan seçilen koordinatlar
    private Double latitude;
    private Double longitude;
}