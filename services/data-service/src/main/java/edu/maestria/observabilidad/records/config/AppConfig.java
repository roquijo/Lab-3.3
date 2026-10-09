package edu.maestria.observabilidad.records.config;

import edu.maestria.observabilidad.lab33.ChaosPolicy;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class AppConfig {

    @Bean
    OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }

    @Bean
    Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("edu.maestria.observabilidad.records", "1.0.0");
    }

    @Bean
    Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.meterBuilder("edu.maestria.observabilidad.records")
                .setInstrumentationVersion("1.0.0")
                .build();
    }

    @Bean
    ChaosPolicy chaosPolicy(@Value("${lab.chaos.armed:false}") boolean armed) {
        return new ChaosPolicy(armed, ThreadLocalRandom.current()::nextDouble);
    }
}
