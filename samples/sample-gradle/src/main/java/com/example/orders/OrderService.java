package com.example.orders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderService {

    private final Map<String, Order> orders = new HashMap<>();

    public Order createOrder(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        for (OrderItem item : items) {
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be positive");
            }
            if (item.getPrice() < 0) {
                throw new IllegalArgumentException("Item price must not be negative");
            }
        }

        Order order = new Order(items);
        orders.put(order.getId(), order);
        return order;
    }

    public void cancelOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        order.cancel();
    }

    public Order getOrder(String orderId) {
        return orders.get(orderId);
    }
}
