package org.orgarchive.archive.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Administrative organization queries using the shared role contract. */
@RestController
@RequestMapping("/organizations")
public class OrganizationController implements AdminRoleSecurityContract<String, String, OrganizationController.OrganizationDto> {
    private final UserRepository users;

    public OrganizationController(UserRepository users) {
        this.users = users;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrganizationDto(String id, Long accountCount) {}

    @Override
    public List<OrganizationDto> list(String filter, boolean debug) {
        String normalizedFilter = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        return counts().entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).contains(normalizedFilter))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new OrganizationDto(entry.getKey(), debug ? entry.getValue() : null))
                .toList();
    }

    @Override
    public OrganizationDto get(@PathVariable("id") String id) {
        Long count = counts().get(id);
        if (count == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new OrganizationDto(id, count);
    }

    private Map<String, Long> counts() {
        return users.findAll().stream()
                .collect(Collectors.groupingBy(User::getOrganizationUuid, Collectors.counting()));
    }
}
