package org.orgarchive.archive.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
public class UserController implements AdminQuery<String, UserController.UserDto> {
    private final UserRepository users;

    public UserController(UserRepository users) { this.users = users; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserDto(String username, String displayName, String email,
                          String role, String organization, String passwordHash) {}

    @Override
    public List<UserDto> list(String filter, boolean debug) {
        String needle = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        return users.findAll().stream()
                .filter(user -> user.getUsername().toLowerCase(Locale.ROOT).contains(needle)
                        || user.getDisplayName().toLowerCase(Locale.ROOT).contains(needle))
                .map(user -> new UserDto(user.getUsername(), user.getDisplayName(), user.getEmail(),
                        user.getRole(), user.getOrganization(), debug ? user.getPasswordHash() : null))
                .toList();
    }

    @GetMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto get(@PathVariable String username) {
        User user = users.findById(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new UserDto(user.getUsername(), user.getDisplayName(), user.getEmail(),
                user.getRole(), user.getOrganization(), null);
    }
}
