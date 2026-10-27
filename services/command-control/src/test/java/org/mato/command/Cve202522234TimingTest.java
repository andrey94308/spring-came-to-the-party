package org.mato.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Asserts the username-enumeration timing leak (CVE-2025-22234 surface) is
 * observable end-to-end. With Spring Security 6.4.4 configured with
 * hideUserNotFoundExceptions=false, an existing user goes through BCrypt
 * (~250ms at cost 10) while a non-existent user fails fast (~2ms), leaving a
 * measurable gap that lets an attacker enumerate usernames.
 *
 * We assert a conservative gap (median > 25ms) so the test is reliable in CI
 * but still actually fails if the vulnerability is patched out.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Cve202522234TimingTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void existingUserTakesMeasurablyLongerThanAbsentUser() throws Exception {
        String wrongPw = "definitely-not-the-right-password";

        // warm up the BCrypt code path so first-run JIT noise doesn't dominate.
        for (int i = 0; i < 3; i++) {
            timeLogin("mr.president", wrongPw);
            timeLogin("nobody-here-at-all", wrongPw);
        }

        List<Long> presidentTimes = new ArrayList<>();
        List<Long> nobodyTimes = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            presidentTimes.add(timeLogin("mr.president", wrongPw));
            nobodyTimes.add(timeLogin("nobody-here-at-all", wrongPw));
        }

        long medianPresident = median(presidentTimes);
        long medianNobody = median(nobodyTimes);

        System.out.println("median present  user: " + medianPresident + " ms");
        System.out.println("median absent   user: " + medianNobody + " ms");
        System.out.println("gap                 : " + (medianPresident - medianNobody) + " ms");

        assertThat(medianPresident - medianNobody)
                .as("timing leak gap is the CVE-2025-22234 surface for username enumeration")
            .isGreaterThan(25);
    }

    private long timeLogin(String user, String pw) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("username", user, "password", pw));
        long t0 = System.nanoTime();
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(body));
        return (System.nanoTime() - t0) / 1_000_000;
    }

    private static long median(List<Long> xs) {
        var sorted = new ArrayList<>(xs);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }
}
