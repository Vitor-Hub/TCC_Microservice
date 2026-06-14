package com.mstcc.friendshipms.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures that HTTP server request observations are properly wired to the
 * Micrometer {@link MeterRegistry}, guaranteeing that the labels {@code status}
 * and {@code outcome} are populated on the {@code http_server_requests_seconds}
 * family of metrics exported to Prometheus.
 *
 * <p>In Spring Boot 3.x the HTTP metrics pipeline migrated from the legacy
 * {@code WebMvcTagsProvider} to the Observability API
 * ({@link io.micrometer.observation.Observation}). The
 * {@code DefaultServerRequestObservationConvention} produces {@code status} and
 * {@code outcome} as {@code KeyValue}s, but they only reach the Prometheus
 * exporter if a {@link DefaultMeterObservationHandler} is registered in the
 * {@link ObservationRegistry}. Without {@code micrometer-tracing} on the
 * classpath, Spring Boot's auto-configure does not always register this handler
 * automatically, leaving the registry with no handler capable of converting
 * observations into meter measurements — so {@code status} and {@code outcome}
 * appear as absent or {@code UNKNOWN} in the scraped output.</p>
 *
 * <p>This bean uses {@link ObservationRegistryCustomizer} (the preferred
 * Boot-idiomatic hook) to add the handler after the registry is fully
 * auto-configured, avoiding ordering issues with other customizers.</p>
 */
@Configuration
public class ObservabilityConfig {

    /**
     * Registers a {@link DefaultMeterObservationHandler} in the global
     * {@link ObservationRegistry}.
     *
     * <p>This handler converts every completed {@code Observation} into
     * corresponding Micrometer timer/counter samples, including the
     * {@code status} and {@code outcome} key-values produced by
     * {@code DefaultServerRequestObservationConvention} for each HTTP
     * server request.</p>
     *
     * @param meterRegistry the auto-configured Prometheus-backed registry
     * @return a customizer that wires the handler into the observation registry
     */
    @Bean
    public ObservationRegistryCustomizer<ObservationRegistry> observationRegistryCustomizer(
            MeterRegistry meterRegistry) {
        return registry ->
                registry.observationConfig()
                        .observationHandler(new DefaultMeterObservationHandler(meterRegistry));
    }
}
