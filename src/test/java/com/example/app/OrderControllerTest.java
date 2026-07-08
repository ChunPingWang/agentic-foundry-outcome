package com.example.app;

import com.example.app.controller.OrderController;
import com.example.app.domain.Order;
import com.example.app.repository.OrderRepository;
import com.example.app.service.OrderService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;

class OrderControllerTest {
    private OrderController controller() {
        var repo = new OrderRepository();
        return new OrderController(new OrderService(repo));
    }

    @Test
    void happyCreate() {
        var created = controller().create("sample");
        assertNotNull(created.getId());
        assertEquals("sample", created.getName());
    }

    @Test
    void negativeCreateBlank() {
        assertThrows(IllegalArgumentException.class, () -> controller().create(""));
    }

    @Test
    void happyGet() {
        var c = controller();
        var created = c.create("sample");
        Optional<Order> found = c.getById(created.getId());
        assertTrue(found.isPresent());
    }
}
