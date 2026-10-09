package edu.maestria.observabilidad.lab33;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

public final class Ids {

    private Ids() {
    }

    public static boolean validOrderId(String orderId) {
        return orderId != null && orderId.matches("[A-Za-z0-9_-]{1,36}");
    }

    public static String traceId() {
        SpanContext context = Span.current().getSpanContext();
        return context.isValid() ? context.getTraceId() : null;
    }
}
