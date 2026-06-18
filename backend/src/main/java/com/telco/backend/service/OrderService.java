package com.telco.backend.service;

import com.telco.backend.config.RabbitMQConfig;
import com.telco.backend.dto.OrderRequestDTO;
import com.telco.backend.model.Building;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.model.Order;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import com.telco.backend.repository.OrderRepository;
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
    private final AmqpTemplate rabbitTemplate;

    @Transactional
    public Order createOrder(OrderRequestDTO request) {
        // 1. Binayı bul
        Building building = buildingRepository.findAll().stream()
                .filter(b -> b.getBbk().equals(request.getBbk()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bina bulunamadı."));

        // 2. En yakın dolabı PostGIS GIST Indeksi ve KNN (<->) gücüyle mikro saniyede bul
        InfrastructureNode closestNode = nodeRepository.findClosestNode(building.getLocation())
                .orElseThrow(() -> new IllegalStateException("Yakın dolap bulunamadı."));

        // 3. Sipariş nesnesini hazırla
        Order order = new Order();
        order.setBbk(request.getBbk());
        order.setPackageName(request.getPackageName());
        order.setSpeedMbps(request.getSpeedMbps());
        order.setPrice(request.getPrice());

        // 4. PORT KONTROLÜ VE KARAR MEKANİZMASI
        boolean hasEmptyPort = (closestNode.getTotalPorts() - closestNode.getAllocatedPorts()) > 0;
        hasEmptyPort = false; // Kuyruk test senaryomuz için aktif kalmaya devam ediyor

        if (hasEmptyPort) {
            // Port var: Siparişi anında onayla ve portu düşür
            order.setStatus("ONAYLANDI");
            closestNode.setAllocatedPorts(closestNode.getAllocatedPorts() + 1);
            nodeRepository.save(closestNode);
            orderRepository.save(order);
        } else {
            // Port yok: Durumu PORT_BEKLENIYOR yap, kaydet ve RabbitMQ kuyruğuna fırlat!
            order.setStatus("PORT_BEKLENIYOR");
            Order savedOrder = orderRepository.save(order);

            // Asenkron işlenmek üzere sadece sipariş ID'sini String olarak fırlatıyoruz (Hatasız/Akıcı)
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    savedOrder.getId().toString());
        }

        return order;
    }

    /**
     * OPERASYONEL OTOMASYON: Admin tarafından bir saha dolabının port kapasitesi artırıldığında tetiklenir.
     * İlgili dolabın kapasitesini günceller ve ardından veritabanında 'PORT_BEKLENIYOR' durumunda olan
     * ve bu dolaba en yakın olan siparişleri FIFO (İlk gelen ilk alır) mantığıyla otomatik onaylar.
     */
    @Transactional
    public InfrastructureNode updateNodeCapacityAndProcessQueue(Long nodeId, int additionalPorts) {
        // 1. İlgili saha dolabını buluyoruz
        InfrastructureNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Saha dolabı bulunamadı. ID: " + nodeId));

        // 2. Toplam port kapasitesini adminin verdiği değer kadar artırıyoruz
        node.setTotalPorts(node.getTotalPorts() + additionalPorts);
        InfrastructureNode updatedNode = nodeRepository.save(node);

        // 3. Veritabanından şu an 'PORT_BEKLENIYOR' durumundaki TÜM siparişleri en eskiden en yeniye (FIFO) çekiyoruz
        java.util.List<Order> pendingOrders = orderRepository.findAll().stream()
                .filter(o -> "PORT_BEKLENIYOR".equals(o.getStatus()))
                .sorted(java.util.Comparator.comparing(Order::getCreatedAt))
                .toList();

        // 4. Bekleyen her sipariş için bu dolabın en yakın dolap olup olmadığını ve boş port durumunu kontrol ediyoruz
        for (Order order : pendingOrders) {
            // Dolapta boş port kalmadıysa döngüden çık
            int emptyPorts = updatedNode.getTotalPorts() - updatedNode.getAllocatedPorts();
            if (emptyPorts <= 0) {
                break;
            }

            // Siparişe ait binayı bulup konumunu alıyoruz
            Building building = buildingRepository.findAll().stream()
                    .filter(b -> b.getBbk().equals(order.getBbk()))
                    .findFirst()
                    .orElse(null);

            if (building != null) {
                // PostGIS KNN sorgumuzla bu binaya en yakın dolabı tekrar buluyoruz
                InfrastructureNode closestNode = nodeRepository.findClosestNode(building.getLocation()).orElse(null);

                // Eğer binaya en yakın dolap, kapasitesini artırdığımız bu dolap ise siparişi onaylıyoruz!
                if (closestNode != null && closestNode.getId().equals(updatedNode.getId())) {
                    order.setStatus("ONAYLANDI");
                    orderRepository.save(order);

                    updatedNode.setAllocatedPorts(updatedNode.getAllocatedPorts() + 1);
                    nodeRepository.save(updatedNode);

                    System.out.println("🚀 [OTOMASYON] Port açıldı! ID: " + order.getId() + " olan sipariş otomatik ONAYLANDI durumuna getirildi.");
                }
            }
        }

        return updatedNode;
    }
}