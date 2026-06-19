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
     * ULTRA OPTİMİZASYON: DBeaver'da oluşturduğumuz GIST indeksini tetikleyen
     * PostGIS KNN (<->) operatörünü kullanıyoruz.
     * ST_DistanceSphere gibi tüm tabloyu taramak yerine, indeks ağacı üzerinden
     * en yakın dolabı mikro saniyeler (µs) içinde şak diye bulur.
     */
    @Query(value = "SELECT * FROM infrastructure_nodes n " +
            "ORDER BY n.location <-> :buildingLocation " +
            "LIMIT 1", nativeQuery = true)
    Optional<InfrastructureNode> findClosestNode(@Param("buildingLocation") Point buildingLocation);

    // InfrastructureNodeRepository.java içerisine eklenecek sorgu:
    @Query(value = "SELECT * FROM infrastructure_nodes n " +
            "WHERE (n.total_ports - n.allocated_ports) > 0 " +
            "ORDER BY n.location <-> :buildingLoc LIMIT 1", nativeQuery = true)
    Optional<InfrastructureNode> findClosestNodeWithEmptyPort(@Param("buildingLoc") Point buildingLoc);
}