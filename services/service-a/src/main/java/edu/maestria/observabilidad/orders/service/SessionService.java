package edu.maestria.observabilidad.orders.service;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final byte[] expected;
    private final LongCounter failures;

    public SessionService(@Value("${lab.api-token}") String expectedToken, Meter meter) {
        this.expected = expectedToken.getBytes(StandardCharsets.UTF_8);
        this.failures = meter.counterBuilder("auth.failures")
                .setDescription("Intentos de autenticación rechazados sin registrar el secreto")
                .build();
    }

    public void open(String presentedToken) {
        byte[] actual = presentedToken == null ? new byte[0] : presentedToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            failures.add(1);
            log.warn("autenticacion rechazada");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        log.info("sesion de laboratorio aceptada");
    }
}
