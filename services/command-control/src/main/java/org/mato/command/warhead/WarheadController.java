package org.mato.command.warhead;

import io.jsonwebtoken.JwtException;
import java.util.Map;
import org.mato.command.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class WarheadController {

    private final JwtService jwt;
    private final String flag4;

    public WarheadController(JwtService jwt, @Value("${flags.flag4}") String flag4) {
        this.jwt = jwt;
        this.flag4 = flag4;
    }

    @GetMapping("/warheads")
    public Map<String, String> warheads(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "bearer required");
        }
        String token = auth.substring("Bearer ".length()).trim();
        String user;
        try {
            user = jwt.parseUsername(token);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }
        if (!"mr.president".equals(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not authorised");
        }
        return Map.of("warheads", "armed",
                      "flag", flag4);
    }
}
