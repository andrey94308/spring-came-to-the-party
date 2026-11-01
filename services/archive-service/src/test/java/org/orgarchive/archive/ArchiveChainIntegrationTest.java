package org.orgarchive.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ArchiveChainIntegrationTest {
    @Container static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("orgarchive").withUsername("orgarchive").withPassword("orgarchive");

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("seed.final-flag", () -> "FLAG{integration-final}");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test void signupAndMethodAuthorizationBoundary() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isUnauthorized());
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        String password = "UserPass!234";
        String response = mvc.perform(post("/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username,
                                "displayName", "Demo Visitor", "email", username + "@example.test",
                                "password", password))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String organizationUuid = json.readTree(response).get("organizationUuid").asText();
        assertThat(UUID.fromString(organizationUuid).toString()).isEqualTo(organizationUuid);
        String ownDocuments = mvc.perform(post("/documents/search")
                        .header("Authorization", basic(username, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("organizationUuid", organizationUuid))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(ownDocuments).isEqualTo("[]");
        int expected = Boolean.getBoolean("demo.fixed-security") ? 403 : 200;
        String userList = mvc.perform(get("/users?debug=true")
                        .header("Authorization", basic(username, password)))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        if (expected == 200) {
            JsonNode users = json.readTree(userList);
            JsonNode anna = null;
            for (JsonNode user : users) {
                if ("anna".equals(user.get("username").asText())) anna = user;
            }
            assertThat(anna).isNotNull();
            assertThat(anna.get("passwordHash").asText()).startsWith("$2");
            assertThat(anna.has("organizationUuid")).isFalse();
        }
        mvc.perform(get("/users/anna").header("Authorization", basic(username, password)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username,
                                "displayName", "Duplicate", "email", "duplicate@example.test",
                                "password", password))))
                .andExpect(status().isConflict());
        String rejectedPrivilegeFields = mvc.perform(post("/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "evil-admin",
                                "displayName", "Bad Input", "email", "bad@example.test",
                                "password", password, "role", "admin", "organizationUuid", "786298a5-8c02-56ce-ba8f-d2b41ec3333c"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(rejectedPrivilegeFields).get("organizationUuid").asText())
                .isNotEqualTo("786298a5-8c02-56ce-ba8f-d2b41ec3333c");
        mvc.perform(get("/users").header("Authorization", basic("evil-admin", password)))
                .andExpect(status().is(expected));
    }

    @Test void adminDebugAndTenantSearch() throws Exception {
        String standard = mvc.perform(get("/users").header("Authorization", basic("admin", "Adm!nQ8vL2n$R7p")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(standard).doesNotContain("passwordHash", "organizationUuid");
        String debug = mvc.perform(get("/users?debug=true").header("Authorization", basic("admin", "Adm!nQ8vL2n$R7p")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(debug).contains("passwordHash", "anna").doesNotContain("organizationUuid");
        String direct = mvc.perform(get("/users/anna")
                        .header("Authorization", basic("admin", "Adm!nQ8vL2n$R7p")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(direct).doesNotContain("organizationUuid");
        String annaUuid = "786298a5-8c02-56ce-ba8f-d2b41ec3333c";
        String foreignUuid = UUID.randomUUID().toString();
        assertThat(search("/documents/search", foreignUuid)).isEqualTo("[]");
        assertThat(search("/documents/search", annaUuid))
                .contains(annaUuid).doesNotContain("d3302858-83b5-5ec5-aba6-1f7c9da45bd0");
        assertThat(search("/documents/search", foreignUuid + "' OR '1'='1"))
                .contains(annaUuid, "1ae53893-c399-57c3-ba32-16e55b435249", "aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b",
                        "768fc845-aff9-5b32-b626-a8f05a8778d2", "d3302858-83b5-5ec5-aba6-1f7c9da45bd0",
                        "FLAG{integration-final}");
        assertThat(search("/documents/search-safe", foreignUuid + "' OR '1'='1")).isEqualTo("[]");
    }

    private String search(String path, String organizationUuid) throws Exception {
        String body = json.writeValueAsString(Map.of("organizationUuid", organizationUuid));
        return mvc.perform(post(path).header("Authorization", basic("anna", "sunshine"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
