package com.telco.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable; // 🎯 Import edildi

@Data
@NoArgsConstructor
@AllArgsConstructor
// 🎯 MADDE 9: Redis önbelleğine nesne olarak yazılabilmesi için Serializable interface'i eklendi kanka ✅
public class BuildingResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L; // 🎯 Kimlik numarası mühürlendi

    private Long id;
    private String buildingNumber;
    private String bbk;
}