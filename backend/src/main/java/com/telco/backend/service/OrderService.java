package com.telco.backend.service;

import com.telco.backend.config.RabbitMQConfig;
import com.telco.backend.dto.OrderRequestDTO;
import com.telco.backend.dto.OrderResponseDTO;
import com.telco.backend.model.*; // 🎯 Port ve PortState modelleri içeri alındı
import com.telco.backend.repository.*; // 🎯 PortRepository içeri alındı
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
    private final PortRepository portRepository; // 🎯 EKLENDİ

    /**
     * 🎯 YARDIMCI METOT: Fiziksel Port Oluşturucu ve Müşteriye Bağlayıcı
     */
    private void assignPortToCustomer(Customer customer, InfrastructureNode node) {
        Port port = new Port();
        port.setPortNumber(node.getAllocatedPorts()); // Dolabın güncel doluluk sayısını port numarası yaptık
        port.setState(Port.PortState.DOLU);
        port.setInfrastructureNode(node);

        Port savedPort = portRepository.save(port);

        customer.setPort(savedPort);
        customerRepository.save(customer);

        log.info("🔌 [PORT ATAMA] Müşteri '{}' için fiziksel Port (No: {}) saha dolabına ({}) mermer gibi çakıldı!",
                customer.getEmail(), savedPort.getPortNumber(), node.getName());
    }

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

        // 🛡️ ÇELİK YELEK (RACE CONDITION ENGELLEYİCİ) EKLENDİ
        List<String> activeStatuses = List.of("RECEIVED", "PORT_BEKLENIYOR", "ONAYLANDI");
        if (orderRepository.existsByCustomerIdAndBbkAndStatusIn(currentCustomer.getId(), request.getBbk(), activeStatuses)) {
            log.warn("🚨 [ÇİFTE SİPARİŞ ENGELLENDİ] Kullanıcı '{}', BBK '{}' için tekrar sipariş atmaya çalıştı!", currentUsersEmail, request.getBbk());
            throw new IllegalStateException("Bu adres (BBK) için zaten devam eden veya onaylanmış bir siparişiniz bulunmaktadır!");
        }

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

            // 🎯 CERRAHİ MÜDAHALE 1: Port fiziksel olarak üretildi ve müşteriye bağlandı!
            assignPortToCustomer(order.getCustomer(), closestNode);

            updateOrderStatus(order, "ONAYLANDI", "En yakın saha dolabında boş port tahsis edildi.");
            clearFeasibilityCache();
        } else {
            updateOrderStatus(order, "PORT_BEKLENIYOR", "En yakın saha dolabında boş port kalmadı.");
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

                    updatedNode.setAllocatedPorts(updatedNode.getAllocatedPorts() + 1);
                    nodeRepository.save(updatedNode);

                    // 🎯 CERRAHİ MÜDAHALE 2: Otomasyon onayında da fiziksel port üretilip bağlandı!
                    assignPortToCustomer(order.getCustomer(), updatedNode);

                    order.setStatus("ONAYLANDI");
                    orderRepository.save(order);
                    historyRepository.save(new OrderStatusHistory(order.getId(), "ONAYLANDI", "Kapasite artışı sonrası onaylandı."));

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