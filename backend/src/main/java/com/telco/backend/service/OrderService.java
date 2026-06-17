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

        // 2. En yakın dolabı bul
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
        //hasEmptyPort = false; // TEST İÇİN KUYRUĞU ZORLUYORUZ testte aldığımız hata yüzünden bu satırı kullandım.

        if (hasEmptyPort) {
            // Port var: Siparişi anında onayla ve portu düşür
            order.setStatus("ONAYLANDI");
            closestNode.setAllocatedPorts(closestNode.getAllocatedPorts() + 1);
            nodeRepository.save(closestNode);
            orderRepository.save(order);
        } else {
            // Port yok: Durumu PORT_BEKLENIYOR yap, kaydet ve RabbitMQ kuyruğuna fırlat!
            order.setStatus("PORT_BEKLENIYOR");
            Order savedOrder = orderRepository.save(order); // ID oluşması için önce kaydettik

            // Asenkron işlenmek üzere kuyruğa gönderiyoruz
       // Nesneyi doğrudan atmak yerine sadece sipariş ID'sini veya BBK'sını String olarak gönderiyoruz
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    order.getId().toString());

        }

        return order;
    }
}