package com.telco.backend.service;

import com.telco.backend.config.RabbitMQConfig;
import com.telco.backend.dto.OrderRequestDTO;
import com.telco.backend.dto.OrderResponseDTO;
import com.telco.backend.model.*;
import com.telco.backend.repository.*;
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
    private final PortRepository portRepository;

    /**
     * 🎯 YARDIMCI METOT: Fiziksel Port Oluşturucu ve Müşteriye Bağlayıcı (AKILLI ALGORİTMA)
     */
    private void assignPortToCustomer(Customer customer, InfrastructureNode node) {
        // 1. Dolaptaki mevcut dolu port numaralarını çek
        List<Integer> usedPorts = portRepository.findAllocatedPortNumbersByNodeId(node.getId());

        // 2. 1'den dolap kapasitesine kadar en küçük BOŞ portu (ilk eksik sayıyı) bul
        int assignedPortNumber = -1;
        for (int i = 1; i <= node.getTotalPorts(); i++) {
            if (!usedPorts.contains(i)) {
                assignedPortNumber = i;
                break;
            }
        }

        if (assignedPortNumber == -1) {
            // Bu durum matematiksel olarak imkansız olmalı (öncesinde boş port kontrolü yapıyoruz) ama zırhlayalım.
            throw new IllegalStateException("Saha dolabında fiziksel olarak atanabilecek boş port numarası bulunamadı!");
        }

        Port port = new Port();
        port.setPortNumber(assignedPortNumber); // 🎯 Artık körü körüne allocatedPorts değil, akıllı boş port numarasını basıyoruz!
        port.setState(Port.PortState.DOLU);
        port.setInfrastructureNode(node);

        Port savedPort = portRepository.save(port);

        customer.setPort(savedPort);
        customerRepository.save(customer);

        log.info("🔌 [AKILLI PORT ATAMA] Müşteri '{}' için fiziksel Port (No: {}) saha dolabına ({}) mermer gibi çakıldı! (Fragmentation engellendi)",
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

        // 🛡️ ÇELİK YELEK (RACE CONDITION ENGELLEYİCİ)
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

            // 🎯 CERRAHİ MÜDAHALE 1: Akıllı algoritma ile port fiziksel olarak üretildi ve müşteriye bağlandı!
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

                    // 🎯 CERRAHİ MÜDAHALE 2: Otomasyon onayında da akıllı port üretilip bağlandı!
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

    /**
     * 🛑 SİPARİŞ İPTAL SERVİSİ (A ve B Senaryolarını İçerir)
     */
    @Transactional
    public String cancelOrder(Long orderId) {
        // 1. Güvenlik: Oturum açan müşteriyi bul
        String currentUsersEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Siparişi bul ve sahibini doğrula
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı! ID: " + orderId));

        if (order.getCustomer() == null || !order.getCustomer().getEmail().equals(currentUsersEmail)) {
            log.warn("🚨 [YETKİSİZ İŞLEM] {} kullanıcısı, başkasına ait {} ID'li siparişi iptal etmeye çalıştı!", currentUsersEmail, orderId);
            throw new SecurityException("Bu siparişi iptal etme yetkiniz bulunmuyor!");
        }

        String currentStatus = order.getStatus();

        // 3. Zaten iptal edilmişse işlemi durdur
        if ("IPTAL_EDILDI".equals(currentStatus)) {
            throw new IllegalStateException("Bu sipariş zaten iptal edilmiş!");
        }

        Customer customer = order.getCustomer();

        // ---------------------------------------------------------
        // SENARYO A: Sipariş henüz onaylanmamış (Kuyrukta/Beklemede)
        // ---------------------------------------------------------
        if ("RECEIVED".equals(currentStatus) || "PORT_BEKLENIYOR".equals(currentStatus)) {
            order.setStatus("IPTAL_EDILDI");
            orderRepository.save(order);

            saveOrderHistory(order, "Sipariş müşteri tarafından iptal edildi. (Port tahsisi yapılmadan önce)");
            log.info("🚫 [SİPARİŞ İPTAL] Sipariş ID: {} iptal edildi. (Kuyruktaydı/Bekliyordu)", orderId);
        }

        // ---------------------------------------------------------
        // SENARYO B: Sipariş onaylanmış ve fiziksel port takılmış
        // ---------------------------------------------------------
        else if ("ONAYLANDI".equals(currentStatus)) {
            order.setStatus("IPTAL_EDILDI");
            orderRepository.save(order);

            // Fiziksel portu ve kapasiteyi iade etme operasyonu
            if (customer.getPort() != null) {
                Port port = customer.getPort();
                InfrastructureNode node = port.getInfrastructureNode();

                // 1. Saha dolabının dolu port kapasitesini 1 azalt
                node.setAllocatedPorts(node.getAllocatedPorts() - 1);
                nodeRepository.save(node);

                // 2. Müşteri ile portun bağını kopar
                customer.setPort(null);
                customerRepository.save(customer);

                // 3. Portu sistemden tamamen sil (Senin akıllı algoritman yeni port create ettiği için silmek en temizi)
                portRepository.delete(port);

                log.info("♻️ [PORT İADE] Sipariş ID: {} iptal edildi. {} dolabından Port No: {} boşa çıkarıldı.",
                        orderId, node.getName(), port.getPortNumber());
            }

            saveOrderHistory(order, "Sipariş müşteri tarafından iptal edildi. Bağlı olan fiziksel port altyapıya iade edildi.");
        } else {
            throw new IllegalStateException("İptal işlemi için uygun olmayan sipariş durumu: " + currentStatus);
        }

        // Redis cache temizliği (Saha dolabı fizibilitesi etkilendiği için)
        clearFeasibilityCache();

        return "Siparişiniz başarıyla iptal edilmiştir.";
    }

    /**
     * 🎯 YARDIMCI METOT: Tarihçe Kaydı İçin
     */
    private void saveOrderHistory(Order order, String note) {
        // Senin yazdığın o harika constructor'ı kullanıyoruz!
        OrderStatusHistory history = new OrderStatusHistory(order.getId(), "IPTAL_EDILDI", note);
        historyRepository.save(history);
    }
}