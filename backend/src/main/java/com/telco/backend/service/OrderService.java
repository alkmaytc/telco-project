package com.telco.backend.service;

import com.telco.backend.config.RabbitMQConfig;
import com.telco.backend.dto.OrderRequestDTO;
import com.telco.backend.model.Building;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.model.Order;
import com.telco.backend.model.OrderStatusHistory;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import com.telco.backend.repository.OrderRepository;
import com.telco.backend.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BuildingRepository buildingRepository;
    private final InfrastructureNodeRepository nodeRepository;
    private final OrderStatusHistoryRepository historyRepository; // Yeni Repository
    private final AmqpTemplate rabbitTemplate;

    @Transactional
    public Order createOrder(OrderRequestDTO request) {
        Building building = buildingRepository.findAll().stream()
                .filter(b -> b.getBbk().equals(request.getBbk()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bina bulunamadı."));

        InfrastructureNode closestNode = nodeRepository.findClosestNode(building.getLocation())
                .orElseThrow(() -> new IllegalStateException("Yakın dolap bulunamadı."));

        Order order = new Order();
        order.setBbk(request.getBbk());
        order.setPackageName(request.getPackageName());
        order.setSpeedMbps(request.getSpeedMbps());
        order.setPrice(request.getPrice());

        boolean hasEmptyPort = (closestNode.getTotalPorts() - closestNode.getAllocatedPorts()) > 0;
        hasEmptyPort = false; // Test senaryomuz için aktif

        if (hasEmptyPort) {
            order.setStatus("ONAYLANDI");
            closestNode.setAllocatedPorts(closestNode.getAllocatedPorts() + 1);
            nodeRepository.save(closestNode);
            Order savedOrder = orderRepository.save(order);

            // AUDIT LOG: Doğrudan Onaylanma Tarihçesi
            historyRepository.save(new OrderStatusHistory(savedOrder.getId(), "ONAYLANDI",
                    "Mesafe bazlı en yakın saha dolabında boş port bulundu. Sipariş anında onaylandı."));
        } else {
            order.setStatus("PORT_BEKLENIYOR");
            Order savedOrder = orderRepository.save(order);

            // AUDIT LOG: Port Bekleme Moduna Alınma Tarihçesi
            historyRepository.save(new OrderStatusHistory(savedOrder.getId(), "PORT_BEKLENIYOR",
                    "En yakın saha dolabında boş port kalmadığı için sipariş bekleme havuzuna alındı."));

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    savedOrder.getId().toString());
        }

        return order;
    }

    @Transactional
    public InfrastructureNode updateNodeCapacityAndProcessQueue(Long nodeId, int additionalPorts) {
        InfrastructureNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Saha dolabı bulunamadı. ID: " + nodeId));

        node.setTotalPorts(node.getTotalPorts() + additionalPorts);
        InfrastructureNode updatedNode = nodeRepository.save(node);

        java.util.List<Order> pendingOrders = orderRepository.findAll().stream()
                .filter(o -> "PORT_BEKLENIYOR".equals(o.getStatus()))
                .sorted(java.util.Comparator.comparing(Order::getCreatedAt))
                .toList();

        for (Order order : pendingOrders) {
            int emptyPorts = updatedNode.getTotalPorts() - updatedNode.getAllocatedPorts();
            if (emptyPorts <= 0) {
                break;
            }

            Building building = buildingRepository.findAll().stream()
                    .filter(b -> b.getBbk().equals(order.getBbk()))
                    .findFirst()
                    .orElse(null);

            if (building != null) {
                InfrastructureNode closestNode = nodeRepository.findClosestNode(building.getLocation()).orElse(null);

                if (closestNode != null && closestNode.getId().equals(updatedNode.getId())) {
                    order.setStatus("ONAYLANDI");
                    orderRepository.save(order);

                    // AUDIT LOG: Admin Tarafından Otomatik Onaylanma Tarihçesi
                    historyRepository.save(new OrderStatusHistory(order.getId(), "ONAYLANDI",
                            "Saha dolabına admin tarafından port eklendi. Sistem bekleyen siparişi otomatik olarak onayladı."));

                    updatedNode.setAllocatedPorts(updatedNode.getAllocatedPorts() + 1);
                    nodeRepository.save(updatedNode);

                    System.out.println("🚀 [OTOMASYON] Port açıldı! ID: " + order.getId() + " olan sipariş otomatik ONAYLANDI durumuna getirildi.");
                }
            }
        }

        return updatedNode;
    }
    /**
     * AUDIT LOG: Belirli bir siparişe ait tarihçe kayıtlarını getirir.
     */
    public java.util.List<OrderStatusHistory> getOrderHistory(Long orderId) {
        return historyRepository.findByOrderIdOrderByChangedAtAsc(orderId);
    }
}