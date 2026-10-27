package org.mato.archive.note;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteRepository notes;

    public NoteController(NoteRepository notes) {
        this.notes = notes;
    }

    public record NoteDto(Long id, String title, String body) {}

    @GetMapping
    public List<NoteDto> list(@AuthenticationPrincipal UserDetails principal) {
        return notes.findByUsername(principal.getUsername()).stream()
                .map(n -> new NoteDto(n.getId(), n.getTitle(), n.getBody()))
                .toList();
    }
}
