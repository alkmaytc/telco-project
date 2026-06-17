package com.telco.backend.service;

import com.telco.backend.config.RabbitMQConfig;
import com.telco.backend.model.Order;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderConsumer {

    /**
     * RabbitListener anotasyonu, belirtilen kuyruğa mesaj düştüğü an bu metodu tetikler.
     * Bu işlem tamamen asenkron (arka planda) yürütülür, kullanıcının ekranını kilitlemez.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeOrderMessage(Order order) {
        System.out.println("\n 📬 [RabbitMQ Kuyruğu tetiklendi] - Yeni Asenkron Sipariş Yakalandı!");
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Sipariş ID     : " + order.getId());
        System.out.println("Bina BBK       : " + order.getBbk());
        System.out.println("Seçilen Paket  : " + order.getPackageName());
        System.out.println("Mevcut Durum   : " + order.getStatus() + " (Altyapı ekibi port açtığında onaylanacak)");
        System.out.println("-----------------------------------------------------------------\n");

        // Buraya gelecekte altyapı ekibine otomatik iş emri (Task) açma
        // veya e-posta/SMS bildirimi gönderme mantığı eklenebilir.
    }
}