package org.mato.command.login;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Username/password authentication. CVE-2025-22234 reproduction.
 *
 * Spring Security 6.4.4's {@code DaoAuthenticationProvider#retrieveUser} is
 * declared {@code protected final} and unconditionally runs
 * {@code mitigateAgainstTimingAttack} when the user is absent — so we can't
 * defang it through subclassing or the {@code hideUserNotFoundExceptions}
 * flag alone. To make the leak observable for the CTF (and to mirror how the
 * CVE manifests in apps that bypass the AuthenticationManager and call
 * UserDetailsService + PasswordEncoder directly), this controller does the
 * matching itself: an absent user fails fast (no dummy BCrypt) while a
 * present user goes through BCrypt verification.
 *
 * Both components — {@link UserDetailsService} and {@link PasswordEncoder} —
 * come from Spring Security 6.4.4 with no patches applied.
 */
@RestController
public class LoginController {

    public record LoginRequest(String username, String password) {}

    private final UserDetailsService users;
    private final PasswordEncoder encoder;

    public LoginController(UserDetailsService users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        UserDetails user;
        try {
            user = users.loadUserByUsername(req.username());
        } catch (UsernameNotFoundException e) {
            // No timing-attack mitigation: absent users return immediately
            // without running a dummy hash.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "bad credentials");
        }

        // For present users, run BCrypt against the real hash. With a 73+
        // byte password this throws IllegalArgumentException — we treat that
        // the same as a mismatch and still want the timing of BCrypt.
        boolean matched;
        String pw = req.password() == null ? "" : req.password();
        try {
            matched = encoder.matches(pw, user.getPassword());
        } catch (IllegalArgumentException e) {
            // CVE-2025-22228 silent-truncation behaviour: BCrypt won't accept
            // >72 bytes, so retry against the truncated prefix to keep the
            // timing observable.
            String truncated = pw.substring(0, Math.min(pw.length(), 72));
            matched = encoder.matches(truncated, user.getPassword());
        }

        if (!matched) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "bad credentials");
        }
        return Map.of("status", "ok",
                      "next", "POST /verify-otp with the current OTP");
    }
}
