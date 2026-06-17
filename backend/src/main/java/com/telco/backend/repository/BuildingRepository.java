package com.telco.backend.repository;

import com.telco.backend.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    // 1. Sistemdeki benzersiz ilçeleri alfabetik getirir (Odunpazarı, Tepebaşı)
    @Query("SELECT DISTINCT b.district FROM Building b ORDER BY b.district")
    List<String> findDistinctDistricts();

    // 2. Seçilen ilçeye ait benzersiz mahalleleri getirir
    @Query("SELECT DISTINCT b.neighborhood FROM Building b WHERE b.district = :district ORDER BY b.neighborhood")
    List<String> findDistinctNeighborhoodsByDistrict(@Param("district") String district);

    // 3. Seçilen ilçe ve mahalleye ait benzersiz sokakları getirir
    @Query("SELECT DISTINCT b.street FROM Building b WHERE b.district = :district AND b.neighborhood = :neighborhood ORDER BY b.street")
    List<String> findDistinctStreetsByDistrictAndNeighborhood(@Param("district") String district, @Param("neighborhood") String neighborhood);

    // 4. Seçilen sokağa ait tüm binaları getirir
    @Query("SELECT b FROM Building b WHERE b.district = :district AND b.neighborhood = :neighborhood AND b.street = :street ORDER BY b.buildingNumber")
    List<Building> findBuildingsByAddress(
            @Param("district") String district,
            @Param("neighborhood") String neighborhood,
            @Param("street") String street);
}