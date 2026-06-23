package com.telco.backend.service;

import com.telco.backend.dto.FeasibilityResponseDTO;
import com.telco.backend.model.Building;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeasibilityServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private InfrastructureNodeRepository nodeRepository;

    @InjectMocks
    private FeasibilityService feasibilityService;

    private Building mockBuilding;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory();
        // Test için Eskişehir merkezli örnek bir koordinat noktası tanımlıyoruz kanka (Lng, Lat)
        Point buildingLocation = geometryFactory.createPoint(new Coordinate(30.5095, 39.7685));

        mockBuilding = new Building();
        mockBuilding.setBbk("1498423684");
        mockBuilding.setLocation(buildingLocation);
    }

    @Test
    @DisplayName("SENARYO 1: Fiber Altyapı İçin Maksimum Hız ve Sinyal Metrikleri Doğru Dönmeli")
    void shouldReturnCorrectMetricsForFiberInfrastructure() {
        // Given
        InfrastructureNode fiberNode = new InfrastructureNode();
        fiberNode.setId(101L);
        fiberNode.setNodeType("FIBER");
        fiberNode.setTotalPorts(20);
        fiberNode.setAllocatedPorts(5);
        fiberNode.setName("SD-101");
        fiberNode.setLocation(geometryFactory.createPoint(new Coordinate(30.5095, 39.7685))); // Mesafe 0 metre

        when(buildingRepository.findByBbk("1498423684")).thenReturn(Optional.of(mockBuilding));
        when(nodeRepository.findClosestNode(any(Point.class))).thenReturn(Optional.of(fiberNode));

        // When
        FeasibilityResponseDTO response = feasibilityService.checkFeasibility("1498423684");

        // Then
        assertNotNull(response);
        assertEquals("FIBER", response.getInfrastructureType());
        assertEquals(1000, response.getMaxAvailableSpeedMbps()); // Fiber her zaman 1000 Mbps basmalı kanka
        assertEquals(2.1, response.getAttenuationDb()); // Fiber sabit zayıflama değeri
        assertEquals(100, response.getLineQualityPercent()); // Fiber hat kalitesi %100 olmalı
        assertTrue(response.isHasEmptyPort());
    }

    @Test
    @DisplayName("SENARYO 2: VDSL Altyapıda Mesafe Yakınsa Maksimum VDSL Hızı (100 Mbps) Tanımlanmalı")
    void shouldReturnMaxVdslSpeedWhenDistanceIsVeryClose() {
        // Given
        InfrastructureNode vdslNode = new InfrastructureNode();
        vdslNode.setId(102L);
        vdslNode.setNodeType("VDSL");
        vdslNode.setTotalPorts(10);
        vdslNode.setAllocatedPorts(2);
        vdslNode.setName("SD-102");
        // Bina ile saha dolabı koordinatlarını aynı vererek mesafeyi 0m (<=50m eşiği) yapıyoruz kanka
        vdslNode.setLocation(geometryFactory.createPoint(new Coordinate(30.5095, 39.7685)));

        when(buildingRepository.findByBbk("1498423684")).thenReturn(Optional.of(mockBuilding));
        when(nodeRepository.findClosestNode(any(Point.class))).thenReturn(Optional.of(vdslNode));

        // When
        FeasibilityResponseDTO response = feasibilityService.checkFeasibility("1498423684");

        // Then
        assertNotNull(response);
        assertEquals("VDSL", response.getInfrastructureType());
        assertEquals(100, response.getMaxAvailableSpeedMbps()); // 50 metrenin altı 100 Mbps basmalı kanka
        assertTrue(response.getLineQualityPercent() > 0);
    }
}