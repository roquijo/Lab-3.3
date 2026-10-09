package edu.maestria.observabilidad.orders.web;

import edu.maestria.observabilidad.orders.client.FulfillmentException;
import edu.maestria.observabilidad.orders.service.InvalidOrderIdException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidOrderIdException.class)
    ResponseEntity<Map<String, String>> invalid(InvalidOrderIdException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "identificador no válido"));
    }

    @ExceptionHandler(FulfillmentException.class)
    ResponseEntity<Map<String, String>> downstream(FulfillmentException exception) {
        HttpStatus status = exception.status() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", exception.getMessage()));
    }
}
