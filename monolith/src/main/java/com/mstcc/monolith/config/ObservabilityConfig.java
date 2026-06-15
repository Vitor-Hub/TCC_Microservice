package com.mstcc.monolith.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the {@link DefaultMeterObservationHandler} into the global
 * {@link ObservationRegistry} so that HTTP server request observations
 * are correctly converted into Micrometer timer/counter samples.
 *
 * <p>Without this, the {@code status} and {@code outcome} labels produced by
 * {@code DefaultServerRequestObservationConvention} would not reach the
 * Prometheus exporter when {@code micrometer-tracing} is absent from the
 * classpath — which is intentionally the case in this monolith to keep the
 * dependency surface minimal.
 *
 * <p>This is identical to the configuration used in each microservice,
 * ensuring that the Grafana queries that target
 * {@code http_server_requests_seconds} work the same way for both stacks.
 */
@Configuration
public class ObservabilityConfig {

    /**
     * Registers a {@link DefaultMeterObservationHandler} via the Boot-idiomatic
     * {@link ObservationRegistryCustomizer} hook, avoiding ordering issues with
     * other auto-configured customizers.
     *
     * @param meterRegistry the auto-configured Prometheus-backed registry
     * @return customizer that wires the handler into the observation registry
     */
    @Bean
    public ObservationRegistryCustomizer<ObservationRegistry> observationRegistryCustomizer(
            MeterRegistry meterRegistry) {
        return registry ->
                registry.observationConfig()
                        .observationHandler(new DefaultMeterObservationHandler(meterRegistry));
    }
}
