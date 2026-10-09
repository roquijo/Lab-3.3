package edu.maestria.observabilidad.records.service;

import edu.maestria.observabilidad.lab33.ChaosPolicy;
import edu.maestria.observabilidad.lab33.Ids;
import edu.maestria.observabilidad.lab33.TraceMdc;
import edu.maestria.observabilidad.records.domain.RecordView;
import edu.maestria.observabilidad.records.repository.RecordRepository;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecordService {

    private static final Logger log = LoggerFactory.getLogger(RecordService.class);

    private final RecordRepository repository;
    private final ChaosPolicy chaos;
    private final Tracer tracer;
    private final LongCounter injectedErrors;

    public RecordService(RecordRepository repository, ChaosPolicy chaos, Tracer tracer, Meter meter) {
        this.repository = repository;
        this.chaos = chaos;
        this.tracer = tracer;
        this.injectedErrors = meter.counterBuilder("chaos.errors.injected")
                .setDescription("Errores HTTP inyectados solo en peticiones marcadas del experimento")
                .build();
    }

    public RecordView load(String orderId, String experiment) {
        if (!Ids.validOrderId(orderId)) {
            throw new InvalidOrderIdException();
        }
        Span span = tracer.spanBuilder("record.load").startSpan();
        try (var scope = span.makeCurrent(); var ignored = new TraceMdc(span)) {
            span.setAttribute("order.id", orderId);
            if (chaos.shouldFail(experiment)) {
                injectedErrors.add(1);
                span.setAttribute("chaos.injected", true);
                span.setStatus(StatusCode.ERROR, "error de experimento");
                log.error("error inyectado orderId={} rate={}", orderId, ChaosPolicy.ERROR_RATE);
                throw new InjectedFailureException();
            }
            List<RecordView> rows = repository.findOrder(orderId);
            if (rows.isEmpty()) {
                log.info("Pedido no encontrado orderId={}", orderId);
                throw new OrderNotFoundException();
            }
            RecordView row = rows.getFirst();
            log.info("Registro leído orderId={} sku={} sufficient={}", orderId, row.productSku(), row.stockSufficient());
            return new RecordView(
                    row.orderId(), row.productSku(), row.quantity(), row.status(),
                    row.available(), row.warehouse(), row.stockSufficient(), 0, Ids.traceId());
        } catch (RuntimeException exception) {
            if (!(exception instanceof OrderNotFoundException) && !(exception instanceof InvalidOrderIdException)) {
                span.recordException(exception);
                if (exception instanceof InjectedFailureException) {
                    span.setStatus(StatusCode.ERROR);
                }
            }
            throw exception;
        } finally {
            span.end();
        }
    }
}
