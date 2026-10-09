package edu.maestria.observabilidad.lab33;

public class ChaosDisarmedException extends RuntimeException {

    public ChaosDisarmedException() {
        super("El experimento está desarmado. Hace falta CHAOS_ARMED=true y no es el valor por defecto.");
    }
}
