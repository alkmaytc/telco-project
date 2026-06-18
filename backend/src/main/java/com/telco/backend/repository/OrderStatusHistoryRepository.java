package com.telco.backend.repository;

import com.telco.backend.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    // Belirli bir siparişe ait tüm geçmişi kronolojik olarak getirmek için
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(Long orderId);
}