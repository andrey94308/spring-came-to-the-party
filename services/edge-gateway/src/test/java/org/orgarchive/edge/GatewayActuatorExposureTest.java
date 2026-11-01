package org.orgarchive.edge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "ARCHIVE_URI=http://localhost:1")
class GatewayActuatorExposureTest {
    @Autowired WebTestClient client;
    @Test void publicIndexListsTheDemoRoutes() {
        client.get().uri("/").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.service").isEqualTo("OrgArchive")
                .jsonPath("$.endpoints[0].path").isEqualTo("/");
    }
    @Test void actuatorRoutesAreClosed() {
        client.get().uri("/actuator/gateway/routes").exchange().expectStatus().isNotFound();
        client.get().uri("/actuator/env").exchange().expectStatus().isNotFound();
    }
    @Test void internalOrganizationRoutesAreNotPublished() {
        client.get().uri("/organizations").exchange().expectStatus().isNotFound();
        client.get().uri("/organizations/786298a5-8c02-56ce-ba8f-d2b41ec3333c")
                .exchange().expectStatus().isNotFound();
    }
}
