package edu.maestria.observabilidad.fulfillment.service;

import edu.maestria.observabilidad.fulfillment.client.DataClient;
import edu.maestria.observabilidad.fulfillment.domain.FulfillmentView;
import edu.maestria.observabilidad.lab33.ChaosPolicy;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FulfillmentServiceTest {

    private final DataClient dataClient = mock(DataClient.class);

    @Test
    void doesNotSleepWhenDisarmed() {
        ChaosPolicy chaos = new ChaosPolicy(false, () -> 0.0);
        FulfillmentService service = service(chaos);
        when(dataClient.load("ord-001", null)).thenReturn(sample(0));
        long started = System.nanoTime();
        FulfillmentView view = service.prepare("ord-001", null);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        assertEquals(0, view.latencyInjectedMs());
        assertEquals(true, elapsedMs < 150);
        verify(dataClient).load("ord-001", null);
    }

    @Test
    void sleeps200msOnlyAfterTheExperimentIsEnabled() {
        ChaosPolicy chaos = new ChaosPolicy(true, () -> 0.0);
        chaos.setLatency(true);
        FulfillmentService service = service(chaos);
        when(dataClient.load("ord-001", "error-rate")).thenReturn(sample(0));
        long started = System.nanoTime();
        FulfillmentView view = service.prepare("ord-001", "error-rate");
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        assertEquals(200, view.latencyInjectedMs());
        assertEquals(true, elapsedMs >= 180);
    }

    private FulfillmentService service(ChaosPolicy chaos) {
        OpenTelemetry telemetry = OpenTelemetry.noop();
        return new FulfillmentService(dataClient, chaos, telemetry.getTracer("test"), telemetry.getMeter("test"));
    }

    private FulfillmentView sample(int delay) {
        return new FulfillmentView("ord-001", "LAPTOP-X1", 1, "delivered", 15, "WH-BOGOTA", true, delay, null);
    }
}
