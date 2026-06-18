package com.telco.backend.controller;

import com.telco.backend.dto.OrderRequestDTO;
import com.telco.backend.model.Order;
import com.telco.backend.model.InfrastructureNode;
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
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequestDTO orderRequestDTO) {
        try {
            Order response = orderService.createOrder(orderRequestDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Backend Hatası: " + e.getMessage());
        }
    }

    /**
     * OPERASYONEL ENDPOINT: Admin paneli üzerinden saha dolabının kapasitesini artırır.
     * Kapasite artışı sağlandığı an arka planda bekleyen siparişler otomatik olarak tetiklenip onaylanır.
     * * PUT http://localhost:8080/api/v1/orders/nodes/{nodeId}/capacity?additionalPorts=5
     */
    @PutMapping("/nodes/{nodeId}/capacity")
    public ResponseEntity<String> updateNodeCapacity(
            @PathVariable Long nodeId,
            @RequestParam int additionalPorts) {

        orderService.updateNodeCapacityAndProcessQueue(nodeId, additionalPorts);

        return ResponseEntity.ok("Saha dolabı (ID: " + nodeId + ") kapasitesi " + additionalPorts +
                " adet artırıldı ve bekleyen uygun siparişler başarıyla otomatik onaylandı!");
    }
}