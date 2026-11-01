package org.mato.edge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "ARCHIVE_URI=http://localhost:1")
class GatewayActuatorExposureTest {
    @Autowired WebTestClient client;
    @Test void actuatorRoutesAreClosed() {
        client.get().uri("/actuator/gateway/routes").exchange().expectStatus().isNotFound();
        client.get().uri("/actuator/env").exchange().expectStatus().isNotFound();
    }
}
