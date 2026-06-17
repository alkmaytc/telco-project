package com.telco.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingResponseDTO {
    private Long id;
    private String buildingNumber;
    private String bbk;
}