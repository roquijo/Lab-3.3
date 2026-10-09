package edu.maestria.observabilidad.lab33;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChaosPolicyTest {

    @Test
    void disarmedRejectsActivationAndDoesNotInject() {
        ChaosPolicy policy = new ChaosPolicy(false, () -> 0.0);
        assertThrows(ChaosDisarmedException.class, () -> policy.setLatency(true));
        assertThrows(ChaosDisarmedException.class, () -> policy.setErrors(true));
        assertEquals(0, policy.applyLatency());
        assertFalse(policy.shouldFail(ChaosPolicy.ERROR_EXPERIMENT));
    }

    @Test
    void latencyIsExactly200msAndCanBeStopped() {
        ChaosPolicy policy = new ChaosPolicy(true, () -> 0.99);
        policy.setLatency(true);
        assertEquals(200, policy.applyLatency());
        policy.disarmRuntime();
        assertEquals(0, policy.applyLatency());
        assertEquals(1, policy.latencyInjections());
    }

    @Test
    void errorsRequireTheExperimentHeaderAndStayAtTenPercent() {
        ChaosPolicy always = new ChaosPolicy(true, () -> 0.0);
        always.setErrors(true);
        assertFalse(always.shouldFail(null));
        assertFalse(always.shouldFail("latency"));
        assertTrue(always.shouldFail(ChaosPolicy.ERROR_EXPERIMENT));

        ChaosPolicy never = new ChaosPolicy(true, () -> 0.10);
        never.setErrors(true);
        assertFalse(never.shouldFail(ChaosPolicy.ERROR_EXPERIMENT));
    }
}
