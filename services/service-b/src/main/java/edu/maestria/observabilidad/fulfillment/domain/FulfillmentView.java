package edu.maestria.observabilidad.fulfillment.domain;

public record FulfillmentView(
        String orderId,
        String productSku,
        int quantity,
        String status,
        int available,
        String warehouse,
        boolean stockSufficient,
        int latencyInjectedMs,
        String traceId
) {
}
