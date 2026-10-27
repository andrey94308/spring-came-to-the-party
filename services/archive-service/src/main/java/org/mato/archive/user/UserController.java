package org.mato.archive.user;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns the user roster. The response shape is gated by the
 * {@code X-Debug-Trace} header that the edge-gateway forwards when its
 * audit-trace flag is enabled. In normal "public" mode the response strips
 * sensitive fields; in "debug-trace" mode it includes password hashes plus
 * Flag 1 — useful for an operator triaging a real incident, dangerous when an
 * attacker can flip the gateway flag remotely (CVE-2025-41243).
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository users;
    private final String flag1;

    public UserController(UserRepository users,
                          @Value("${flags.flag1}") String flag1) {
        this.users = users;
        this.flag1 = flag1;
    }

    public record UserPublicDto(String username, String displayName, String email,
                                String role, String department, boolean has2fa) {}

    public record UserDebugDto(String username, String displayName, String email,
                               String role, String department, boolean has2fa,
                               String passwordHash) {}

    public record UsersResponse(String mode, String flag, List<? extends Object> users) {}

    @GetMapping
    public UsersResponse list(@RequestHeader(value = "X-Debug-Trace", defaultValue = "false") String trace) {
        boolean debug = "true".equalsIgnoreCase(trace);
        if (debug) {
            List<UserDebugDto> dtos = users.findAll().stream()
                    .map(u -> new UserDebugDto(
                            u.getUsername(), u.getDisplayName(), u.getEmail(),
                            u.getRole(), u.getDepartment(), u.isHas2fa(),
                            u.getPasswordHash()))
                    .toList();
            return new UsersResponse("debug-trace", flag1, dtos);
        }
        List<UserPublicDto> dtos = users.findAll().stream()
                .map(u -> new UserPublicDto(
                        u.getUsername(), u.getDisplayName(), u.getEmail(),
                        u.getRole(), u.getDepartment(), u.isHas2fa()))
                .toList();
        return new UsersResponse("public", null, dtos);
    }
}
