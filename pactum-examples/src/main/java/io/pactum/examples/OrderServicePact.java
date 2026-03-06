package io.pactum.examples;

import io.pactum.annotations.Pact;
import io.pactum.annotations.Requirement;

/**
 * Example: annotated class describing the contract between an order consumer
 * and an order-service provider.
 *
 * <p>To run pactum verification against this class via Maven:
 * <pre>{@code
 * mvn io.pactum:pactum-maven-plugin:verify -Dpactum.classes=io.pactum.examples.OrderServicePact
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
