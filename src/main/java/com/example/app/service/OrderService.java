package com.example.app.service;

import com.example.app.domain.Order;
import com.example.app.repository.OrderRepository;
import java.util.Optional;
import java.util.UUID;

/** Business logic for Order (three-tier: Service). */
public class OrderService {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        String id = UUID.randomUUID().toString();
        return repository.save(new Order(id, name));
    }

    public Optional<Order> get(String id) {
        return repository.findById(id);
    }
}
