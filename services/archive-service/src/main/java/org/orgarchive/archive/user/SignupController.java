package org.orgarchive.archive.user;

import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SignupController {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public SignupController(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    public record SignupRequest(String username, String displayName, String email, String password) {}
    public record SignupResponse(String username, String organizationUuid) {}

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SignupResponse signup(@RequestBody SignupRequest request) {
        if (request == null || request.username() == null
                || !request.username().matches("[a-zA-Z0-9._-]{3,64}")
                || request.displayName() == null || request.displayName().isBlank()
                || request.displayName().length() > 128
                || request.email() == null || request.email().length() > 128
                || !request.email().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
                || request.password() == null || request.password().length() < 12
                || request.password().length() > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration fields");
        }
        String username = request.username().toLowerCase(Locale.ROOT);
        if (users.existsById(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        String organizationUuid = UUID.randomUUID().toString();
        try {
            users.saveAndFlush(new User(username, request.displayName().trim(), request.email().trim(),
                    passwords.encode(request.password()), "user", organizationUuid));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists", e);
        }
        return new SignupResponse(username, organizationUuid);
    }
}
