package edu.maestria.observabilidad.fulfillment.web;

import edu.maestria.observabilidad.fulfillment.client.DataClientException;
import edu.maestria.observabilidad.fulfillment.service.InvalidOrderIdException;
import edu.maestria.observabilidad.lab33.ChaosDisarmedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidOrderIdException.class)
    ResponseEntity<Map<String, String>> invalid() {
        return ResponseEntity.badRequest().body(Map.of("error", "identificador no válido"));
    }

    @ExceptionHandler(DataClientException.class)
    ResponseEntity<Map<String, String>> downstream(DataClientException exception) {
        HttpStatus status = exception.status() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(ChaosDisarmedException.class)
    ResponseEntity<Map<String, String>> disarmed(ChaosDisarmedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }
}
