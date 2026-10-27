package org.mato.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mato.command.otp.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class OtpAndWarheadTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OtpService otp;
    @Value("${flags.flag4}") String flag4;

    @Test
    void warheadsRejectsRequestsWithoutBearerToken() throws Exception {
        mvc.perform(get("/warheads")).andExpect(status().isUnauthorized());
    }

    @Test
    void warheadsRejectsForgedTokens() throws Exception {
        mvc.perform(get("/warheads").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyOtpRejectsWrongCode() throws Exception {
        String body = json.writeValueAsString(java.util.Map.of(
                "username", "mr.president", "otp", "0000"));
        // 0000 might happen to be the current code 1/10000 of the time; pick
        // a deliberately-wrong code derived from the current code.
        String currentCode = otp.currentCode();
        String wrong = String.format("%04d", (Integer.parseInt(currentCode) + 1) % 10000);
        body = json.writeValueAsString(java.util.Map.of(
                "username", "mr.president", "otp", wrong));
        mvc.perform(post("/verify-otp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyOtpAcceptsCurrentCodeAndIssuesJwt() throws Exception {
        String currentCode = otp.currentCode();
        String body = json.writeValueAsString(java.util.Map.of(
                "username", "mr.president", "otp", currentCode));
        MvcResult r = mvc.perform(post("/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tree = json.readTree(r.getResponse().getContentAsString());
        String jwt = tree.get("token").asText();
        assertThat(jwt).contains(".");

        // The JWT actually unlocks /warheads.
        MvcResult flagged = mvc.perform(get("/warheads")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(flagged.getResponse().getContentAsString()).contains(flag4);
    }

    @Test
    void verifyOtpRejectsWrongUsernameEvenWithRightCode() throws Exception {
        String currentCode = otp.currentCode();
        String body = json.writeValueAsString(java.util.Map.of(
                "username", "alice", "otp", currentCode));
        mvc.perform(post("/verify-otp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }
}
