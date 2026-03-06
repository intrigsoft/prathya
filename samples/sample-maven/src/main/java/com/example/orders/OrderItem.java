package com.example.orders;

public class OrderItem {

    private final String name;
    private final int quantity;
    private final double price;

    public OrderItem(String name, int quantity, double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getSubtotal() { return price * quantity; }
}
