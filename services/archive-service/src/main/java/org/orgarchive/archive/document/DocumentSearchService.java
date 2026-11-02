package org.orgarchive.archive.document;

import java.util.List;
import org.orgarchive.archive.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class DocumentSearchService {

    private final DocumentRepository documents;
    private final UserRepository users;

    public DocumentSearchService(DocumentRepository documents, UserRepository users) {
        this.documents = documents;
        this.users = users;
    }

    public List<DocumentDto> search(String username, String category, Integer minRevision) {
        return documents.search(allowedOrganization(username), category, minRevision);
    }

    public List<DocumentDto> searchSafe(String username, String category, Integer minRevision) {
        return documents.searchSafe(allowedOrganization(username), category, minRevision);
    }

    public String filterSql(String category, Integer minRevision) {
        if (category == null && minRevision == null) {
            return "";
        }
        return documents.filterSql(category, minRevision);
    }

    private String allowedOrganization(String username) {
        return users.findById(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user is missing"))
                .getOrganizationUuid();
    }
}
