package edu.maestria.observabilidad.fulfillment.service;

import edu.maestria.observabilidad.fulfillment.client.DataClient;
import edu.maestria.observabilidad.fulfillment.client.DataClientException;
import edu.maestria.observabilidad.fulfillment.domain.FulfillmentView;
import edu.maestria.observabilidad.lab33.ChaosPolicy;
import edu.maestria.observabilidad.lab33.Ids;
import edu.maestria.observabilidad.lab33.TraceMdc;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentService.class);

    private final DataClient dataClient;
    private final ChaosPolicy chaos;
    private final Tracer tracer;
    private final LongCounter injections;

    public FulfillmentService(DataClient dataClient, ChaosPolicy chaos, Tracer tracer, Meter meter) {
        this.dataClient = dataClient;
        this.chaos = chaos;
        this.tracer = tracer;
        this.injections = meter.counterBuilder("chaos.latency.injections")
                .setDescription("Veces que service-b durmió por el experimento de latencia")
                .build();
    }

    public FulfillmentView prepare(String orderId, String experiment) {
        if (!Ids.validOrderId(orderId)) {
            throw new InvalidOrderIdException();
        }
        Span span = tracer.spanBuilder("fulfillment.prepare").startSpan();
        try (var scope = span.makeCurrent(); var ignored = new TraceMdc(span)) {
            int delay = chaos.applyLatency();
            span.setAttribute("chaos.injected", delay > 0);
            span.setAttribute("chaos.delay_ms", delay);
            if (delay > 0) {
                injections.add(1, Attributes.of(AttributeKey.longKey("chaos.delay_ms"), (long) delay));
                log.warn("latencia inyectada delayMs={} enabledAt={}", delay, chaos.latencyEnabledAt());
                Thread.sleep(delay);
            }
            FulfillmentView loaded = dataClient.load(orderId, experiment);
            log.info("Fulfillment preparado orderId={} delayMs={}", orderId, delay);
            return new FulfillmentView(
                    loaded.orderId(), loaded.productSku(), loaded.quantity(), loaded.status(),
                    loaded.available(), loaded.warehouse(), loaded.stockSufficient(),
                    delay, Ids.traceId());
        } catch (DataClientException exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR);
            log.error("Fallo de data-service orderId={} status={}", orderId, exception.status());
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR);
            throw new IllegalStateException("latencia interrumpida");
        } finally {
            span.end();
        }
    }
}
