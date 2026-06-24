package com.telco.backend.dto;

import java.util.List;

public class AdminDashboardDTO {
    private StatsDTO stats;
    private List<PendingOrderDTO> pendingOrders;
    private List<NodeAdminDTO> nodes;
    private List<LogDTO> logs;

    // Getters ve Setters
    public StatsDTO getStats() { return stats; }
    public void setStats(StatsDTO stats) { this.stats = stats; }

    public List<PendingOrderDTO> getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(List<PendingOrderDTO> pendingOrders) { this.pendingOrders = pendingOrders; }

    public List<NodeAdminDTO> getNodes() { return nodes; }
    public void setNodes(List<NodeAdminDTO> nodes) { this.nodes = nodes; }

    public List<LogDTO> getLogs() { return logs; }
    public void setLogs(List<LogDTO> logs) { this.logs = logs; }

    // İç DTO Sınıfları
    public static class StatsDTO {
        public String totalRevenue;
        public long activeSubscribers;
        public long pendingRabbitMq;
        public long totalNodes;
    }

    public static class PendingOrderDTO {
        public Long id;
        public String bbk;
        public String pkg;
        public String status;
    }

    public static class NodeAdminDTO {
        public Long id;
        public String name;
        public String region;
        public String capacity;
        public String status;
        public String color;
        public String nodeType; // FIBER veya VDSL bilgisi frontend'e akacak

        // 🎯 YENİ EKLENDİ: Harita entegrasyonu için coğrafi koordinatlar
        public Double lat;
        public Double lng;
    }

    public static class LogDTO {
        public String changedAt;
        public Long orderId;
        public String status;
        public String description;
    }
}