package edu.maestria.observabilidad.orders.service;

import edu.maestria.observabilidad.lab33.Ids;
import edu.maestria.observabilidad.lab33.TraceMdc;
import edu.maestria.observabilidad.orders.client.FulfillmentClient;
import edu.maestria.observabilidad.orders.client.FulfillmentException;
import edu.maestria.observabilidad.orders.domain.OrderView;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderCoordinator {

    private static final Logger log = LoggerFactory.getLogger(OrderCoordinator.class);

    private final FulfillmentClient client;
    private final Tracer tracer;

    public OrderCoordinator(FulfillmentClient client, Tracer tracer) {
        this.client = client;
        this.tracer = tracer;
    }

    public OrderView coordinate(String orderId) {
        if (!Ids.validOrderId(orderId)) {
            throw new InvalidOrderIdException(orderId);
        }
        Span span = tracer.spanBuilder("order.coordinate").startSpan();
        try (var scope = span.makeCurrent(); var ignored = new TraceMdc(span)) {
            span.setAttribute("order.id", orderId);
            OrderView view = client.fetch(orderId);
            log.info("Pedido coordinado orderId={} sku={} sufficient={}", orderId, view.productSku(), view.stockSufficient());
            return new OrderView(
                    view.orderId(), view.productSku(), view.quantity(), view.status(),
                    view.available(), view.warehouse(), view.stockSufficient(),
                    view.latencyInjectedMs(), Ids.traceId());
        } catch (FulfillmentException exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR);
            log.error("Fallo de fulfillment orderId={} status={}", orderId, exception.status());
            throw exception;
        } finally {
            span.end();
        }
    }
}
