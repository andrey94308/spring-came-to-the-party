package org.mato.command.otp;

import java.util.Map;
import org.mato.command.jwt.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class OtpController {

    public record VerifyRequest(String username, String otp) {}

    private final OtpService otp;
    private final JwtService jwt;

    public OtpController(OtpService otp, JwtService jwt) {
        this.otp = otp;
        this.jwt = jwt;
    }

    @PostMapping("/verify-otp")
    public Map<String, String> verify(@RequestBody VerifyRequest req) {
        if (!"mr.president".equals(req.username()) || !otp.verify(req.otp())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "bad otp");
        }
        return Map.of("token", jwt.issue(req.username()));
    }
}
