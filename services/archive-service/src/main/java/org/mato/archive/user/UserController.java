package org.mato.archive.user;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Intentionally unsafe admin roster for the hash-recovery lab. */
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository users;

    public UserController(UserRepository users) { this.users = users; }

    public record UserDto(String username, String displayName, String email,
                          String role, String department, String passwordHash) {}

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> list() {
        return users.findAll().stream()
                .map(u -> new UserDto(u.getUsername(), u.getDisplayName(), u.getEmail(),
                        u.getRole(), u.getDepartment(), u.getPasswordHash()))
                .toList();
    }
}
