package com.telco.backend.repository;

import com.telco.backend.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Giriş yaparken kullanıcıyı e-postasından bulmamızı sağlayacak metot
    Optional<Customer> findByEmail(String email);
}