package edu.maestria.observabilidad.orders.service;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionServiceTest {

    private final SessionService sessions = new SessionService("lab-token", OpenTelemetry.noop().getMeter("test"));

    @Test
    void acceptsTheConfiguredTokenWithoutEchoingIt() {
        assertDoesNotThrow(() -> sessions.open("lab-token"));
    }

    @Test
    void rejectsAWrongToken() {
        assertThrows(ResponseStatusException.class, () -> sessions.open("otra-cosa"));
    }
}
