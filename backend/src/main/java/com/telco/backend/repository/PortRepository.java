package com.telco.backend.repository;

import com.telco.backend.model.Port;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortRepository extends JpaRepository<Port, Long> {

    // Belirli bir saha dolabındaki portu numarasına göre bulmak için
    Optional<Port> findByInfrastructureNodeIdAndPortNumber(Long nodeId, Integer portNumber);

    // 🎯 AKILLI ALGORİTMA İÇİN: Bir dolaptaki tüm dolu port numaralarını liste olarak getirir
    @Query("SELECT p.portNumber FROM Port p WHERE p.infrastructureNode.id = :nodeId")
    List<Integer> findAllocatedPortNumbersByNodeId(@Param("nodeId") Long nodeId);
}