package com.telco.backend.service;

import com.telco.backend.config.RabbitMQConfig;
import com.telco.backend.model.Order;
import com.telco.backend.model.OrderStatusHistory;
import com.telco.backend.repository.OrderRepository;
import com.telco.backend.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository; // Yeni Repository

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeOrderMessage(String orderId) {
        log.info("📩 RabbitMQ'dan yeni mesaj alındı. İşlenecek Sipariş ID: {}", orderId);

        try {
            Long id = Long.parseLong(orderId);
            Order order = orderRepository.findById(id).orElse(null);

            if (order != null) {
                log.info("📦 Sipariş detayları kontrol ediliyor... BBK: {}, Paket: {}", order.getBbk(), order.getPackageName());

                // AUDIT LOG: Siparişin asenkron olarak kuyrukta işlendiğinin tarihçesi
                historyRepository.save(new OrderStatusHistory(id, "PORT_BEKLENIYOR",
                        "Sipariş arka planda RabbitMQ asenkron işleyicisi tarafından teslim alındı ve operasyon sırasına eklendi."));

                log.info("⏳ Sipariş (ID: {}) için şu an boş port bulunamadığından 'PORT_BEKLENIYOR' havuzunda güvenle bekletiliyor.", orderId);
            } else {
                log.warn("⚠️ Kuyruktan gelen ID ({}) ile eşleşen bir sipariş veritabanında bulunamadı!", orderId);
            }

        } catch (NumberFormatException e) {
            log.error("❌ Kuyruktan geçersiz bir sipariş ID formatı alındı: {}", orderId);
        }
    }
}