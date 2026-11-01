package org.orgarchive.archive.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The same administrative listing contract is used for users and organizations. */
@RestController
@RequestMapping("/organizations")
public class OrganizationController implements AdminQuery<String, OrganizationController.OrganizationDto> {
    private final UserRepository users;

    public OrganizationController(UserRepository users) { this.users = users; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrganizationDto(String id, Long accountCount) {}

    @Override
    public List<OrganizationDto> list(String filter, boolean debug) {
        String needle = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        Map<String, Long> counts = users.findAll().stream()
                .collect(Collectors.groupingBy(User::getOrganizationUuid, Collectors.counting()));
        return counts.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new OrganizationDto(entry.getKey(), debug ? entry.getValue() : null))
                .toList();
    }
}
