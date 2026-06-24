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
     * 🎯 MADDE 6: RABBITMQ KUYRUK TÜKETİCİSİ (CONSUMER)
     * Kuyruğa düşen sipariş ID'sini asenkron olarak yakalar ve işlenmek üzere servis motoruna paslar! ✅
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeOrderMessage(String message) {
        log.info("📨 [RABBITMQ CONSUMER] Kuyruktan yeni bir sipariş mesajı yakalandı! Sipariş ID: {}", message);

        try {
            // 🎯 SİNSİ HATA ÇÖZÜMÜ: Veritabanı Race Condition (Yarış Durumu) Engellendi!
            // OrderService'in veritabanına kayıt işlemini (Commit) tam olarak bitirebilmesi için
            // kuyruğa sadece yarım saniyelik bir nefes aldırıyoruz kanka.
            Thread.sleep(500);

            Long orderId = Long.parseLong(message);

            // Siparişi işleyen arka plan metodunu tetikliyoruz
            orderService.processOrderFromQueue(orderId);

            log.info("✅ [RABBITMQ CONSUMER] Sipariş ID: {} başarıyla asenkron olarak işlendi.", orderId);

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("❌ [RABBITMQ CONSUMER] Uyku modu kesintiye uğradı!", ie);
        } catch (Exception e) {
            log.error("❌ [RABBITMQ CONSUMER] Kuyruk mesajı işlenirken kritik hata oluştu! Mesaj: " + message, e);

            // 🚨 ÖNEMLİ: Hatayı asla yutmuyoruz! Exception fırlatıyoruz ki RabbitMQ mesajı silmesin.
            // Sistem hatayı anlayıp mesajı tekrar kuyruğa koysun (Retry/DLQ mekanizması).
            throw new RuntimeException("Kuyruk işlenemedi, RabbitMQ lütfen tekrar dene!", e);
        }
    }
}