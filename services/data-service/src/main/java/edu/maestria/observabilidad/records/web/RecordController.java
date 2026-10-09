package edu.maestria.observabilidad.records.web;

import edu.maestria.observabilidad.lab33.ChaosPolicy;
import edu.maestria.observabilidad.records.domain.RecordView;
import edu.maestria.observabilidad.records.service.RecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecordController {

    private final RecordService records;

    public RecordController(RecordService records) {
        this.records = records;
    }

    @GetMapping("/api/records/orders/{orderId}")
    public RecordView load(
            @PathVariable String orderId,
            @RequestHeader(value = ChaosPolicy.EXPERIMENT_HEADER, required = false) String experiment) {
        return records.load(orderId, experiment);
    }
}
