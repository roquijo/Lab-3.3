package edu.maestria.observabilidad.lab33;

/**
 * SLO de éxito. El presupuesto es la fracción de fallos todavía permitida.
 * Consumo = errores observados / (total * (1 - SLO)).
 * La extrapolación reparte ese consumo como si el mismo ritmo durara toda la ventana del SLO.
 */
public final class ErrorBudget {

    private ErrorBudget() {
    }

    public static double allowedFailureRatio(double slo) {
        requireSlo(slo);
        return 1.0 - slo;
    }

    public static double consumedFraction(long total, long failures, double slo) {
        requireSlo(slo);
        if (total < 0 || failures < 0 || failures > total) {
            throw new IllegalArgumentException("totales incoherentes");
        }
        if (total == 0) {
            return 0.0;
        }
        double budget = total * allowedFailureRatio(slo);
        if (budget == 0.0) {
            return failures == 0 ? 0.0 : Double.POSITIVE_INFINITY;
        }
        return failures / budget;
    }

    public static double extrapolatedFraction(double consumedInExperiment, long experimentSeconds, long sloWindowSeconds) {
        if (experimentSeconds <= 0 || sloWindowSeconds <= 0) {
            throw new IllegalArgumentException("las ventanas deben ser positivas");
        }
        return consumedInExperiment * (experimentSeconds / (double) sloWindowSeconds);
    }

    private static void requireSlo(double slo) {
        if (slo <= 0.0 || slo > 1.0) {
            throw new IllegalArgumentException("el SLO debe estar en (0, 1]");
        }
    }
}
