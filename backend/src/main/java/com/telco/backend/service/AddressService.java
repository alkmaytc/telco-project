package com.telco.backend.service;

import com.telco.backend.dto.BuildingResponseDTO;
import com.telco.backend.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable; // Cache kütüphanesi eklendi kanka ✅
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final BuildingRepository buildingRepository;

    /**
     * 1. Tüm ilçeleri getirir.
     * 🎯 REDIS: Parametre olmadığı için sabit bir cache key ile RAM'e kilitlenir kanka ✅
     */
    @Cacheable(value = "districts", key = "'all'")
    public List<String> getDistricts() {
        return buildingRepository.findDistinctDistricts();
    }

    /**
     * 2. Seçilen ilçeye ait mahalleleri getirir.
     * 🎯 REDIS: Gelen 'district' parametresine göre dinamik cache'lenir (Örn: districts::Eskisehir) ✅
     */
    @Cacheable(value = "neighborhoods", key = "#district")
    public List<String> getNeighborhoods(String district) {
        return buildingRepository.findDistinctNeighborhoodsByDistrict(district);
    }

    /**
     * 3. Seçilen ilçe ve mahalleye ait sokakları getirir.
     * 🎯 REDIS: İki parametreyi birleştirerek benzersiz bir cache key üretir kanka ✅
     */
    @Cacheable(value = "streets", key = "#district + '_' + #neighborhood")
    public List<String> getStreets(String district, String neighborhood) {
        return buildingRepository.findDistinctStreetsByDistrictAndNeighborhood(district, neighborhood);
    }

    /**
     * 4. Binaları alır ve BuildingResponseDTO listesine dönüştürür.
     * 🎯 REDIS: Üçlü parametre kombinasyonuna göre nihai bina listesini ön belleğe alır ✅
     */
    //@Cacheable(value = "buildings", key = "#district + '_' + #neighborhood + '_' + #street")
    public List<BuildingResponseDTO> getBuildings(String district, String neighborhood, String street) {
        // MODERNİZASYON: Hantal Collectors.toList() yerine modern .toList() kullanımına geçildi kanka ✅
        return buildingRepository.findBuildingsByAddress(district, neighborhood, street)
                .stream()
                .map(b -> new BuildingResponseDTO(b.getId(), b.getBuildingNumber(), b.getBbk()))
                .toList();
    }
}