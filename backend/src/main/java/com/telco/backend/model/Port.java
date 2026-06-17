package com.telco.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ports", uniqueConstraints = {
        // KURAL: Bir dolabın içinde aynı numaradan iki port olamaz!
        @UniqueConstraint(columnNames = {"infrastructure_node_id", "port_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Port {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "port_number", nullable = false)
    private Integer portNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private PortState state = PortState.BOS;

    // İLİŞKİ: Bu port hangi dolaba ait?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "infrastructure_node_id", nullable = false)
    private com.telco.backend.model.InfrastructureNode infrastructureNode;

    public enum PortState {
        BOS,
        DOLU,
        ARIZALI
    }
}