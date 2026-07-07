package com.telco.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Altyapı Talep Yanıt/Listeleme Nesnesi")
public class ServiceRequestResponseDTO {

    private Long id;
    private String bbk;
    private String district;
    private String neighborhood;
    private LocalDateTime requestDate;
    private String status;
}