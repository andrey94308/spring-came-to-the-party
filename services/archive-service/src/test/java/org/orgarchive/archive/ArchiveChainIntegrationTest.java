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
        mvc.perform(get("/notes")).andExpect(status().isUnauthorized());
        String username = "visitor-" + UUID.randomUUID().toString().substring(0, 8);
        String password = "VisitorPass!234";
        String response = mvc.perform(post("/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username,
                                "displayName", "Demo Visitor", "email", username + "@example.test",
                                "password", password))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String organization = json.readTree(response).get("organization").asText();
        assertThat(organization).startsWith("trial-");
        mvc.perform(get("/organizations/" + organization + "/notes")
                        .header("Authorization", basic(username, password)))
                .andExpect(status().isOk());
        mvc.perform(get("/organizations/atlas-consulting/notes")
                        .header("Authorization", basic(username, password)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/notes").header("Authorization", basic(username, password)))
                .andExpect(status().isOk());
        int expected = Boolean.getBoolean("demo.fixed-security") ? 403 : 200;
        String roster = mvc.perform(get("/users?debug=true")
                        .header("Authorization", basic(username, password)))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        if (expected == 200) {
            JsonNode users = json.readTree(roster);
            JsonNode anna = null;
            for (JsonNode user : users) {
                if ("anna".equals(user.get("username").asText())) anna = user;
            }
            assertThat(anna).isNotNull();
            assertThat(anna.get("passwordHash").asText()).startsWith("$2");
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
                                "password", password, "role", "admin", "organization", "atlas-consulting"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(rejectedPrivilegeFields).get("organization").asText()).startsWith("trial-");
        mvc.perform(get("/users").header("Authorization", basic("evil-admin", password)))
                .andExpect(status().is(expected));
    }

    @Test void adminDebugAndTenantSearch() throws Exception {
        String standard = mvc.perform(get("/users").header("Authorization", basic("admin", "Adm!nQ8vL2n$R7p")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(standard).doesNotContain("passwordHash");
        String debug = mvc.perform(get("/users?debug=true").header("Authorization", basic("admin", "Adm!nQ8vL2n$R7p")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(debug).contains("passwordHash", "anna");
        mvc.perform(get("/users/anna").header("Authorization", basic("admin", "Adm!nQ8vL2n$R7p")))
                .andExpect(status().isOk());
        mvc.perform(get("/notes").header("Authorization", basic("admin", "Adm!nQ8vL2n$R7p")))
                .andExpect(status().isForbidden());
        String annaNotes = notes("anna", "sunshine");
        assertThat(json.readTree(annaNotes)).isEqualTo(json.readTree(notes("m.lehtinen", "d5jW#9pqL!2zX")));
        assertThat(annaNotes).contains("Report index");
        assertThat(notes("r.dubois", "Tr0ub4dor&3xZ!")).contains("Fuel review").doesNotContain("Report index");
        assertThat(search("/documents/search", "harbor-delivery")).isEqualTo("[]");
        assertThat(search("/documents/search", "atlas-consulting"))
                .contains("atlas-consulting").doesNotContain("meridian-legal");
        assertThat(search("/documents/search", "atlas-consulting' OR '1'='1"))
                .contains("atlas-consulting", "harbor-delivery", "cedar-partners",
                        "siteworks-group", "meridian-legal", "FLAG{integration-final}");
        assertThat(search("/documents/search-safe", "atlas-consulting' OR '1'='1")).isEqualTo("[]");
    }

    private String notes(String username, String password) throws Exception {
        return mvc.perform(get("/notes").header("Authorization", basic(username, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String search(String path, String organization) throws Exception {
        String body = json.writeValueAsString(Map.of("organization", organization));
        return mvc.perform(post(path).header("Authorization", basic("anna", "sunshine"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
