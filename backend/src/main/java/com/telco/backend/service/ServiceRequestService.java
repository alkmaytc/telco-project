package com.telco.backend.service;

import com.telco.backend.dto.ServiceRequestDTO;
import com.telco.backend.dto.ServiceRequestResponseDTO;
import com.telco.backend.model.ServiceRequest;
import com.telco.backend.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final ServiceRequestRepository repository;

    public ServiceRequestResponseDTO createRequest(ServiceRequestDTO dto, String customerEmail) {

        // 🎯 KUSURSUZ SPAM KONTROLÜ: Kim olursa olsun (anonim veya üye), bu bina için zaten BEKLEMEDE olan bir talep varsa kapıdan çevir!
        if (repository.existsByBbkAndStatus(dto.getBbk(), "BEKLEMEDE")) {
            throw new IllegalArgumentException("Bu adres için halihazırda alınmış ve değerlendirilmekte olan bir altyapı talebi bulunmaktadır.");
        }

        ServiceRequest request = new ServiceRequest();
        request.setBbk(dto.getBbk());
        request.setDistrict(dto.getDistrict());
        request.setNeighborhood(dto.getNeighborhood());
        request.setStreet(dto.getStreet());
        request.setCustomerEmail(customerEmail);

        ServiceRequest savedRequest = repository.save(request);
        log.info("🏗️ Yeni altyapı talebi alındı! BBK: {}", savedRequest.getBbk());

        return mapToResponseDTO(savedRequest);
    }

    public List<ServiceRequestResponseDTO> getAllPendingRequests() {
        return repository.findByStatusOrderByRequestDateDesc("BEKLEMEDE")
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // 🎯 YENİ EKLENDİ: Admin'in statü güncellemesi için
    public ServiceRequestResponseDTO updateRequestStatus(Long id, String newStatus) {
        ServiceRequest request = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Talep bulunamadı! ID: " + id));

        request.setStatus(newStatus);
        ServiceRequest updatedRequest = repository.save(request);

        log.info("🔧 Altyapı talep statüsü güncellendi! ID: {}, Yeni Statü: {}", id, newStatus);
        return mapToResponseDTO(updatedRequest);
    }

    private ServiceRequestResponseDTO mapToResponseDTO(ServiceRequest entity) {
        return ServiceRequestResponseDTO.builder()
                .id(entity.getId())
                .bbk(entity.getBbk())
                .district(entity.getDistrict())
                .neighborhood(entity.getNeighborhood())
                .requestDate(entity.getRequestDate())
                .status(entity.getStatus())
                .build();
    }
}