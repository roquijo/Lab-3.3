package edu.maestria.observabilidad.lab33;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;

/**
 * Los experimentos nacen apagados. Si el proceso no está armado, ninguna llamada
 * puede inyectar latencia ni errores. La tasa de error, cuando se enciende, es
 * fija del 10 % y solo aplica a peticiones que traen la cabecera de experimento.
 */
public final class ChaosPolicy {

    public static final String EXPERIMENT_HEADER = "X-Lab-Experiment";
    public static final String ERROR_EXPERIMENT = "error-rate";
    public static final double ERROR_RATE = 0.10;
    public static final int LATENCY_MS = 200;

    private final boolean armed;
    private final DoubleSupplier random;
    private final AtomicBoolean latencyOn = new AtomicBoolean(false);
    private final AtomicInteger latencyMs = new AtomicInteger(0);
    private final AtomicBoolean errorsOn = new AtomicBoolean(false);
    private final AtomicLong latencyInjections = new AtomicLong();
    private final AtomicLong errorsInjected = new AtomicLong();
    private volatile Instant latencyEnabledAt;
    private volatile Instant errorsEnabledAt;

    public ChaosPolicy(boolean armed, DoubleSupplier random) {
        this.armed = armed;
        this.random = random;
    }

    public boolean armed() {
        return armed;
    }

    public synchronized void setLatency(boolean enabled) {
        ensureArmed();
        boolean active = enabled;
        latencyOn.set(active);
        latencyMs.set(active ? LATENCY_MS : 0);
        latencyEnabledAt = active ? Instant.now() : null;
    }

    public synchronized void setErrors(boolean enabled) {
        ensureArmed();
        errorsOn.set(enabled);
        errorsEnabledAt = enabled ? Instant.now() : null;
    }

    public synchronized void disarmRuntime() {
        latencyOn.set(false);
        latencyMs.set(0);
        errorsOn.set(false);
        latencyEnabledAt = null;
        errorsEnabledAt = null;
    }

    public int currentLatencyMs() {
        if (!armed || !latencyOn.get()) {
            return 0;
        }
        return latencyMs.get();
    }

    public boolean errorsEnabled() {
        return armed && errorsOn.get();
    }

    public Instant latencyEnabledAt() {
        return latencyEnabledAt;
    }

    public Instant errorsEnabledAt() {
        return errorsEnabledAt;
    }

    public long latencyInjections() {
        return latencyInjections.get();
    }

    public long errorsInjected() {
        return errorsInjected.get();
    }

    public int applyLatency() {
        int delay = currentLatencyMs();
        if (delay <= 0) {
            return 0;
        }
        latencyInjections.incrementAndGet();
        return delay;
    }

    public boolean shouldFail(String experimentHeader) {
        if (!errorsEnabled() || !ERROR_EXPERIMENT.equals(experimentHeader)) {
            return false;
        }
        if (random.getAsDouble() >= ERROR_RATE) {
            return false;
        }
        errorsInjected.incrementAndGet();
        return true;
    }

    private void ensureArmed() {
        if (!armed) {
            throw new ChaosDisarmedException();
        }
    }
}
