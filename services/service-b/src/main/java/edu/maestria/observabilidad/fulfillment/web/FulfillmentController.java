package edu.maestria.observabilidad.fulfillment.web;

import edu.maestria.observabilidad.fulfillment.domain.FulfillmentView;
import edu.maestria.observabilidad.fulfillment.service.FulfillmentService;
import edu.maestria.observabilidad.lab33.ChaosPolicy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FulfillmentController {

    private final FulfillmentService service;

    public FulfillmentController(FulfillmentService service) {
        this.service = service;
    }

    @GetMapping("/api/fulfillment/{orderId}")
    public FulfillmentView fulfill(
            @PathVariable String orderId,
            @RequestHeader(value = ChaosPolicy.EXPERIMENT_HEADER, required = false) String experiment) {
        return service.prepare(orderId, experiment);
    }
}
