package edu.maestria.observabilidad.orders.web;

import edu.maestria.observabilidad.orders.domain.OrderView;
import edu.maestria.observabilidad.orders.service.OrderCoordinator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderCoordinator coordinator;

    public OrderController(OrderCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @GetMapping("/api/orders/{orderId}")
    public OrderView order(@PathVariable String orderId) {
        return coordinator.coordinate(orderId);
    }
}
