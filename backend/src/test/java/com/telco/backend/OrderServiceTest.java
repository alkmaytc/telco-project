package com.telco.backend.service;

import com.telco.backend.model.Building;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.model.Order;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import com.telco.backend.repository.OrderRepository;
import com.telco.backend.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private InfrastructureNodeRepository nodeRepository;

    @Mock
    private OrderStatusHistoryRepository historyRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("SENARYO 3: Boş Port Olmadığında Sipariş Otomatik Olarak PORT_BEKLENIYOR Statüsüne Düşmeli")
    void shouldMoveOrderToPortBekliyorStatusWhenNoEmptyPortAvailable() {
        // Given
        Long orderId = 500L;
        Order mockOrder = new Order();
        mockOrder.setId(orderId);
        mockOrder.setBbk("111222333");
        mockOrder.setStatus("RECEIVED");

        GeometryFactory factory = new GeometryFactory();
        Building mockBuilding = new Building();
        mockBuilding.setBbk("111222333");
        mockBuilding.setLocation(factory.createPoint(new Coordinate(30.5095, 39.7685)));

        // Kapasitesi tamamen dolu bir saha dolabı simüle ediyoruz kanka (10 port var, 10'u da dolu)
        InfrastructureNode fullNode = new InfrastructureNode();
        fullNode.setId(999L);
        fullNode.setTotalPorts(10);
        fullNode.setAllocatedPorts(10);
        fullNode.setNodeType("VDSL");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(buildingRepository.findByBbk("111222333")).thenReturn(Optional.of(mockBuilding));
        when(nodeRepository.findClosestNode(any())).thenReturn(Optional.of(fullNode));

        // When
        orderService.processOrderFromQueue(orderId);

        // Then
        assertEquals("PORT_BEKLENIYOR", mockOrder.getStatus()); // Statü port bekleme listesine dönmeli kanka ✅
        verify(orderRepository, times(1)).save(mockOrder);
        verify(historyRepository, times(1)).save(any()); // Tarihçeye log basıldığı doğrulanmalı
    }
}