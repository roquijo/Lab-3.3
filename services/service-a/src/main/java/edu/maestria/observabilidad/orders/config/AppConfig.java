package edu.maestria.observabilidad.orders.config;

import edu.maestria.observabilidad.lab33.ChaosPolicy;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class AppConfig {

    @Bean
    OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }

    @Bean
    Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("edu.maestria.observabilidad.orders", "1.0.0");
    }

    @Bean
    Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.meterBuilder("edu.maestria.observabilidad.orders")
                .setInstrumentationVersion("1.0.0")
                .build();
    }

    @Bean
    ChaosPolicy chaosPolicy(@Value("${lab.chaos.armed:false}") boolean armed) {
        return new ChaosPolicy(armed, ThreadLocalRandom.current()::nextDouble);
    }

    @Bean
    RestClient restClient(RestClient.Builder builder, @Value("${lab.service-b-url}") String serviceBUrl) {
        return builder.baseUrl(serviceBUrl)
                .requestFactory(clientHttpRequestFactory())
                .build();
    }

    private org.springframework.http.client.ClientHttpRequestFactory clientHttpRequestFactory() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }
}
