package com.telco.backend.controller;

import com.telco.backend.dto.OrderRequestDTO;
import com.telco.backend.model.Order;
import com.telco.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Frontend erişim izni
public class OrderController {

    private final OrderService orderService;

    /**
     * Yeni Sipariş Oluşturma Endpoint'i
     * POST http://localhost:8080/api/v1/orders
     */
    // Mevcut @PostMapping anotasyonunun olduğu metodu bul ve sadece içini bu şekilde sar:

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody com.telco.backend.dto.OrderRequestDTO orderRequestDTO) {
        try {
            // Servise doğru DTO nesnesini paslıyoruz
            com.telco.backend.model.Order response = orderService.createOrder(orderRequestDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Kilitlenme hatasını konsolda görmek için logluyoruz
            e.printStackTrace();

            // Terminalin boş kalmasını engelleyen kurumsal hata dönüşü
            return ResponseEntity.status(500).body("Backend Hatası: " + e.getMessage());
        }
    }
}