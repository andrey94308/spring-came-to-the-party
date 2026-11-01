package org.orgarchive.edge;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiIndexController {
    public record Endpoint(String method, String path, String access) {}

    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of("service", "OrgArchive", "endpoints", List.of(
                new Endpoint("GET", "/", "public"),
                new Endpoint("POST", "/signup", "public"),
                new Endpoint("GET", "/users", "authenticated; admin role"),
                new Endpoint("GET", "/users/{username}", "authenticated; admin role"),
                new Endpoint("POST", "/documents/search", "authenticated user"),
                new Endpoint("POST", "/documents/search-safe", "authenticated user"),
                new Endpoint("POST", "/documents/search-debug", "authenticated user")));
    }
}
