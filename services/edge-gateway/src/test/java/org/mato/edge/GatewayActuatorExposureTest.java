package org.mato.edge;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Asserts that the intended demonstration misconfiguration is present:
 *
 *   1. /actuator/gateway/routes is reachable without authentication
 *   2. /actuator/env is reachable without authentication (post-exploit observability)
 *   3. mato.gateway.audit.debug-trace is reachable through @systemProperties
 *      (so the CVE-2025-41243 SpEL payload writes there with the canonical
 *      indexer syntax)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "ARCHIVE_URI=http://localhost:1")
class GatewayActuatorExposureTest {

    @Autowired Environment env;

    @Test
    void actuatorGatewayRoutesIsReachableUnauthenticated(@Autowired WebTestClient client) {
        client.get().uri("/actuator/gateway/routes")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void actuatorEnvIsReachableUnauthenticated(@Autowired WebTestClient client) {
        client.get().uri("/actuator/env")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void auditTraceFlagDefaultsToFalseAndIsSpelWritable() {
        // The initializer seeds the property to "false" by default. The
        // CVE-2025-41243 exploit writes "true" via @systemProperties[...].
        assertThat(env.getProperty(AuditTraceInitializer.PROPERTY)).isEqualTo("false");
        assertThat(System.getProperties().getProperty(AuditTraceInitializer.PROPERTY))
                .as("audit-trace flag must live in System.getProperties() so the "
                        + "SpEL indexer payload @systemProperties[...] can write to it")
                .isEqualTo("false");
    }
}
