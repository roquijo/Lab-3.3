package edu.maestria.observabilidad.fulfillment.client;

public class DataClientException extends RuntimeException {

    private final int status;

    public DataClientException(int status) {
        super("data-service respondió HTTP " + status);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
