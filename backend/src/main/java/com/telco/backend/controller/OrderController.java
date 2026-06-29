package com.telco.backend.controller;

import com.telco.backend.dto.OrderRequestDTO;
import com.telco.backend.dto.OrderResponseDTO;
import com.telco.backend.model.OrderStatusHistory;
import com.telco.backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "📦 Sipariş Yönetimi (Order API)", description = "Müşteri sipariş süreçleri, asenkron port tahsisi ve saha dolabı kapasite operasyonları")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Müşterinin Kendi Siparişlerini Getirir", description = "Sisteme giriş yapmış olan müşterinin geçmiş ve aktif tüm siparişlerini güvenli (IDOR korumalı) bir şekilde listeler.")
    @ApiResponse(responseCode = "200", description = "Siparişler başarıyla getirildi")
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @Operation(summary = "Yeni İnternet Siparişi Oluştur", description = "Müşterinin seçtiği paket için yeni bir sipariş başlatır. Sipariş anında RabbitMQ kuyruğuna aktarılır ve asenkron işlenir.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sipariş başarıyla oluşturuldu ve kuyruğa atıldı"),
            @ApiResponse(responseCode = "400", description = "Validasyon hatası veya BBK bulunamadı"),
            @ApiResponse(responseCode = "409", description = "Kullanıcının bu adreste zaten aktif bir siparişi var (Race Condition Koruması)")
    })
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO orderRequestDTO) {
        OrderResponseDTO response = orderService.createOrder(orderRequestDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Saha Dolabı Kapasite Artırımı (Otomasyon)", description = "Admin tarafından bir saha dolabına yeni port eklenmesini sağlar. Kapasite artınca 'PORT_BEKLENIYOR' statüsündeki siparişler otomatik onaylanır.")
    @ApiResponse(responseCode = "200", description = "Kapasite başarıyla artırıldı ve bekleyen siparişler işlendi")
    @PutMapping("/nodes/{nodeId}/capacity")
    public ResponseEntity<String> updateNodeCapacity(
            @Parameter(description = "Kapasitesi artırılacak saha dolabının ID'si", example = "1") @PathVariable Long nodeId,
            @Parameter(description = "Eklenecek yeni boş port sayısı", example = "5") @RequestParam int additionalPorts) {

        orderService.updateNodeCapacityAndProcessQueue(nodeId, additionalPorts);

        return ResponseEntity.ok("Saha dolabı (ID: " + nodeId + ") kapasitesi " + additionalPorts +
                " adet artırıldı ve bekleyen uygun siparişler başarıyla otomatik onaylandı!");
    }

    @Operation(summary = "Sipariş Tarihçesini Getir (Audit Log)", description = "Bir siparişin durum (status) değişikliklerini kronolojik sıra ile listeler. Adminler tümünü, müşteriler sadece kendi tarihçesini görebilir.")
    @ApiResponse(responseCode = "200", description = "Tarihçe başarıyla getirildi")
    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<OrderStatusHistory>> getOrderHistory(
            @Parameter(description = "Tarihçesi sorgulanacak Sipariş ID", example = "10") @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderHistory(orderId));
    }
}