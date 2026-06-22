package com.telco.backend.controller;

import com.telco.backend.dto.OrderRequestDTO;
import com.telco.backend.dto.OrderResponseDTO; // Yeni DTO import edildi kanka ✅
import com.telco.backend.model.OrderStatusHistory;
import com.telco.backend.service.OrderService;
import jakarta.validation.Valid; // Validasyon tetikleyicisi aktif kanka ✅
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
// 🎯 MADDE 8 TEMİZLİĞİ: Global SecurityConfig'den yönetmek için @CrossOrigin kaldırıldı kanka ✅
public class OrderController {

    private final OrderService orderService;

    /**
     * GİZLİLİK KURALI: Giriş yapmış olan müşterinin sadece kendine ait siparişlerini listeler.
     * GET http://localhost:8080/api/v1/orders/my-orders
     */
    @GetMapping("/my-orders")
    // 🎯 MADDE 6 / KVKK: List<Order> yerine siber güvenli List<OrderResponseDTO> dönüyoruz ✅
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    /**
     * Yeni Sipariş Oluşturma Endpoint'i
     * POST http://localhost:8080/api/v1/orders
     */
    @PostMapping
    // 🎯 MADDE 10 / MADDE 6: Girişte @Valid koruması, çıkışta ise güvenli OrderResponseDTO dönüşü ✅
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO orderRequestDTO) {
        OrderResponseDTO response = orderService.createOrder(orderRequestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * OPERASYONEL ENDPOINT: Admin paneli üzerinden saha dolabının kapasitesini artırır.
     * PUT http://localhost:8080/api/v1/orders/nodes/{nodeId}/capacity?additionalPorts=5
     */
    @PutMapping("/nodes/{nodeId}/capacity")
    public ResponseEntity<String> updateNodeCapacity(
            @PathVariable Long nodeId,
            @RequestParam int additionalPorts) {

        orderService.updateNodeCapacityAndProcessQueue(nodeId, additionalPorts);

        return ResponseEntity.ok("Saha dolabı (ID: " + nodeId + ") kapasitesi " + additionalPorts +
                " adet artırıldı ve bekleyen uygun siparişler başarıyla otomatik onaylandı!");
    }

    /**
     * AUDIT LOG ENDPOINT: Belirli bir siparişin başından geçen tüm tarihçeyi kronolojik olarak listeler.
     * GET http://localhost:8080/api/v1/orders/{orderId}/history
     */
    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<OrderStatusHistory>> getOrderHistory(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderHistory(orderId));
    }
}