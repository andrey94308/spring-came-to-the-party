package org.orgarchive.archive.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "basicAuth")
@Tag(name = "Users", description = "Administration endpoints; intended for the ADMIN role.")
public class UserController implements AdminRoleSecurityContract<String, String, UserController.UserDto> {
    private final UserRepository users;

    public UserController(UserRepository users) { this.users = users; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserDto(String username, String displayName, String email,
                          String role, String passwordHash) {}

    @Override
    public List<UserDto> list(String filter, boolean debug) {
        String normalizedFilter = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        return users.findAll().stream()
                .filter(user -> user.getUsername().toLowerCase(Locale.ROOT).contains(normalizedFilter)
                        || user.getDisplayName().toLowerCase(Locale.ROOT).contains(normalizedFilter))
                .map(user -> new UserDto(user.getUsername(), user.getDisplayName(), user.getEmail(),
                        user.getRole(), debug ? user.getPasswordHash() : null))
                .toList();
    }

    @Override
    public UserDto get(@PathVariable("id") String username) {
        User user = users.findById(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new UserDto(user.getUsername(), user.getDisplayName(), user.getEmail(),
                user.getRole(), null);
    }
}
