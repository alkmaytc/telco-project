package com.telco.backend.repository;

import com.telco.backend.model.Building;
import org.locationtech.jts.geom.Point; // JTS Point import'u eklendi
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Optional import'u eklendi

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

    /**
     * YENİ - MEKANSAL YAKINLIK KÖPRÜSÜ
     * Google haritasından gelen serbest koordinata en yakın bizim sistemimizdeki 1 binayı bulur.
     * PostGIS GIST index destekli <-> operatörü sayesinde mikrosaniyeler içinde çalışır.
     */
    @Query(value = "SELECT * FROM buildings b ORDER BY b.location <-> :googlePoint LIMIT 1", nativeQuery = true)
    Optional<Building> findClosestBuildingToCoordinates(@Param("googlePoint") Point googlePoint);

    /**
     * 🎯 ADIM 1.1: OPTİMİZASYON METODU
     * Java Stream filtreleme hantallığını çözmek için BBK'ya göre doğrudan SQL indeks sorgusu atar.
     */
    Optional<Building> findByBbk(String bbk);
}