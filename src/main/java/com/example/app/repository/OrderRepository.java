package com.example.app.repository;

import com.example.app.domain.Order;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory repository (swap for Spring Data JPA in production). */
public class OrderRepository {
    private final Map<String, Order> store = new ConcurrentHashMap<>();

    public Order save(Order e) {
        store.put(e.getId(), e);
        return e;
    }

    public Optional<Order> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public int count() { return store.size(); }
}
