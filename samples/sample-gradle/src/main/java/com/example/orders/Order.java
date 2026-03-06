package com.example.orders;

import java.util.List;
import java.util.UUID;

public class Order {

    public enum Status { PENDING, CONFIRMED, CANCELLED }

    private final String id;
    private final List<OrderItem> items;
    private Status status;

    public Order(List<OrderItem> items) {
        this.id = UUID.randomUUID().toString();
        this.items = List.copyOf(items);
        this.status = Status.PENDING;
    }

    public String getId() { return id; }
    public List<OrderItem> getItems() { return items; }
    public Status getStatus() { return status; }

    public double getTotal() {
        return items.stream().mapToDouble(OrderItem::getSubtotal).sum();
    }

    public void cancel() {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Cannot cancel order in status " + status);
        }
        this.status = Status.CANCELLED;
    }
}
