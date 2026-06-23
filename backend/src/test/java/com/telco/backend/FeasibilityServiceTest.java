package com.telco.backend;

import com.telco.backend.dto.FeasibilityResponseDTO;
import com.telco.backend.model.Building;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import com.telco.backend.service.FeasibilityService;
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
        Point buildingLocation = geometryFactory.createPoint(new Coordinate(30.5095, 39.7685));

        mockBuilding = new Building();
        mockBuilding.setBbk("1498423684");
        mockBuilding.setLocation(buildingLocation);
    }

    @Test
    @DisplayName("SENARYO 1: Fiber Altyapı İçin Maksimum Hız ve Sinyal Metrikleri Doğru Dönmeli")
    void shouldReturnCorrectMetricsForFiberInfrastructure() {
        InfrastructureNode fiberNode = new InfrastructureNode();
        fiberNode.setId(101L);
        fiberNode.setNodeType("FIBER");
        fiberNode.setTotalPorts(20);
        fiberNode.setAllocatedPorts(5);
        fiberNode.setName("SD-101");
        fiberNode.setLocation(geometryFactory.createPoint(new Coordinate(30.5095, 39.7685)));

        when(buildingRepository.findByBbk("1498423684")).thenReturn(Optional.of(mockBuilding));
        when(nodeRepository.findClosestNode(any(Point.class))).thenReturn(Optional.of(fiberNode));

        FeasibilityResponseDTO response = feasibilityService.checkFeasibility("1498423684");

        assertNotNull(response);
        assertEquals("FIBER", response.getInfrastructureType());
        assertEquals(1000, response.getMaxAvailableSpeedMbps());
        assertEquals(2.1, response.getAttenuationDb());
        assertEquals(100, response.getLineQualityPercent());
        assertTrue(response.isHasEmptyPort());
    }

    @Test
    @DisplayName("SENARYO 2: VDSL Altyapıda Mesafe Yakınsa Maksimum VDSL Hızı (100 Mbps) Tanımlanmalı")
    void shouldReturnMaxVdslSpeedWhenDistanceIsVeryClose() {
        InfrastructureNode vdslNode = new InfrastructureNode();
        vdslNode.setId(102L);
        vdslNode.setNodeType("VDSL");
        vdslNode.setTotalPorts(10);
        vdslNode.setAllocatedPorts(2);
        vdslNode.setName("SD-102");
        vdslNode.setLocation(geometryFactory.createPoint(new Coordinate(30.5095, 39.7685)));

        when(buildingRepository.findByBbk("1498423684")).thenReturn(Optional.of(mockBuilding));
        when(nodeRepository.findClosestNode(any(Point.class))).thenReturn(Optional.of(vdslNode));

        FeasibilityResponseDTO response = feasibilityService.checkFeasibility("1498423684");

        assertNotNull(response);
        assertEquals("VDSL", response.getInfrastructureType());
        assertEquals(100, response.getMaxAvailableSpeedMbps());
        assertTrue(response.getLineQualityPercent() > 0);
    }

    @Test
    @DisplayName("SENARYO 4: İlk Saha Dolabı Dolu Olduğunda Akıllı Algoritma Alternatif Boş Portlu Dolabı Seçmeli")
    void shouldSelectAlternativeNodeWhenInitialNodeIsFull() {
        InfrastructureNode fullNode = new InfrastructureNode();
        fullNode.setId(103L);
        fullNode.setNodeType("VDSL");
        fullNode.setTotalPorts(10);
        fullNode.setAllocatedPorts(10);
        fullNode.setName("SD-103");
        fullNode.setLocation(geometryFactory.createPoint(new Coordinate(30.5095, 39.7685)));

        InfrastructureNode alternativeNode = new InfrastructureNode();
        alternativeNode.setId(104L);
        alternativeNode.setNodeType("VDSL");
        alternativeNode.setTotalPorts(10);
        alternativeNode.setAllocatedPorts(4);
        alternativeNode.setName("SD-104");
        alternativeNode.setLocation(geometryFactory.createPoint(new Coordinate(30.5110, 39.7690)));

        when(buildingRepository.findByBbk("1498423684")).thenReturn(Optional.of(mockBuilding));
        when(nodeRepository.findClosestNode(any(Point.class))).thenReturn(Optional.of(fullNode));
        when(nodeRepository.findClosestNodeWithEmptyPort(any(Point.class))).thenReturn(Optional.of(alternativeNode));

        FeasibilityResponseDTO response = feasibilityService.checkFeasibility("1498423684");

        assertNotNull(response);
        assertTrue(response.getClosestNodeName().contains("[Alternatif Dağıtım Hattı]"));
        assertEquals("SD-104 (VDSL) [Alternatif Dağıtım Hattı]", response.getClosestNodeName());
        assertTrue(response.isHasEmptyPort());
    }
}