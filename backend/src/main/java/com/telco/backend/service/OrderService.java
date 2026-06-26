package com.telco.backend.service;

import com.telco.backend.config.RabbitMQConfig;
import com.telco.backend.dto.OrderRequestDTO;
import com.telco.backend.dto.OrderResponseDTO;
import com.telco.backend.model.Building;
import com.telco.backend.model.Customer;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.model.Order;
import com.telco.backend.model.OrderStatusHistory;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.CustomerRepository;
import com.telco.backend.repository.OrderRepository;
import com.telco.backend.repository.OrderStatusHistoryRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final BuildingRepository buildingRepository;
    private final InfrastructureNodeRepository nodeRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final AmqpTemplate rabbitTemplate;
    private final CacheManager cacheManager;

    /**
     * 🎯 NÜKLEER ÇÖZÜM: Tüm fizibilite önbelleğini temizler.
     * Bir dolabın kapasitesi değiştiğinde, tüm bölge binasının verisi eski kalabileceği için
     * tek tek evict yerine tüm cache'i temizliyoruz.
     */
    private void clearFeasibilityCache() {
        try {
            if (cacheManager.getCache("feasibility_bbk") != null) {
                cacheManager.getCache("feasibility_bbk").clear();
                log.info("🧹 [REDIS CLEAR] Tüm fizibilite önbelleği başarıyla sıfırlandı.");
            }
        } catch (Exception e) {
            log.error("❌ [REDIS CLEAR ERROR] Önbellek temizlenirken hata oluştu!", e);
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrders() {
        String currentUsersEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        Customer customer = customerRepository.findByEmail(currentUsersEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Oturum açmış kullanıcı veritabanında bulunamadı."));

        return orderRepository.findByCustomerId(customer.getId()).stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        buildingRepository.findByBbk(request.getBbk())
                .orElseThrow(() -> new IllegalArgumentException("Sipariş verilmek istenen bina bulunamadı. BBK: " + request.getBbk()));

        String currentUsersEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Customer currentCustomer = customerRepository.findByEmail(currentUsersEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Sipariş veren kullanıcı oturumu bulunamadı."));

        Order order = new Order();
        order.setBbk(request.getBbk());
        order.setPackageName(request.getPackageName());
        order.setSpeedMbps(request.getSpeedMbps());
        order.setPrice(request.getPrice());
        order.setCustomer(currentCustomer);
        order.setStatus("RECEIVED");

        Order savedOrder = orderRepository.save(order);

        historyRepository.save(new OrderStatusHistory(
                savedOrder.getId(),
                "RECEIVED",
                "Sipariş sistem tarafından alındı, asenkron port kontrolü için RabbitMQ kuyruğuna iletildi."
        ));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                savedOrder.getId().toString()
        );

        log.info("Sipariş başarıyla alındı ve kuyruğa atıldı. ID: {} | BBK: {}", savedOrder.getId(), savedOrder.getBbk());

        return convertToResponseDTO(savedOrder);
    }

    @Transactional
    public void processOrderFromQueue(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Kuyruktan gelen sipariş veritabanında bulunamadı! ID: " + orderId));

        if (!"RECEIVED".equals(order.getStatus())) {
            return;
        }

        Building building = buildingRepository.findByBbk(order.getBbk()).orElse(null);
        if (building == null) {
            updateOrderStatus(order, "IPTAL", "Bina lokasyon verisi bulunamadığı için sipariş iptal edildi.");
            return;
        }

        InfrastructureNode closestNode = nodeRepository.findClosestNode(building.getLocation()).orElse(null);
        if (closestNode == null) {
            updateOrderStatus(order, "IPTAL", "Binaya hizmet verebilecek menzilde altyapı saha dolabı bulunamadı.");
            return;
        }

        boolean hasEmptyPort = (closestNode.getTotalPorts() - closestNode.getAllocatedPorts()) > 0;

        if (hasEmptyPort) {
            closestNode.setAllocatedPorts(closestNode.getAllocatedPorts() + 1);
            nodeRepository.save(closestNode);
            updateOrderStatus(order, "ONAYLANDI", "En yakın saha dolabında boş port tahsis edildi.");

            // 🎯 Önbelleği temizle
            clearFeasibilityCache();
        } else {
            updateOrderStatus(order, "PORT_BEKLENIYOR", "En yakın saha dolabında boş port kalmadı.");

            // 🎯 Önbelleği temizle
            clearFeasibilityCache();
        }
    }

    private void updateOrderStatus(Order order, String status, String description) {
        order.setStatus(status);
        orderRepository.save(order);

        historyRepository.save(new OrderStatusHistory(order.getId(), status, description));
        log.info("Sipariş Durumu Güncellendi -> ID: {} | Durum: {} | Detay: {}", order.getId(), status, description);
    }

    @Transactional
    public InfrastructureNode updateNodeCapacityAndProcessQueue(Long nodeId, int additionalPorts) {
        InfrastructureNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Saha dolabı bulunamadı. ID: " + nodeId));

        node.setTotalPorts(node.getTotalPorts() + additionalPorts);
        InfrastructureNode updatedNode = nodeRepository.save(node);

        List<Order> pendingOrders = orderRepository.findByStatusOrderByCreatedAtAsc("PORT_BEKLENIYOR");

        for (Order order : pendingOrders) {
            int emptyPorts = updatedNode.getTotalPorts() - updatedNode.getAllocatedPorts();
            if (emptyPorts <= 0) break;

            Building building = buildingRepository.findByBbk(order.getBbk()).orElse(null);
            if (building != null) {
                InfrastructureNode closestNode = nodeRepository.findClosestNode(building.getLocation()).orElse(null);
                if (closestNode != null && closestNode.getId().equals(updatedNode.getId())) {
                    order.setStatus("ONAYLANDI");
                    orderRepository.save(order);
                    historyRepository.save(new OrderStatusHistory(order.getId(), "ONAYLANDI", "Kapasite artışı sonrası onaylandı."));
                    updatedNode.setAllocatedPorts(updatedNode.getAllocatedPorts() + 1);
                    nodeRepository.save(updatedNode);

                    // 🎯 Önbelleği temizle
                    clearFeasibilityCache();
                }
            }
        }
        return updatedNode;
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistory> getOrderHistory(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı. ID: " + orderId));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsersEmail = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        Customer currentCustomer = customerRepository.findByEmail(currentUsersEmail).orElseThrow();

        if (!isAdmin && !order.getCustomer().getId().equals(currentCustomer.getId())) {
            throw new SecurityException("IDOR İhlali!");
        }
        return historyRepository.findByOrderIdOrderByChangedAtAsc(orderId);
    }

    private OrderResponseDTO convertToResponseDTO(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .bbk(order.getBbk())
                .packageName(order.getPackageName())
                .speedMbps(order.getSpeedMbps())
                .price(order.getPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .customerFullName(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName())
                .build();
    }
}