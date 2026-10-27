package org.mato.archive.seed;

import org.mato.archive.document.Document;
import org.mato.archive.document.DocumentRepository;
import org.mato.archive.note.Note;
import org.mato.archive.note.NoteRepository;
import org.mato.archive.user.User;
import org.mato.archive.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Seeder implements CommandLineRunner {

    private final UserRepository users;
    private final NoteRepository notes;
    private final DocumentRepository documents;
    private final PasswordEncoder encoder;

    private final String flag2;
    private final String flag3;
    private final String weakPassword;
    private final String commandUrl;
    private final String presidentPassword;

    public Seeder(UserRepository users,
                  NoteRepository notes,
                  DocumentRepository documents,
                  PasswordEncoder encoder,
                  @Value("${flags.flag2}") String flag2,
                  @Value("${flags.flag3}") String flag3,
                  @Value("${seed.weak-password}") String weakPassword,
                  @Value("${seed.command-url}") String commandUrl,
                  @Value("${seed.command-president-password}") String presidentPassword) {
        this.users = users;
        this.notes = notes;
        this.documents = documents;
        this.encoder = encoder;
        this.flag2 = flag2;
        this.flag3 = flag3;
        this.weakPassword = weakPassword;
        this.commandUrl = commandUrl;
        this.presidentPassword = presidentPassword;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;

        users.save(new User("m.lehtinen", "Maija Lehtinen", "m.lehtinen@mato.int",
                encoder.encode("d5jW#9pqL!2zX"),
                "user", "intel-analysis", false));
        users.save(new User("bob", "Bob Brennan", "bob@mato.int",
                encoder.encode(weakPassword),
                "user", "intel-analysis", false));
        users.save(new User("r.dubois", "Renaud Dubois", "r.dubois@mato.int",
                encoder.encode("Tr0ub4dor&3xZ!"),
                "user", "logistics-corps", false));
        users.save(new User("mr.president", "Mr. President", "president@mato.int",
                encoder.encode(presidentPassword),
                "admin", "command-staff", true));

        notes.save(new Note("m.lehtinen", "Standup",
                "Daily reminder: don't paste production secrets into commit messages."));

        notes.save(new Note("bob", "Morning thoughts",
                "I accidentally backed up some classified material into the doc archive. " +
                        "Ask the archivist — search by department.\n\n" +
                        flag2));
        notes.save(new Note("bob", "TODO",
                "Rotate password from 'sunshine' to something stronger before audit week."));

        notes.save(new Note("r.dubois", "Pickup list",
                "Order more printer paper. Lots of redactions this week."));

        notes.save(new Note("mr.president", "Private",
                "Note to self: rotate the launch-code OTP seed before next briefing."));

        documents.save(new Document(
                "MATO Intel — Q1 Threat Assessment",
                "Quarterly intelligence summary for the intel-analysis desk.",
                "{\"department\":\"intel-analysis\"}"));
        documents.save(new Document(
                "Source Contact Roster — Intel Analysis",
                "Approved contact list for the intel-analysis pool.",
                "{\"department\":\"intel-analysis\"}"));
        documents.save(new Document(
                "Logistics Corps — Rotation Schedule",
                "Standard logistics rotation schedule.",
                "{\"department\":\"logistics-corps\"}"));
        documents.save(new Document(
                "Logistics Corps — Fuel Reserves",
                "Fuel availability across regional depots.",
                "{\"department\":\"logistics-corps\"}"));
        documents.save(new Document(
                "Command Staff — Briefing Template",
                "Template for weekly command-staff briefings.",
                "{\"department\":\"command-staff\"}"));

        // The classified doc that flag 3 lives inside. It's tagged with a
        // department no authenticated user belongs to, so the only way to
        // surface it is via the SQL injection in the metadata filter.
        String specialOpsDoc = String.join("\n",
                "CLASSIFIED — MATO SPECIAL-OPS ONLY.",
                "Command-control is reachable at " + commandUrl + ".",
                "The president's password is base64: " +
                        java.util.Base64.getEncoder().encodeToString(
                                presidentPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + ".",
                "Access is restricted. Credentials are time-based.",
                "",
                flag3);
        documents.save(new Document(
                "OPERATION ZIRCON — Credentials Package",
                specialOpsDoc,
                "{\"department\":\"mato-special-ops\"}"));
    }
}
