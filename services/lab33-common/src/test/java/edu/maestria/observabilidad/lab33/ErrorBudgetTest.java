package edu.maestria.observabilidad.lab33;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorBudgetTest {

    @Test
    void consumesTheBudgetOfA99PercentSlo() {
        double consumed = ErrorBudget.consumedFraction(1000, 10, 0.99);
        assertEquals(1.0, consumed, 0.0001);
        double extrapolated = ErrorBudget.extrapolatedFraction(consumed, 120, 30L * 24 * 60 * 60);
        assertEquals(120.0 / (30.0 * 24 * 60 * 60), extrapolated, 0.0000001);
    }
}
