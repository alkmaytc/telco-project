package com.telco.backend.service;

import com.telco.backend.config.RabbitMQConfig;
import com.telco.backend.model.Order;
import com.telco.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderRepository orderRepository;

    /**
     * RabbitMQ kuyruğunu sürekli dinler.
     * Kuyruğa yeni bir sipariş ID'si düştüğü an asenkron olarak bu metot tetiklenir.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeOrderMessage(String orderId) {
        log.info("📩 RabbitMQ'dan yeni mesaj alındı. İşlenecek Sipariş ID: {}", orderId);

        try {
            Long id = Long.parseLong(orderId);

            // 1. Siparişi veritabanından buluyoruz
            Order order = orderRepository.findById(id).orElse(null);

            if (order != null) {
                log.info("📦 Sipariş detayları kontrol ediliyor... BBK: {}, Paket: {}", order.getBbk(), order.getPackageName());

                // 2. BURASI GELECEKTEKİ ADMIN PANELİ VE OTOMASYON BAĞLANTISI:
                // Şu an port olmadığı için bu sipariş 'PORT_BEKLENIYOR' durumunda kalmaya devam edecek.
                // Altyapı ekipleri port açtığında bu kuyruk tekrar tetiklenecek.

                log.info("⏳ Sipariş (ID: {}) için şu an boş port bulunamadığından 'PORT_BEKLENIYOR' havuzunda güvenle bekletiliyor.", orderId);
            } else {
                log.warn("⚠️ Kuyruktan gelen ID ({}) ile eşleşen bir sipariş veritabanında bulunamadı!", orderId);
            }

        } catch (NumberFormatException e) {
            log.error("❌ Kuyruktan geçersiz bir sipariş ID formatı alındı: {}", orderId);
        }
    }
}