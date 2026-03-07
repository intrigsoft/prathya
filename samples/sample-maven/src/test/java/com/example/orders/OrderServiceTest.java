package com.example.orders;

import dev.pratya.annotations.Requirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService();
    }

    // --- ORD-001: Create order with valid items ---

    @Test
    @Requirement("ORD-001")
    void createOrder_withValidItems_returnsOrderWithPendingStatus() {
        Order order = service.createOrder(List.of(
                new OrderItem("Widget", 2, 9.99),
                new OrderItem("Gadget", 1, 24.99)
        ));

        assertNotNull(order.getId());
        assertEquals(Order.Status.PENDING, order.getStatus());
        assertEquals(2, order.getItems().size());
        assertEquals(44.97, order.getTotal(), 0.01);
    }

    @Test
    @Requirement("ORD-001-CC-001")
    void createOrder_withEmptyItems_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(List.of()));
    }

    @Test
    @Requirement("ORD-001-CC-002")
    void createOrder_withZeroQuantity_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(List.of(new OrderItem("Widget", 0, 9.99))));
    }

    @Test
    @Requirement("ORD-001-CC-003")
    void createOrder_withNegativePrice_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(List.of(new OrderItem("Widget", 1, -5.00))));
    }

    // --- ORD-002: Cancel a pending order ---

    @Test
    @Requirement("ORD-002")
    void cancelOrder_pendingOrder_transitionsToCancelled() {
        Order order = service.createOrder(List.of(new OrderItem("Widget", 1, 9.99)));
        service.cancelOrder(order.getId());

        assertEquals(Order.Status.CANCELLED, service.getOrder(order.getId()).getStatus());
    }

    @Test
    @Requirement("ORD-002-CC-001")
    void cancelOrder_alreadyCancelled_throwsException() {
        Order order = service.createOrder(List.of(new OrderItem("Widget", 1, 9.99)));
        service.cancelOrder(order.getId());

        assertThrows(IllegalStateException.class,
                () -> service.cancelOrder(order.getId()));
    }

    // --- ORD-003: Compute order total ---

    @Test
    @Requirement("ORD-003")
    void orderTotal_multipleItems_sumsCorrectly() {
        Order order = service.createOrder(List.of(
                new OrderItem("A", 3, 10.00),
                new OrderItem("B", 2, 5.50)
        ));

        assertEquals(41.00, order.getTotal(), 0.01);
    }

    @Test
    @Requirement("ORD-003-CC-001")
    void orderTotal_singleItem_equalsSubtotal() {
        Order order = service.createOrder(List.of(
                new OrderItem("A", 4, 7.25)
        ));

        assertEquals(29.00, order.getTotal(), 0.01);
    }

    // --- Undocumented: test references a requirement not in CONTRACT.yaml ---

    @Test
    @Requirement("ORD-099")
    void orderToString_returnsReadableFormat() {
        Order order = service.createOrder(List.of(
                new OrderItem("Widget", 1, 9.99)
        ));

        assertNotNull(order.toString());
    }
}
