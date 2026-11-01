package org.mato.archive.seed;

import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

@Component
public class Seeder implements CommandLineRunner {
    private final UserRepository users;
    private final NoteRepository notes;
    private final DocumentRepository documents;
    private final PasswordEncoder encoder;
    private final String weakPassword;
    private final String finalFlag;

    public Seeder(UserRepository users, NoteRepository notes, DocumentRepository documents,
                  PasswordEncoder encoder, @Value("${seed.weak-password}") String weakPassword,
                  @Value("${seed.final-flag}") String finalFlag) {
        this.users = users;
        this.notes = notes;
        this.documents = documents;
        this.encoder = encoder;
        this.weakPassword = weakPassword;
        this.finalFlag = finalFlag;
    }

    @Override
    @Transactional
    public void run(String... args) {
        long existing = users.count() + notes.count() + documents.count();
        if (existing > 0) {
            if (users.count() == 18 && notes.count() == 30 && documents.count() == 49) return;
            throw new IllegalStateException("Partial or old seed detected; run make demo-reset");
        }
        users.save(new User("m.lehtinen", "Maija Lehtinen", "m.lehtinen@mato.int",
                encoder.encode("d5jW#9pqL!2zX"), "user", "intel-analysis"));
        users.save(new User("bob", "Bob Brennan", "bob@mato.int",
                encoder.encode(weakPassword), "user", "intel-analysis"));
        users.save(new User("a.keller", "Anika Keller", "a.keller@mato.int",
                encoder.encode("V8!aP4#rQ2sLm"), "user", "intel-analysis"));
        users.save(new User("s.cho", "Sora Cho", "s.cho@mato.int",
                encoder.encode("Q6^tN9@xC3wBv"), "user", "intel-analysis"));
        users.save(new User("j.ortega", "Jules Ortega", "j.ortega@mato.int",
                encoder.encode("M2%fK7!zT5nYp"), "user", "intel-analysis"));
        users.save(new User("r.dubois", "Renaud Dubois", "r.dubois@mato.int",
                encoder.encode("Tr0ub4dor&3xZ!"), "user", "logistics-corps"));
        users.save(new User("l.meyer", "Lena Meyer", "l.meyer@mato.int",
                encoder.encode("F7!qD2#vH9mRx"), "user", "logistics-corps"));
        users.save(new User("p.ahmed", "Parvez Ahmed", "p.ahmed@mato.int",
                encoder.encode("N4@kS8^aL1tWz"), "user", "logistics-corps"));
        users.save(new User("e.rossi", "Elena Rossi", "e.rossi@mato.int",
                encoder.encode("C9%xG3!pJ6bUa"), "user", "logistics-corps"));
        users.save(new User("mr.president", "Mr. President", "mr.president@mato.int",
                encoder.encode("Pr3s!d3nt$Q8vL2n"), "admin", "command-staff"));
        users.save(new User("c.wright", "Clara Wright", "c.wright@mato.int",
                encoder.encode("R5@hV2#sK8dPm"), "user", "command-staff"));
        users.save(new User("t.sato", "Taro Sato", "t.sato@mato.int",
                encoder.encode("B4!nY7^fD1qZx"), "user", "command-staff"));
        users.save(new User("i.novak", "Irena Novak", "i.novak@mato.int",
                encoder.encode("J8%wC3@rM6aTs"), "user", "command-staff"));
        users.save(new User("o.martin", "Owen Martin", "o.martin@mato.int",
                encoder.encode("P2#zL9!cV4hQy"), "user", "command-staff"));
        users.save(new User("n.okafor", "Nia Okafor", "n.okafor@mato.int",
                encoder.encode("X7@tF3^mB9kRz"), "user", "field-operations"));
        users.save(new User("d.berg", "Daniel Berg", "d.berg@mato.int",
                encoder.encode("A6!vW2#qH8nLp"), "user", "field-operations"));
        users.save(new User("k.silva", "Kira Silva", "k.silva@mato.int",
                encoder.encode("E4%yJ9@pT1dSc"), "user", "field-operations"));
        users.save(new User("v.hale", "Victor Hale", "v.hale@mato.int",
                encoder.encode("U3#bN7!xG5rMw"), "user", "field-operations"));
        notes.save(new Note("intel-analysis", "Standup", "Daily reminder: keep source identifiers out of meeting notes."));
        notes.save(new Note("intel-analysis", "Morning thoughts", "The document archive is easier to navigate by department. Try document search after this handover."));
        notes.save(new Note("intel-analysis", "Collection priorities", "Review the northern corridor briefings before the weekly meeting."));
        notes.save(new Note("intel-analysis", "Source hygiene", "Use the approved contact roster for external calls."));
        notes.save(new Note("intel-analysis", "Desk handover", "The next shift owns the Q1 threat assessment updates."));
        notes.save(new Note("intel-analysis", "Translation queue", "Three foreign-language summaries await review."));
        notes.save(new Note("intel-analysis", "Briefing index", "Search the archive with the intel-analysis department filter."));
        notes.save(new Note("intel-analysis", "Analyst rota", "Pair new analysts with a reviewer for initial reports."));
        notes.save(new Note("intel-analysis", "Archive reminder", "Metadata is part of the document index; check it when filing."));
        notes.save(new Note("logistics-corps", "Pickup list", "Order more printer paper for the regional depots."));
        notes.save(new Note("logistics-corps", "Fuel review", "Depot forecasts are due on Friday."));
        notes.save(new Note("logistics-corps", "Fleet rota", "Confirm vehicle assignments before dispatch."));
        notes.save(new Note("logistics-corps", "Warehouse count", "Reconcile the south warehouse inventory."));
        notes.save(new Note("logistics-corps", "Vendor calls", "Use approved purchasing channels for spare parts."));
        notes.save(new Note("logistics-corps", "Shift notes", "Two delivery windows changed this morning."));
        notes.save(new Note("logistics-corps", "Maintenance", "Inspect generators at the field hub."));
        notes.save(new Note("command-staff", "Briefing template", "Use the latest command-staff briefing template."));
        notes.save(new Note("command-staff", "Meeting room", "Reserve the secure conference room for Tuesday."));
        notes.save(new Note("command-staff", "Review queue", "Three policy memos await signature."));
        notes.save(new Note("command-staff", "Travel desk", "Confirm travel requests with operations."));
        notes.save(new Note("command-staff", "Protocol", "Archive the final meeting agenda."));
        notes.save(new Note("command-staff", "Calendar", "Coordinate next month’s planning session."));
        notes.save(new Note("command-staff", "Minutes", "Distribute the approved minutes to staff."));
        notes.save(new Note("field-operations", "Patrol plan", "Review the field rota before departure."));
        notes.save(new Note("field-operations", "Radio check", "Confirm batteries and spare headsets."));
        notes.save(new Note("field-operations", "Weather watch", "Update route plans for changing conditions."));
        notes.save(new Note("field-operations", "Supply pickup", "Collect the approved equipment kit."));
        notes.save(new Note("field-operations", "Field report", "Submit daily observations to the archive."));
        notes.save(new Note("field-operations", "Team handover", "Brief the incoming shift at 18:00."));
        notes.save(new Note("field-operations", "Vehicle log", "Record mileage after every trip."));
        documents.save(new Document("MATO Intel — Q1 Threat Assessment", "Archive record 1 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"briefing\",\"revision\":1}"));
        documents.save(new Document("Source Contact Roster — Intel Analysis", "Archive record 2 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"reference\",\"revision\":2}"));
        documents.save(new Document("Northern Corridor Brief", "Archive record 3 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"operations\",\"revision\":3}"));
        documents.save(new Document("Regional Signals Summary", "Archive record 4 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"briefing\",\"revision\":4}"));
        documents.save(new Document("Desk Handover Memo", "Archive record 5 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"reference\",\"revision\":5}"));
        documents.save(new Document("Open Source Digest", "Archive record 6 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"operations\",\"revision\":6}"));
        documents.save(new Document("Analyst Review Checklist", "Archive record 7 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"briefing\",\"revision\":7}"));
        documents.save(new Document("Translation Index", "Archive record 8 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"reference\",\"revision\":8}"));
        documents.save(new Document("Maritime Pattern Note", "Archive record 9 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"operations\",\"revision\":9}"));
        documents.save(new Document("Quarterly Risk Matrix", "Archive record 10 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"briefing\",\"revision\":10}"));
        documents.save(new Document("Border Activity Summary", "Archive record 11 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"reference\",\"revision\":11}"));
        documents.save(new Document("Collection Request Log", "Archive record 12 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"operations\",\"revision\":12}"));
        documents.save(new Document("Threat Trends Appendix", "Archive record 13 for intel-analysis. Approved internal reference material.", "{\"department\":\"intel-analysis\",\"category\":\"briefing\",\"revision\":13}"));
        documents.save(new Document("Logistics Corps — Rotation Schedule", "Archive record 1 for logistics-corps. Approved internal reference material.", "{\"department\":\"logistics-corps\",\"category\":\"briefing\",\"revision\":1}"));
        documents.save(new Document("Logistics Corps — Fuel Reserves", "Archive record 2 for logistics-corps. Approved internal reference material.", "{\"department\":\"logistics-corps\",\"category\":\"reference\",\"revision\":2}"));
        documents.save(new Document("Depot Inventory Sheet", "Archive record 3 for logistics-corps. Approved internal reference material.", "{\"department\":\"logistics-corps\",\"category\":\"operations\",\"revision\":3}"));
        documents.save(new Document("Supply Route Overview", "Archive record 4 for logistics-corps. Approved internal reference material.", "{\"department\":\"logistics-corps\",\"category\":\"briefing\",\"revision\":4}"));
        documents.save(new Document("Fleet Maintenance Plan", "Archive record 5 for logistics-corps. Approved internal reference material.", "{\"department\":\"logistics-corps\",\"category\":\"reference\",\"revision\":5}"));
        documents.save(new Document("Warehouse Audit Notes", "Archive record 6 for logistics-corps. Approved internal reference material.", "{\"department\":\"logistics-corps\",\"category\":\"operations\",\"revision\":6}"));
        documents.save(new Document("Vendor Approval Register", "Archive record 7 for logistics-corps. Approved internal reference material.", "{\"department\":\"logistics-corps\",\"category\":\"briefing\",\"revision\":7}"));
        documents.save(new Document("Spare Parts Forecast", "Archive record 8 for logistics-corps. Approved internal reference material.", "{\"department\":\"logistics-corps\",\"category\":\"reference\",\"revision\":8}"));
        documents.save(new Document("Regional Delivery Calendar", "Archive record 9 for logistics-corps. Approved internal reference material.", "{\"department\":\"logistics-corps\",\"category\":\"operations\",\"revision\":9}"));
        documents.save(new Document("Generator Service Log", "Archive record 10 for logistics-corps. Approved internal reference material.", "{\"department\":\"logistics-corps\",\"category\":\"briefing\",\"revision\":10}"));
        documents.save(new Document("Purchasing Summary", "Archive record 11 for logistics-corps. Approved internal reference material.", "{\"department\":\"logistics-corps\",\"category\":\"reference\",\"revision\":11}"));
        documents.save(new Document("Command Staff — Briefing Template", "Archive record 1 for command-staff. Approved internal reference material.", "{\"department\":\"command-staff\",\"category\":\"briefing\",\"revision\":1}"));
        documents.save(new Document("Policy Review Calendar", "Archive record 2 for command-staff. Approved internal reference material.", "{\"department\":\"command-staff\",\"category\":\"reference\",\"revision\":2}"));
        documents.save(new Document("Leadership Meeting Minutes", "Archive record 3 for command-staff. Approved internal reference material.", "{\"department\":\"command-staff\",\"category\":\"operations\",\"revision\":3}"));
        documents.save(new Document("Communications Protocol", "Archive record 4 for command-staff. Approved internal reference material.", "{\"department\":\"command-staff\",\"category\":\"briefing\",\"revision\":4}"));
        documents.save(new Document("Planning Cycle Overview", "Archive record 5 for command-staff. Approved internal reference material.", "{\"department\":\"command-staff\",\"category\":\"reference\",\"revision\":5}"));
        documents.save(new Document("Travel Approval Guide", "Archive record 6 for command-staff. Approved internal reference material.", "{\"department\":\"command-staff\",\"category\":\"operations\",\"revision\":6}"));
        documents.save(new Document("Budget Review Summary", "Archive record 7 for command-staff. Approved internal reference material.", "{\"department\":\"command-staff\",\"category\":\"briefing\",\"revision\":7}"));
        documents.save(new Document("Secure Room Schedule", "Archive record 8 for command-staff. Approved internal reference material.", "{\"department\":\"command-staff\",\"category\":\"reference\",\"revision\":8}"));
        documents.save(new Document("Records Retention Memo", "Archive record 9 for command-staff. Approved internal reference material.", "{\"department\":\"command-staff\",\"category\":\"operations\",\"revision\":9}"));
        documents.save(new Document("Training Agenda", "Archive record 10 for command-staff. Approved internal reference material.", "{\"department\":\"command-staff\",\"category\":\"briefing\",\"revision\":10}"));
        documents.save(new Document("Weekly Decision Log", "Archive record 11 for command-staff. Approved internal reference material.", "{\"department\":\"command-staff\",\"category\":\"reference\",\"revision\":11}"));
        documents.save(new Document("Field Team Rota", "Archive record 1 for field-operations. Approved internal reference material.", "{\"department\":\"field-operations\",\"category\":\"briefing\",\"revision\":1}"));
        documents.save(new Document("Patrol Route Overview", "Archive record 2 for field-operations. Approved internal reference material.", "{\"department\":\"field-operations\",\"category\":\"reference\",\"revision\":2}"));
        documents.save(new Document("Equipment Issue Register", "Archive record 3 for field-operations. Approved internal reference material.", "{\"department\":\"field-operations\",\"category\":\"operations\",\"revision\":3}"));
        documents.save(new Document("Radio Check Procedure", "Archive record 4 for field-operations. Approved internal reference material.", "{\"department\":\"field-operations\",\"category\":\"briefing\",\"revision\":4}"));
        documents.save(new Document("Weather Contingency Plan", "Archive record 5 for field-operations. Approved internal reference material.", "{\"department\":\"field-operations\",\"category\":\"reference\",\"revision\":5}"));
        documents.save(new Document("Observation Report Template", "Archive record 6 for field-operations. Approved internal reference material.", "{\"department\":\"field-operations\",\"category\":\"operations\",\"revision\":6}"));
        documents.save(new Document("Vehicle Inspection Form", "Archive record 7 for field-operations. Approved internal reference material.", "{\"department\":\"field-operations\",\"category\":\"briefing\",\"revision\":7}"));
        documents.save(new Document("Field Safety Bulletin", "Archive record 8 for field-operations. Approved internal reference material.", "{\"department\":\"field-operations\",\"category\":\"reference\",\"revision\":8}"));
        documents.save(new Document("Shift Handover Guide", "Archive record 9 for field-operations. Approved internal reference material.", "{\"department\":\"field-operations\",\"category\":\"operations\",\"revision\":9}"));
        documents.save(new Document("Regional Operations Map", "Archive record 10 for field-operations. Approved internal reference material.", "{\"department\":\"field-operations\",\"category\":\"briefing\",\"revision\":10}"));
        documents.save(new Document("Deployment Calendar", "Archive record 11 for field-operations. Approved internal reference material.", "{\"department\":\"field-operations\",\"category\":\"reference\",\"revision\":11}"));
        documents.save(new Document("Operation Zircon — Final Archive", finalFlag + "\nArchive boundary breach confirmed.", "{\"department\":\"mato-special-ops\",\"category\":\"briefing\",\"revision\":1}"));
        documents.save(new Document("Special Operations Routing Note", "Archive record 2 for mato-special-ops. Approved internal reference material.", "{\"department\":\"mato-special-ops\",\"category\":\"reference\",\"revision\":2}"));
        documents.save(new Document("Special Operations Records Index", "Archive record 3 for mato-special-ops. Approved internal reference material.", "{\"department\":\"mato-special-ops\",\"category\":\"operations\",\"revision\":3}"));
    }
}
