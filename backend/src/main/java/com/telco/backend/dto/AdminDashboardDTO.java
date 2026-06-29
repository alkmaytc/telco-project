package com.telco.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Admin Dashboard Ana Analitik ve Yönetim Nesnesi")
public class AdminDashboardDTO {
    private StatsDTO stats;
    private List<PendingOrderDTO> pendingOrders;
    private List<NodeAdminDTO> nodes;
    private List<LogDTO> logs;

    public StatsDTO getStats() { return stats; }
    public void setStats(StatsDTO stats) { this.stats = stats; }

    public List<PendingOrderDTO> getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(List<PendingOrderDTO> pendingOrders) { this.pendingOrders = pendingOrders; }

    public List<NodeAdminDTO> getNodes() { return nodes; }
    public void setNodes(List<NodeAdminDTO> nodes) { this.nodes = nodes; }

    public List<LogDTO> getLogs() { return logs; }
    public void setLogs(List<LogDTO> logs) { this.logs = logs; }

    @Schema(description = "Sistem Genel İstatistikleri")
    public static class StatsDTO {
        @Schema(description = "Toplam Ciro", example = "154,500.00 TL")
        public String totalRevenue;
        @Schema(description = "Aktif abone sayısı", example = "1250")
        public long activeSubscribers;
        @Schema(description = "RabbitMQ kuyruğunda bekleyen işlem sayısı", example = "5")
        public long pendingRabbitMq;
        @Schema(description = "Toplam saha dolabı sayısı", example = "42")
        public long totalNodes;
    }

    @Schema(description = "Bekleyen (Kuyruktaki) Sipariş Detayı")
    public static class PendingOrderDTO {
        @Schema(example = "10")
        public Long id;
        @Schema(description = "Bina Kimlik Kodu", example = "1750295558")
        public String bbk;
        @Schema(description = "Satın alınan paket", example = "Telco Giga Fiber 1000")
        public String pkg;
        @Schema(description = "Anlık sipariş durumu", example = "PORT_BEKLENIYOR")
        public String status;
    }

    @Schema(description = "Saha Dolabı (Node) Yönetim Detayları")
    public static class NodeAdminDTO {
        @Schema(example = "1")
        public Long id;
        @Schema(example = "SD-1026")
        public String name;
        @Schema(example = "Odunpazarı / Vişnelik")
        public String region;
        @Schema(description = "Doluluk Oranı", example = "45/50")
        public String capacity;
        @Schema(description = "Kapasite Statüsü", example = "%90 Dolu")
        public String status;
        @Schema(description = "Arayüz için renk kodu", example = "danger")
        public String color;
        @Schema(description = "Altyapı Türü", example = "FIBER")
        public String nodeType;
        @Schema(description = "Google Maps Enlemi", example = "39.789500")
        public Double lat;
        @Schema(description = "Google Maps Boylamı", example = "30.508000")
        public Double lng;
    }

    @Schema(description = "Sistem Audit Log Kaydı")
    public static class LogDTO {
        @Schema(description = "Değişiklik zamanı", example = "2026-06-29 14:00:00")
        public String changedAt;
        @Schema(description = "İlgili sipariş ID'si", example = "10")
        public Long orderId;
        @Schema(description = "Yeni Durum", example = "ONAYLANDI")
        public String status;
        @Schema(description = "İşlem açıklaması", example = "En yakın saha dolabında boş port tahsis edildi.")
        public String description;
    }
}