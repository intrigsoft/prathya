package io.pratya.examples;

import io.pratya.annotations.Pact;
import io.pratya.annotations.Requirement;

/**
 * Example: annotated class describing the contract between an order consumer
 * and an order-service provider.
 *
 * <p>To run pratya verification against this class via Maven:
 * <pre>{@code
 * mvn io.pratya:pratya-maven-plugin:verify -Dpratya.classes=io.pratya.examples.OrderServicePact
 * }</pre>
 */
@Pact(consumer = "order-consumer", provider = "order-service",
      description = "Order service contract")
public class OrderServicePact {

    @Requirement(id = "REQ-001", description = "Retrieve order by ID")
    @Pact(consumer = "order-consumer", provider = "order-service")
    public void getOrderById() {
        // interaction definition goes here
    }

    @Requirement(id = "REQ-002", description = "Create a new order")
    @Pact(consumer = "order-consumer", provider = "order-service")
    public void createOrder() {
        // interaction definition goes here
    }

    @Requirement(id = "REQ-003", description = "Cancel an existing order")
    @Pact(consumer = "order-consumer", provider = "order-service")
    public void cancelOrder() {
        // interaction definition goes here
    }
}
