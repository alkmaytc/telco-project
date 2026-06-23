package com.telco.backend;

import com.telco.backend.dto.OrderRequestDTO;
import com.telco.backend.dto.OrderResponseDTO;
import com.telco.backend.model.Building;
import com.telco.backend.model.Customer;
import com.telco.backend.model.InfrastructureNode;
import com.telco.backend.model.Order;
import com.telco.backend.repository.BuildingRepository;
import com.telco.backend.repository.CustomerRepository;
import com.telco.backend.repository.InfrastructureNodeRepository;
import com.telco.backend.repository.OrderRepository;
import com.telco.backend.repository.OrderStatusHistoryRepository;
import com.telco.backend.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private OrderStatusHistoryRepository historyRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AmqpTemplate rabbitTemplate;

    @Mock
    private InfrastructureNodeRepository nodeRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("SENARYO 3: Boş Port Olmadığında Sipariş Otomatik Olarak PORT_BEKLENIYOR Statüsüne Düşmeli")
    void shouldMoveOrderToPortBekliyorStatusWhenNoEmptyPortAvailable() {
        Long orderId = 500L;
        Order mockOrder = new Order();
        mockOrder.setId(orderId);
        mockOrder.setBbk("111222333");
        mockOrder.setStatus("RECEIVED");

        GeometryFactory factory = new GeometryFactory();
        Building mockBuilding = new Building();
        mockBuilding.setBbk("111222333");
        mockBuilding.setLocation(factory.createPoint(new Coordinate(30.5095, 39.7685)));

        InfrastructureNode fullNode = new InfrastructureNode();
        fullNode.setId(999L);
        fullNode.setTotalPorts(10);
        fullNode.setAllocatedPorts(10);
        fullNode.setNodeType("VDSL");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(buildingRepository.findByBbk("111222333")).thenReturn(Optional.of(mockBuilding));
        when(nodeRepository.findClosestNode(any())).thenReturn(Optional.of(fullNode));

        orderService.processOrderFromQueue(orderId);

        assertEquals("PORT_BEKLENIYOR", mockOrder.getStatus());
        verify(orderRepository, times(1)).save(mockOrder);
        verify(historyRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("SENARYO 5: Kullanıcı Sipariş Oluşturduğunda Sipariş RECEIVED Olarak Kaydedilmeli ve Kuyruğa Atılmalı")
    void shouldCreateOrderSuccessfullyAndPushToQueue() {
        try (var mockedSecurity = Mockito.mockStatic(SecurityContextHolder.class)) {

            // 🎯 DÜZELTME: Constructor hatasını engellemek için nesneyi setter'lar ile dolduruyoruz kanka ✅
            OrderRequestDTO request = new OrderRequestDTO();
            request.setBbk("111222333");
            request.setPackageName("Telco Hiper Paket");
            request.setSpeedMbps(50);
            request.setPrice(329.90);

            Building mockBuilding = new Building();
            mockBuilding.setBbk("111222333");

            Customer mockCustomer = new Customer();
            mockCustomer.setId(1L);
            mockCustomer.setEmail("admin@telco.com");
            mockCustomer.setFirstName("Alkim");
            mockCustomer.setLastName("Aytac");

            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);

            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("admin@telco.com");

            when(buildingRepository.findByBbk("111222333")).thenReturn(Optional.of(mockBuilding));
            when(customerRepository.findByEmail("admin@telco.com")).thenReturn(Optional.of(mockCustomer));

            Order savedOrder = new Order();
            savedOrder.setId(777L);
            savedOrder.setBbk("111222333");
            savedOrder.setPackageName("Telco Hiper Paket");
            savedOrder.setStatus("RECEIVED");
            savedOrder.setCustomer(mockCustomer);

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

            OrderResponseDTO response = orderService.createOrder(request);

            assertNotNull(response);
            assertEquals(777L, response.getId());
            assertEquals("RECEIVED", response.getStatus());

            verify(historyRepository, times(1)).save(any());
            verify(rabbitTemplate, times(1)).convertAndSend(any(), any(), eq("777"));
        }
    }
}