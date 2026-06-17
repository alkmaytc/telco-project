package com.telco.backend.repository;

import com.telco.backend.model.InfrastructureNode;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InfrastructureNodeRepository extends JpaRepository<InfrastructureNode, Long> {

    /**
     * PostGIS kullanarak verilen bir binanın koordinatına en yakın saha dolabını bulur.
     * ST_DistanceSphere: İki coğrafi koordinat (WGS84) arasındaki mesafeyi metre cinsinden kuş uçuşu hesaplar.
     * LIMIT 1: En yakın olan tek bir dolabı getirir.
     */
    @Query(value = "SELECT * FROM infrastructure_nodes n " +
            "ORDER BY ST_DistanceSphere(n.location, :buildingLocation) " +
            "LIMIT 1", nativeQuery = true)
    Optional<InfrastructureNode> findClosestNode(@Param("buildingLocation") Point buildingLocation);
}