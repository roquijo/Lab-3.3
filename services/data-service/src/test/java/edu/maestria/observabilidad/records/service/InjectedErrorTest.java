package edu.maestria.observabilidad.records.service;

import edu.maestria.observabilidad.lab33.ChaosPolicy;
import edu.maestria.observabilidad.records.domain.RecordView;
import edu.maestria.observabilidad.records.repository.RecordRepository;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InjectedErrorTest {

    private final RecordRepository repository = mock(RecordRepository.class);

    @Test
    void aMarkedRequestFailsBeforeTouchingTheDatabase() {
        ChaosPolicy chaos = new ChaosPolicy(true, () -> 0.0);
        chaos.setErrors(true);
        RecordService service = service(chaos);
        assertThrows(InjectedFailureException.class, () -> service.load("ord-001", ChaosPolicy.ERROR_EXPERIMENT));
        verify(repository, never()).findOrder("ord-001");
    }

    @Test
    void anUnmarkedRequestStillReads() {
        ChaosPolicy chaos = new ChaosPolicy(true, () -> 0.0);
        chaos.setErrors(true);
        when(repository.findOrder("ord-001")).thenReturn(List.of(
                new RecordView("ord-001", "LAPTOP-X1", 1, "delivered", 15, "WH-BOGOTA", true, 0, null)));
        RecordView view = service(chaos).load("ord-001", null);
        assert view.stockSufficient();
    }

    private RecordService service(ChaosPolicy chaos) {
        OpenTelemetry telemetry = OpenTelemetry.noop();
        return new RecordService(repository, chaos, telemetry.getTracer("test"), telemetry.getMeter("test"));
    }
}
