package edu.maestria.observabilidad.records.web;

import edu.maestria.observabilidad.lab33.ChaosDisarmedException;
import edu.maestria.observabilidad.records.service.InjectedFailureException;
import edu.maestria.observabilidad.records.service.InvalidOrderIdException;
import edu.maestria.observabilidad.records.service.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<Map<String, String>> missing() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "pedido no encontrado"));
    }

    @ExceptionHandler(InvalidOrderIdException.class)
    ResponseEntity<Map<String, String>> invalid() {
        return ResponseEntity.badRequest().body(Map.of("error", "identificador no válido"));
    }

    @ExceptionHandler(InjectedFailureException.class)
    ResponseEntity<Map<String, String>> injected() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "error inyectado por el experimento"));
    }

    @ExceptionHandler(ChaosDisarmedException.class)
    ResponseEntity<Map<String, String>> disarmed(ChaosDisarmedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }
}
