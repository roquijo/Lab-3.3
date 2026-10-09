package edu.maestria.observabilidad.orders.domain;

public record OrderView(
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
