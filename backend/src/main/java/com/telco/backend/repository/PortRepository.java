package com.telco.backend.repository;

import com.telco.backend.model.Port;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortRepository extends JpaRepository<Port, Long> {

    // Belirli bir saha dolabındaki portu numarasına göre bulmak için (İleride lazım olabilir)
    Optional<Port> findByInfrastructureNodeIdAndPortNumber(Long nodeId, Integer portNumber);
}