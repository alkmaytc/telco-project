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

    /**
     * GİZLİLİK KURALI: Sadece giriş yapmış olan kullanıcının kendi siparişlerini getirir.
     */
    public List<OrderResponseDTO> getMyOrders() {
        String currentUsersEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        Customer customer = customerRepository.findByEmail(currentUsersEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Oturum açmış kullanıcı veritabanında bulunamadı."));

        return orderRepository.findByCustomerId(customer.getId()).stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    /**
     * 🎯 SİPARİŞ OLUŞTURMA VE ASENKRON KUYRUĞA FIRLATMA MOTORU
     */
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

    /**
     * 🎯 CONSUMER İÇİR ARKA PLAN İŞ MANTIĞI MOTORU
     */
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
            updateOrderStatus(order, "ONAYLANDI", "En yakın saha dolabında (" + closestNode.getName() + ") boş port tahsis edildi. Sipariş asenkron olarak onaylandı.");
        } else {
            updateOrderStatus(order, "PORT_BEKLENIYOR", "En yakın saha dolabında boş port kalmadığı için sipariş otomatik olarak port bekleme listesine alındı.");
        }
    }

    private void updateOrderStatus(Order order, String status, String description) {
        order.setStatus(status);
        orderRepository.save(order);

        historyRepository.save(new OrderStatusHistory(order.getId(), status, description));
        log.info("Sipariş Durumu Güncellendi -> ID: {} | Durum: {} | Detay: {}", order.getId(), status, description);
    }

    /**
     * ⚡ DİNAMİK KAPASİTE ARTIŞI VE PORT KUYRUĞU ERİTME OTOMASYONU
     * 🎯 OPTİMİZE EDİLDİ: findAll().stream() hafıza sızıntısı ve System.out sorunu giderildi! ✅
     */
    @Transactional
    public InfrastructureNode updateNodeCapacityAndProcessQueue(Long nodeId, int additionalPorts) {
        InfrastructureNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Saha dolabı bulunamadı. ID: " + nodeId));

        node.setTotalPorts(node.getTotalPorts() + additionalPorts);
        InfrastructureNode updatedNode = nodeRepository.save(node);

        // 🎯 MADDE 6 ÇÖZÜMÜ: Milyonlarca veriyi RAM'e çekmek yerine sadece bekleyenleri PostgreSQL seviyesinde FIFO ile çektik kanka ✅
        List<Order> pendingOrders = orderRepository.findByStatusOrderByCreatedAtAsc("PORT_BEKLENIYOR");

        for (Order order : pendingOrders) {
            int emptyPorts = updatedNode.getTotalPorts() - updatedNode.getAllocatedPorts();
            if (emptyPorts <= 0) {
                break;
            }

            Building building = buildingRepository.findByBbk(order.getBbk()).orElse(null);

            if (building != null) {
                InfrastructureNode closestNode = nodeRepository.findClosestNode(building.getLocation()).orElse(null);

                if (closestNode != null && closestNode.getId().equals(updatedNode.getId())) {
                    order.setStatus("ONAYLANDI");
                    orderRepository.save(order);

                    historyRepository.save(new OrderStatusHistory(order.getId(), "ONAYLANDI",
                            "Saha dolabına admin tarafından port eklendi. Sistem bekleyen siparişi otomatik olarak onayladı."));

                    updatedNode.setAllocatedPorts(updatedNode.getAllocatedPorts() + 1);
                    nodeRepository.save(updatedNode);

                    // 🎯 MADDE 7 ÇÖZÜMÜ: System.out.println yerine log katmanı asenkronize edildi kanka ✅
                    log.info("🚀 [OTOMASYON] Port açıldı! ID: {} olan sipariş otomatik ONAYLANDI durumuna getirildi.", order.getId());
                }
            }
        }

        return updatedNode;
    }

    /**
     * 🛡️ IDOR GÜVENLİK DUVARI EKLENDİ (Aşama 2)
     * Kullanıcının sadece kendi sipariş geçmişini görmesini garanti eder. Admin ise hepsini görebilir.
     */
    @Transactional(readOnly = true)
    public List<OrderStatusHistory> getOrderHistory(Long orderId) {
        // 1. Önce siparişi veritabanından bul
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı. ID: " + orderId));

        // 2. O an istek atan (oturum açmış) kullanıcıyı Spring Security'den yakala
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsersEmail = authentication.getName();

        // Kullanıcının rolünü al (ROLE_ADMIN veya ADMIN olabilir)
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN"));

        // 3. İstek atan müşteriyi veritabanından bul
        Customer currentCustomer = customerRepository.findByEmail(currentUsersEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Oturum açmış kullanıcı bulunamadı."));

        // 4. 🚨 IDOR KONTROLÜ: Eğer kullanıcı ADMIN değilse VE sipariş bu müşteriye ait değilse, anında kapıyı yüzüne çarp!
        if (!isAdmin && !order.getCustomer().getId().equals(currentCustomer.getId())) {
            log.warn("🚨 SİBER GÜVENLİK İHLALİ (IDOR GİRİŞİMİ): Kullanıcı ({}) başkasına ait bir siparişe (ID: {}) erişmeye çalıştı!", currentUsersEmail, orderId);
            throw new SecurityException("IDOR İhlali! Bu sipariş geçmişini görüntüleme yetkiniz yok.");
        }

        // 5. Güvenlik duvarı başarıyla geçildi, veriyi teslim et kanka ✅
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