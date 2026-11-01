package org.mato.archive.note;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.mato.archive.user.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteRepository notes;
    private final UserRepository users;

    public NoteController(NoteRepository notes, UserRepository users) {
        this.notes = notes;
        this.users = users;
    }

    public record NoteDto(Long id, String title, String body) {}

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<NoteDto> list(@AuthenticationPrincipal UserDetails principal) {
        return notes.findByDepartment(users.findById(principal.getUsername()).orElseThrow().getDepartment()).stream()
                .map(n -> new NoteDto(n.getId(), n.getTitle(), n.getBody()))
                .toList();
    }
}
