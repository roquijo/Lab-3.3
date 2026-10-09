package edu.maestria.observabilidad.orders.client;

public class FulfillmentException extends RuntimeException {

    private final int status;

    public FulfillmentException(int status) {
        super("service-b respondió HTTP " + status);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
