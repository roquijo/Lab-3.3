package edu.maestria.observabilidad.orders.service;

import edu.maestria.observabilidad.orders.client.FulfillmentClient;
import edu.maestria.observabilidad.orders.client.FulfillmentException;
import edu.maestria.observabilidad.orders.domain.OrderView;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderCoordinatorTest {

    private final FulfillmentClient client = mock(FulfillmentClient.class);
    private final OrderCoordinator coordinator = new OrderCoordinator(client, OpenTelemetry.noop().getTracer("test"));

    @Test
    void delegatesToServiceB() {
        when(client.fetch("ord-001")).thenReturn(new OrderView(
                "ord-001", "LAPTOP-X1", 1, "delivered", 15, "WH-BOGOTA", true, 0, null));
        OrderView view = coordinator.coordinate("ord-001");
        assertEquals("LAPTOP-X1", view.productSku());
        verify(client).fetch("ord-001");
    }

    @Test
    void rejectsInvalidIdBeforeCallingDownstream() {
        assertThrows(InvalidOrderIdException.class, () -> coordinator.coordinate("../etc"));
    }

    @Test
    void marksDependencyFailure() {
        when(client.fetch("ord-001")).thenThrow(new FulfillmentException(500));
        assertThrows(FulfillmentException.class, () -> coordinator.coordinate("ord-001"));
    }
}
