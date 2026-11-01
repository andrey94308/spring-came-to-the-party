package org.orgarchive.archive.note;

import java.util.List;
import org.orgarchive.archive.user.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
public class NoteController {
    private final NoteRepository notes;
    private final UserRepository users;

    public NoteController(NoteRepository notes, UserRepository users) {
        this.notes = notes;
        this.users = users;
    }

    public record NoteDto(Long id, String title, String body) {}

    @GetMapping("/notes")
    @PreAuthorize("hasRole('USER')")
    public List<NoteDto> own(@AuthenticationPrincipal UserDetails principal) {
        return listFor(organization(principal));
    }

    @GetMapping("/organizations/{organization}/notes")
    @PreAuthorize("hasRole('USER')")
    public List<NoteDto> byOrganization(@PathVariable String organization,
                                        @AuthenticationPrincipal UserDetails principal) {
        if (!organization(principal).equals(organization)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this organization");
        }
        return listFor(organization);
    }

    private String organization(UserDetails principal) {
        return users.findById(principal.getUsername()).orElseThrow().getOrganization();
    }

    private List<NoteDto> listFor(String organization) {
        return notes.findByOrganization(organization).stream()
                .map(note -> new NoteDto(note.getId(), note.getTitle(), note.getBody()))
                .toList();
    }
}
