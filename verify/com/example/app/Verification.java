package com.example.app;

/** Offline verification harness (no external deps). Exit 0 = all pass. */
public class Verification {
    static int failures = 0;

    static void check(boolean cond, String label) {
        if (cond) {
            System.out.println("PASS: " + label);
        } else {
            System.out.println("FAIL: " + label);
            failures++;
        }
    }

    public static void main(String[] args) {
        {
            var repo = new com.example.app.repository.OrderRepository();
            var ctrl = new com.example.app.controller.OrderController(new com.example.app.service.OrderService(repo));
            var created = ctrl.create("sample");
            check(created.getId() != null, "Order: create returns id");
            check("sample".equals(created.getName()), "Order: name preserved");
            check(ctrl.getById(created.getId()).isPresent(), "Order: get returns entity");
            boolean threw = false;
            try { ctrl.create(""); } catch (IllegalArgumentException ex) { threw = true; }
            check(threw, "Order: blank name rejected");
        }
        if (failures > 0) {
            System.out.println(failures + " test(s) failed");
            System.exit(1);
        }
        System.out.println("All tests passed");
    }
}
