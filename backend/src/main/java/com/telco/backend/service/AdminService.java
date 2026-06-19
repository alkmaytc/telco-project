package com.telco.backend.service;

import com.telco.backend.dto.AdminDashboardDTO;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.model.Order;
import com.telco.backend.model.OrderStatusHistory;
import com.telco.backend.repository.InfrastructureNodeRepository;
import com.telco.backend.repository.OrderRepository;
import com.telco.backend.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository orderRepository;
    private final InfrastructureNodeRepository nodeRepository;
    private final OrderStatusHistoryRepository historyRepository;

    public AdminDashboardDTO getDashboardData() {
        AdminDashboardDTO dashboard = new AdminDashboardDTO();

        // 1. İstatistikler
        AdminDashboardDTO.StatsDTO stats = new AdminDashboardDTO.StatsDTO();
        stats.totalRevenue = orderRepository.calculateTotalRevenue() + "₺";
        stats.activeSubscribers = orderRepository.countByStatus("ONAYLANDI");
        stats.pendingRabbitMq = orderRepository.countByStatus("PORT_BEKLENIYOR");
        stats.totalNodes = nodeRepository.count();
        dashboard.setStats(stats);

        // 2. Bekleyen Siparişler
        dashboard.setPendingOrders(orderRepository.findByStatusOrderByCreatedAtDesc("PORT_BEKLENIYOR")
                .stream().map(o -> {
                    AdminDashboardDTO.PendingOrderDTO dto = new AdminDashboardDTO.PendingOrderDTO();
                    dto.id = o.getId();
                    dto.bbk = o.getBbk();
                    dto.pkg = o.getPackageName();
                    dto.status = o.getStatus();
                    return dto;
                }).collect(Collectors.toList()));

// Düzeltilmiş Servis Metodu
        dashboard.setNodes(nodeRepository.findAll().stream().map(n -> {
            AdminDashboardDTO.NodeAdminDTO dto = new AdminDashboardDTO.NodeAdminDTO();
            dto.id = n.getId();
            dto.name = n.getName(); // <--- İŞTE BURAYI DÜZELTTİK: getNodeName() yerine getName() yaptık
            dto.region = n.getDistrict() + " / " + n.getNeighborhood();
            dto.capacity = n.getAllocatedPorts() + " / " + n.getTotalPorts();
            boolean isFull = n.getAllocatedPorts() >= n.getTotalPorts();
            dto.status = isFull ? "FULL" : "OK";
            dto.color = isFull ? "#fed3c7" : "#e6ffe6";
            return dto;
        }).collect(Collectors.toList()));
        // 4. Loglar
        dashboard.setLogs(historyRepository.findTop50ByOrderByChangedAtDesc().stream().map(l -> {
            AdminDashboardDTO.LogDTO dto = new AdminDashboardDTO.LogDTO();
            dto.time = l.getChangedAt().toLocalTime().withNano(0).toString();
            dto.msg = "[DB] STATUS: " + l.getStatus() + " - " + l.getNote();
            return dto;
        }).collect(Collectors.toList()));

        return dashboard;
    }
}