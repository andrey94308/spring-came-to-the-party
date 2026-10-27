package org.mato.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end "the secret is intentionally placed" tests for archive-service.
 *
 * Each flag asserts both:
 *   (a) a positive case — the flag IS reachable through its intended exploit path
 *   (b) a negative case — the flag is NOT reachable through the honest path
 *
 * Spins up real MariaDB via Testcontainers so the SQL-injection assertion
 * exercises the real database engine, not an H2 stand-in.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class IntegrationFlagsTest {

    @Container
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("sctp")
            .withUsername("sctp")
            .withPassword("sctp");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mariadb::getJdbcUrl);
        r.add("spring.datasource.username", mariadb::getUsername);
        r.add("spring.datasource.password", mariadb::getPassword);
        r.add("flags.flag1", () -> "FLAG{test-flag1}");
        r.add("flags.flag2", () -> "FLAG{test-flag2}");
        r.add("flags.flag3", () -> "FLAG{test-flag3}");
        r.add("seed.weak-password", () -> "sunshine");
        r.add("seed.command-url", () -> "http://command-control:8082");
        r.add("seed.command-president-password",
                () -> "correct horse battery staple but longer than seventy two bytes for the timing leak demo");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Value("${flags.flag1}") String flag1;
    @Value("${flags.flag2}") String flag2;
    @Value("${flags.flag3}") String flag3;

    // ---- /users ---------------------------------------------------------

    @Test
    void usersInPublicHidesHashesAndFlag1() throws Exception {
        MvcResult r = mvc.perform(get("/users").header("X-Debug-Trace", "false"))
                .andExpect(status().isOk())
                .andReturn();
        String body = r.getResponse().getContentAsString();
        JsonNode tree = json.readTree(body);

        assertThat(tree.get("mode").asText()).isEqualTo("public");
        assertThat(tree.get("flag").isNull()).isTrue();
        assertThat(body).doesNotContain(flag1);
        assertThat(body).doesNotContain("passwordHash");
    }

    @Test
    void usersInDebugTraceExposesHashesAndFlag1() throws Exception {
        MvcResult r = mvc.perform(get("/users").header("X-Debug-Trace", "true"))
                .andExpect(status().isOk())
                .andReturn();
        String body = r.getResponse().getContentAsString();
        assertThat(body).contains(flag1);
        assertThat(body).contains("passwordHash");
        assertThat(body).contains("$2a$10$"); // BCrypt prefix
    }

    @Test
    void usersDefaultsToPublic() throws Exception {
        // No header == public by default.
        MvcResult r = mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(r.getResponse().getContentAsString()).doesNotContain(flag1);
    }

    // ---- /notes ---------------------------------------------------------

    @Test
    void notesRequireAuth() throws Exception {
        mvc.perform(get("/notes")).andExpect(status().isUnauthorized());
    }

    @Test
    void notesForBobContainFlag2() throws Exception {
        MvcResult r = mvc.perform(get("/notes")
                        .header("Authorization", basic("bob", "sunshine")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(r.getResponse().getContentAsString()).contains(flag2);
    }

    @Test
    void notesForAnotherUserDoNotContainFlag2() throws Exception {
        MvcResult r = mvc.perform(get("/notes")
                        .header("Authorization", basic("m.lehtinen", "d5jW#9pqL!2zX")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(r.getResponse().getContentAsString()).doesNotContain(flag2);
    }

    // ---- /documents/search ---------------------------------------------

    @Test
    void documentSearchRequiresAuth() throws Exception {
        mvc.perform(post("/documents/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"x\",\"department\":\"intel-analysis\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void documentSearchHonestQueryHidesSpecialOpsDoc() throws Exception {
        MvcResult r = mvc.perform(post("/documents/search")
                        .header("Authorization", basic("bob", "sunshine"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"briefing\",\"department\":\"intel-analysis\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = r.getResponse().getContentAsString();
        assertThat(body).doesNotContain(flag3);
        assertThat(body).doesNotContain("mato-special-ops");
        // ensure the intel-analysis docs ARE present
        assertThat(body).contains("intel-analysis");
    }

    @Test
    void documentSearchSqlInjectionExposesSpecialOpsDocAndFlag3() throws Exception {
        // The CVE-2026-22730 payload — value contains a single quote that
        // breaks out of the SQL string literal.
        String payload = "{\"query\":\"x\",\"department\":\"intel-analysis' OR '1'='1\"}";
        MvcResult r = mvc.perform(post("/documents/search")
                        .header("Authorization", basic("bob", "sunshine"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        String body = r.getResponse().getContentAsString();
        assertThat(body).contains(flag3);
        assertThat(body).contains("mato-special-ops");
    }

    @Test
    void documentSearchTargetedInjectionFiltersToSpecialOpsOnly() throws Exception {
        String payload = "{\"query\":\"x\",\"department\":\"intel-analysis' OR JSON_VALUE(metadata, '$.department')='mato-special-ops\"}";
        MvcResult r = mvc.perform(post("/documents/search")
                        .header("Authorization", basic("bob", "sunshine"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        String body = r.getResponse().getContentAsString();
        assertThat(body).contains(flag3);
        assertThat(body).contains("OPERATION ZIRCON");
    }

    // ---- actuator -------------------------------------------------------

    @Test
    void actuatorHealthIsOpen() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UP")));
    }

    @Test
    void actuatorEnvIsClosed() throws Exception {
        // The archive-service does NOT expose env (only the gateway does, by design).
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }

    // ---- helpers --------------------------------------------------------

    private static String basic(String user, String pw) {
        return "Basic " + java.util.Base64.getEncoder().encodeToString(
                (user + ":" + pw).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
