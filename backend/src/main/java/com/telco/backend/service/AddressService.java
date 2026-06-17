package com.telco.backend.service;

import com.telco.backend.dto.BuildingResponseDTO;
import com.telco.backend.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final BuildingRepository buildingRepository;

    // 1. İlçeleri servis katmanına taşır
    public List<String> getDistricts() {
        return buildingRepository.findDistinctDistricts();
    }

    // 2. Mahalleleri servis katmanına taşır
    public List<String> getNeighborhoods(String district) {
        return buildingRepository.findDistinctNeighborhoodsByDistrict(district);
    }

    // 3. Sokakları servis katmanına taşır
    public List<String> getStreets(String district, String neighborhood) {
        return buildingRepository.findDistinctStreetsByDistrictAndNeighborhood(district, neighborhood);
    }

    // 4. Binaları alır ve Stream API kullanarak BuildingResponseDTO listesine dönüştürür
    public List<BuildingResponseDTO> getBuildings(String district, String neighborhood, String street) {
        return buildingRepository.findBuildingsByAddress(district, neighborhood, street)
                .stream()
                .map(b -> new BuildingResponseDTO(b.getId(), b.getBuildingNumber(), b.getBbk()))
                .collect(Collectors.toList());
    }
}