package com.telco.backend.service;

import com.telco.backend.dto.UserProfileDTO;
import com.telco.backend.model.Customer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    @PersistenceContext
    private final EntityManager entityManager;

    public UserProfileDTO getProfile(Customer currentCustomer) {
        return UserProfileDTO.builder()
                .firstName(currentCustomer.getFirstName())
                .lastName(currentCustomer.getLastName())
                .email(currentCustomer.getEmail())
                .identityNumber(currentCustomer.getIdentityNumber())
                .build();
    }

    @Transactional
    public UserProfileDTO updateProfile(Customer currentCustomer, UserProfileDTO dto) {

        // 1. E-posta eşsizliği kontrolü (Saf JPQL sorgusu)
        if (!currentCustomer.getEmail().equalsIgnoreCase(dto.getEmail())) {
            List<Long> existingUsers = entityManager.createQuery(
                            "SELECT c.id FROM Customer c WHERE LOWER(c.email) = LOWER(:email) AND c.id != :currentId", Long.class)
                    .setParameter("email", dto.getEmail().trim())
                    .setParameter("currentId", currentCustomer.getId())
                    .getResultList();

            if (!existingUsers.isEmpty()) {
                // 🎯 ÇÖZÜM: Parametre krizine giren BusinessException yerine
                // Java'nın saf ve tek parametre kabul eden hazır hatasını fırlatıyoruz!
                throw new IllegalArgumentException("Bu e-posta adresi zaten baska bir kullanici tarafindan kullaniliyor!");
            }
        }

        // 2. Saf SQL/JPQL ile doğrudan sütunları güncelliyoruz
        entityManager.createQuery(
                        "UPDATE Customer c SET c.firstName = :firstName, c.lastName = :lastName, c.email = :email WHERE c.id = :id")
                .setParameter("firstName", dto.getFirstName().trim())
                .setParameter("lastName", dto.getLastName().trim())
                .setParameter("email", dto.getEmail().trim())
                .setParameter("id", currentCustomer.getId())
                .executeUpdate();

        return UserProfileDTO.builder()
                .firstName(dto.getFirstName().trim())
                .lastName(dto.getLastName().trim())
                .email(dto.getEmail().trim())
                .identityNumber(currentCustomer.getIdentityNumber())
                .build();
    }
}