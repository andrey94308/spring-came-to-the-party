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
import org.orgarchive.archive.user.UserRepository;
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
    @Container static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.8")
            .withDatabaseName("orgarchive").withUsername("orgarchive").withPassword("orgarchive");

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("seed.final-flag", () -> "FLAG{integration-final}");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;

    @Test void signupAndMethodAuthorizationBoundary() throws Exception {
        String index = mvc.perform(get("/"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(index).contains("/signup", "/users", "/users/{id}",
                "/organizations", "/organizations/{id}",
                "/documents/search", "/documents/search-safe", "/documents/search-debug");
        mvc.perform(get("/users")).andExpect(status().isUnauthorized());
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        String password = "UserPass!234";
        String response = mvc.perform(post("/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username,
                                "displayName", "Demo Visitor", "email", username + "@example.test",
                                "password", password))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(response).has("organizationUuid")).isFalse();
        String organizationUuid = users.findById(username).orElseThrow().getOrganizationUuid();
        assertThat(UUID.fromString(organizationUuid).toString()).isEqualTo(organizationUuid);
        String ownDocuments = mvc.perform(post("/documents/search")
                        .header("Authorization", basic(username, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
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
                .andExpect(status().is(expected));
        mvc.perform(get("/organizations").header("Authorization", basic(username, password)))
                .andExpect(status().is(expected));
        mvc.perform(get("/organizations/" + organizationUuid)
                        .header("Authorization", basic(username, password)))
                .andExpect(status().is(expected));
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
        assertThat(json.readTree(rejectedPrivilegeFields).has("organizationUuid")).isFalse();
        assertThat(users.findById("evil-admin").orElseThrow().getOrganizationUuid())
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
        String organizations = mvc.perform(get("/organizations")
                        .header("Authorization", basic("admin", "Adm!nQ8vL2n$R7p")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(organizations).contains("786298a5-8c02-56ce-ba8f-d2b41ec3333c")
                .doesNotContain("accountCount");
        String organization = mvc.perform(get("/organizations/786298a5-8c02-56ce-ba8f-d2b41ec3333c")
                        .header("Authorization", basic("admin", "Adm!nQ8vL2n$R7p")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(organization).contains("accountCount");
        String annaUuid = "786298a5-8c02-56ce-ba8f-d2b41ec3333c";
        assertThat(search("/documents/search", "nonexistent-category")).isEqualTo("[]");
        assertThat(search("/documents/search", "report"))
                .contains(annaUuid).doesNotContain("d3302858-83b5-5ec5-aba6-1f7c9da45bd0");
        String filtered = mvc.perform(post("/documents/search")
                        .header("Authorization", basic("anna", "sunshine"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("category", "report", "minRevision", 5))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(filtered).size()).isGreaterThan(0);
        for (JsonNode doc : json.readTree(filtered)) {
            assertThat(doc.get("metadata").asText()).contains("\"category\":\"report\"");
            assertThat(json.readTree(doc.get("metadata").asText()).get("revision").asInt()).isGreaterThanOrEqualTo(5);
        }
        String revisionOnly = mvc.perform(post("/documents/search")
                        .header("Authorization", basic("anna", "sunshine"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("minRevision", 10))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(revisionOnly).size()).isGreaterThan(0);
        for (JsonNode doc : json.readTree(revisionOnly)) {
            JsonNode metadata = json.readTree(doc.get("metadata").asText());
            assertThat(metadata.get("organizationUuid").asText()).isEqualTo(annaUuid);
            assertThat(metadata.get("revision").asInt()).isGreaterThanOrEqualTo(10);
        }
        assertThat(search("/documents/search", "report' OR '1'='1"))
                .contains(annaUuid, "1ae53893-c399-57c3-ba32-16e55b435249", "aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b",
                        "768fc845-aff9-5b32-b626-a8f05a8778d2", "d3302858-83b5-5ec5-aba6-1f7c9da45bd0",
                        "FLAG{integration-final}");
        assertThat(search("/documents/search-safe", "report' OR '1'='1")).isEqualTo("[]");
    }

    private String search(String path, String category) throws Exception {
        String body = json.writeValueAsString(Map.of("category", category));
        return mvc.perform(post(path).header("Authorization", basic("anna", "sunshine"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
