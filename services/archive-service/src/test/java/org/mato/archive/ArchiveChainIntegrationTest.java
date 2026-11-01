package org.mato.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ArchiveChainIntegrationTest {
    @Container static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("sctp").withUsername("sctp").withPassword("sctp");
    @Container static final GenericContainer<?> ldap = new GenericContainer<>("osixia/openldap:1.5.0")
            .withEnv("LDAP_DOMAIN", "mato.int")
            .withEnv("LDAP_ADMIN_PASSWORD", "local-bootstrap-only")
            .withEnv("LDAP_TLS", "false")
            .withCommand("--copy-service")
            .withCopyFileToContainer(MountableFile.forHostPath(
                    Path.of("../../infra/ldap/bootstrap/00-allow-unauthenticated-bind.ldif").toAbsolutePath()),
                    "/container/service/slapd/assets/config/bootstrap/ldif/custom/00-allow-unauthenticated-bind.ldif")
            .withCopyFileToContainer(MountableFile.forHostPath(
                    Path.of("../../infra/ldap/bootstrap/01-people.ldif").toAbsolutePath()),
                    "/container/service/slapd/assets/config/bootstrap/ldif/custom/01-people.ldif")
            .withExposedPorts(389);
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mariadb::getJdbcUrl);
        r.add("spring.datasource.username", mariadb::getUsername);
        r.add("spring.datasource.password", mariadb::getPassword);
        r.add("ldap.url", () -> "ldap://" + ldap.getHost() + ":" + ldap.getMappedPort(389));
        r.add("seed.final-flag", () -> "FLAG{integration-final}");
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test void authenticationAndRoles() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/notes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users").header("Authorization", basic("unknown", "")))
                .andExpect(status().isUnauthorized());
        String roster = mvc.perform(get("/users").header("Authorization", basic("mr.president", "")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(roster).contains("passwordHash", "bob", "$2a$10$");
        mvc.perform(get("/notes").header("Authorization", basic("mr.president", "")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/documents/search").header("Authorization", basic("mr.president", "")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/users").header("Authorization", basic("bob", "sunshine")))
                .andExpect(status().isForbidden());
    }

    @Test void sharedNotesAndSpoofing() throws Exception {
        String bob = notes("bob", "sunshine", "evil");
        assertThat(json.readTree(bob)).isEqualTo(json.readTree(notes("m.lehtinen", "d5jW#9pqL!2zX", "logistics-corps")));
        assertThat(bob).contains("document search");
        assertThat(notes("r.dubois", "Tr0ub4dor&3xZ!", "intel-analysis"))
                .contains("Fuel review").doesNotContain("document search");
    }

    @Test void injectionCrossesDepartmentsButSafeQueryDoesNot() throws Exception {
        assertThat(search("/documents/search", "logistics-corps")).isEqualTo("[]");
        assertThat(search("/documents/search", "intel-analysis"))
                .contains("intel-analysis").doesNotContain("mato-special-ops");
        assertThat(search("/documents/search", "intel-analysis' OR '1'='1"))
                .contains("intel-analysis", "logistics-corps", "command-staff", "field-operations",
                        "mato-special-ops", "FLAG{integration-final}");
        assertThat(search("/documents/search-safe", "intel-analysis' OR '1'='1"))
                .isEqualTo("[]");
    }
    private String notes(String user, String password, String spoof) throws Exception {
        return mvc.perform(get("/notes?department=" + spoof).header("X-Department", spoof)
                .header("Authorization", basic(user, password))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
    private String search(String path, String department) throws Exception {
        String body = json.writeValueAsString(Map.of("query", "briefing", "department", department));
        return mvc.perform(post(path).header("Authorization", basic("bob", "sunshine"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }
}
