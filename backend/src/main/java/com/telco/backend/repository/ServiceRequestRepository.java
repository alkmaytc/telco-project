package com.telco.backend.repository;

import com.telco.backend.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    // Admin paneli için belirli statüdeki (örneğin BEKLEMEDE) talepleri listeler
    List<ServiceRequest> findByStatusOrderByRequestDateDesc(String status);

    // 🎯 YENİ ZIRH: Aynı BBK için BEKLEMEDE olan bir talep var mı? (Kim attığından bağımsız)
    boolean existsByBbkAndStatus(String bbk, String status);

    // İleride admin dashboard'unda "Hangi ilçede kaç talep var" istatistiği için
    long countByDistrict(String district);
}