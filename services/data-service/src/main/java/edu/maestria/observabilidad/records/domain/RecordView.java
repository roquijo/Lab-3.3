package edu.maestria.observabilidad.records.domain;

public record RecordView(
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
