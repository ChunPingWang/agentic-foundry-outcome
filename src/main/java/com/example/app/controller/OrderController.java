package com.example.app.controller;

import com.example.app.domain.Order;
import com.example.app.service.OrderService;
import java.util.Optional;

/**
 * REST-style controller for Order (three-tier: Controller).
 * Framework annotations (e.g. @RestController) are added when wiring Spring Boot;
 * the logic is framework-agnostic and unit-testable as-is.
 */
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** POST /orders -> 201 with created entity. */
    public Order create(String name) {
        return service.create(name);
    }

    /** GET /orders/{id} -> 200 or 404. */
    public Optional<Order> getById(String id) {
        return service.get(id);
    }
}
