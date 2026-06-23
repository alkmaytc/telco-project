package com.telco.backend.repository;

import com.telco.backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Toplam Onaylanan Sipariş Ciro Hesaplaması
    @Query("SELECT COALESCE(SUM(o.price), 0) FROM Order o WHERE o.status = 'ONAYLANDI'")
    Double calculateTotalRevenue();

    // Belirli bir statüdeki sipariş sayısını bulma (Örn: Aktif Aboneler)
    long countByStatus(String status);

    // Bekleyen siparişleri listelemek için (Yeniden eskiye)
    List<Order> findByStatusOrderByCreatedAtDesc(String status);

    // 🎯 MADDE 6: Bekleyen siparişleri kuyruk mantığıyla çekme (Eskiden yeniye - FIFO) ✅
    List<Order> findByStatusOrderByCreatedAtAsc(String status);

    // GİZLİLİK KURALI: Giriş yapan müşterinin sadece kendi siparişlerini görmesi için
    List<Order> findByCustomerId(Long customerId);
}