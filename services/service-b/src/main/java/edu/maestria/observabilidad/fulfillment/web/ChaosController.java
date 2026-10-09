package edu.maestria.observabilidad.fulfillment.web;

import edu.maestria.observabilidad.lab33.ChaosPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
public class ChaosController {

    private final ChaosPolicy chaos;
    private final byte[] token;

    public ChaosController(ChaosPolicy chaos, @Value("${lab.api-token}") String token) {
        this.chaos = chaos;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/api/chaos")
    public Map<String, Object> status() {
        return Map.of(
                "service", "service-b",
                "armed", chaos.armed(),
                "latencyMs", chaos.currentLatencyMs(),
                "latencyEnabledAt", chaos.latencyEnabledAt() == null ? "" : chaos.latencyEnabledAt().toString(),
                "injections", chaos.latencyInjections()
        );
    }

    @PostMapping("/api/chaos/latency")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void latency(@RequestHeader("X-Lab-Token") String presented, @RequestHeader("X-Lab-Enable") boolean enabled) {
        authorize(presented);
        chaos.setLatency(enabled);
    }

    @PostMapping("/api/chaos/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(@RequestHeader("X-Lab-Token") String presented) {
        authorize(presented);
        chaos.disarmRuntime();
    }

    private void authorize(String presented) {
        byte[] actual = presented == null ? new byte[0] : presented.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(token, actual)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
