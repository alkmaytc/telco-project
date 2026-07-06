package com.telco.backend.consumer;

import com.telco.backend.config.RabbitMQConfig;
import com.telco.backend.model.Order;
import com.telco.backend.repository.OrderRepository;
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
    private final OrderRepository orderRepository; // 🚨 Zırh için repository eklendi!

    /**
     * 🎯 MADDE 6: RABBITMQ KUYRUK TÜKETİCİSİ (CONSUMER)
     * Kuyruğa düşen sipariş ID'sini asenkron olarak yakalar ve işlenmek üzere servis motoruna paslar! ✅
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeOrderMessage(String message) {
        log.info("📨 [RABBITMQ CONSUMER] Kuyruktan yeni bir sipariş mesajı yakalandı! Sipariş ID: {}", message);

        try {
            /*
             * 🚨 TECH DEBT (TEKNİK BORÇ) - RACE CONDITION GEÇİCİ ÇÖZÜMÜ
             * Durum: Veritabanı transaction'ı henüz commit edilmeden RabbitMQ mesajı fırlattığı için,
             * Consumer anında devreye girip veritabanında olmayan siparişi arıyor ve hata yiyor.
             * * Geçici Çözüm: Veritabanının commit'i tamamlayabilmesi için 500ms suni bekleme (sleep) eklendi.
             */
            Thread.sleep(500);

            Long orderId = Long.parseLong(message);

            // -----------------------------------------------------------------
            // 🚨 ASENKRON ZIRH (KÖRDÜĞÜM ÇÖZÜCÜ) DEVREDE 🚨
            // Siparişi işleme almadan önce güncel durumuna bakıyoruz.
            Order currentOrder = orderRepository.findById(orderId).orElse(null);

            if (currentOrder != null && "IPTAL_EDILDI".equals(currentOrder.getStatus())) {
                log.warn("🚨 [KÖRDÜĞÜM ÇÖZÜLDÜ] Sipariş ID: {} kuyrukta beklerken müşteri tarafından iptal edilmiş. Port tahsisi pas geçiliyor!", orderId);
                return; // İşlemi burada kesiyoruz! Aşağıdaki port bağlama metoduna asla gitmeyecek!
            }
            // -----------------------------------------------------------------

            // Zırhtan geçerse (iptal edilmemişse) siparişi işleyen arka plan metodunu tetikliyoruz
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