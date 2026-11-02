package org.orgarchive.archive.document;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documents")
@SecurityRequirement(name = "basicAuth")
@Tag(name = "Documents", description = "Document search endpoints; require the USER role.")
public class DocumentController {

    private final DocumentSearchService searchService;

    public DocumentController(DocumentSearchService searchService) {
        this.searchService = searchService;
    }

    public record SearchRequest(String category, Integer minRevision) {}

    @PostMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public List<DocumentDto> search(@RequestBody SearchRequest req,
                                    @AuthenticationPrincipal UserDetails principal) {
        return searchService.search(principal.getUsername(), req.category(), req.minRevision());
    }

    @PostMapping("/search-safe")
    @PreAuthorize("hasRole('USER')")
    public List<DocumentDto> searchSafe(@RequestBody SearchRequest req,
                                        @AuthenticationPrincipal UserDetails principal) {
        return searchService.searchSafe(principal.getUsername(), req.category(), req.minRevision());
    }

    @PostMapping("/search-debug")
    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> searchDebug(@RequestBody SearchRequest req) {
        return Map.of("whereFragment", searchService.filterSql(req.category(), req.minRevision()));
    }
}
