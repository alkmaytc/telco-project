package com.telco.backend.consumer;

import com.telco.backend.config.RabbitMQConfig;
import com.telco.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final OrderService orderService;

    /**
     * 🎯 MADDE 6: RABBITMQ KUYREK TÜKETİCİSİ (CONSUMER)
     * Kuyruğa düşen sipariş ID'sini asenkron olarak yakalar ve işlenmek üzere servis motoruna paslar kanka! ✅
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeOrderMessage(String message) {
        log.info("📨 [RABBITMQ CONSUMER] Kuyruktan yeni bir sipariş mesajı yakalandı! Sipariş ID: {}", message);

        try {
            Long orderId = Long.parseLong(message);

            // Siparişi işleyen arka plan metodunu tetikliyoruz kanka kanka
            orderService.processOrderFromQueue(orderId);

            log.info("✅ [RABBITMQ CONSUMER] Sipariş ID: {} başarıyla asenkron olarak işlendi.", orderId);
        } catch (Exception e) {
            log.error("❌ [RABBITMQ CONSUMER] Kuyruk mesajı işlenirken kritik hata oluştu! Mesaj: " + message, e);
        }
    }
}