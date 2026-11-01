package org.orgarchive.archive.seed;

import org.orgarchive.archive.document.Document;
import org.orgarchive.archive.document.DocumentRepository;
import org.orgarchive.archive.user.User;
import org.orgarchive.archive.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Seeder implements CommandLineRunner {
    private final UserRepository users;
    private final DocumentRepository documents;
    private final PasswordEncoder encoder;
    private final String weakPassword;
    private final String finalFlag;

    public Seeder(UserRepository users, DocumentRepository documents,
                  PasswordEncoder encoder, @Value("${seed.weak-password}") String weakPassword,
                  @Value("${seed.final-flag}") String finalFlag) {
        this.users = users;
        this.documents = documents;
        this.encoder = encoder;
        this.weakPassword = weakPassword;
        this.finalFlag = finalFlag;
    }

    @Override
    @Transactional
    public void run(String... args) {
        long existing = users.count() + documents.count();
        if (existing > 0) {
            if (users.existsById("anna") && users.existsById("admin")
                    && documents.count() == 49) return;
            throw new IllegalStateException("Partial or old seed detected; run make demo-reset");
        }
        users.save(new User("m.lehtinen", "Maija Lehtinen", "m.lehtinen@orgarchive.example",
                encoder.encode("d5jW#9pqL!2zX"), "user", "786298a5-8c02-56ce-ba8f-d2b41ec3333c"));
        users.save(new User("anna", "Anna Brenner", "anna@orgarchive.example",
                encoder.encode(weakPassword), "user", "786298a5-8c02-56ce-ba8f-d2b41ec3333c"));
        users.save(new User("a.keller", "Anika Keller", "a.keller@orgarchive.example",
                encoder.encode("V8!aP4#rQ2sLm"), "user", "786298a5-8c02-56ce-ba8f-d2b41ec3333c"));
        users.save(new User("s.cho", "Sora Cho", "s.cho@orgarchive.example",
                encoder.encode("Q6^tN9@xC3wBv"), "user", "786298a5-8c02-56ce-ba8f-d2b41ec3333c"));
        users.save(new User("j.ortega", "Jules Ortega", "j.ortega@orgarchive.example",
                encoder.encode("M2%fK7!zT5nYp"), "user", "786298a5-8c02-56ce-ba8f-d2b41ec3333c"));
        users.save(new User("r.dubois", "Renaud Dubois", "r.dubois@orgarchive.example",
                encoder.encode("Tr0ub4dor&3xZ!"), "user", "1ae53893-c399-57c3-ba32-16e55b435249"));
        users.save(new User("l.meyer", "Lena Meyer", "l.meyer@orgarchive.example",
                encoder.encode("F7!qD2#vH9mRx"), "user", "1ae53893-c399-57c3-ba32-16e55b435249"));
        users.save(new User("p.ahmed", "Parvez Ahmed", "p.ahmed@orgarchive.example",
                encoder.encode("N4@kS8^aL1tWz"), "user", "1ae53893-c399-57c3-ba32-16e55b435249"));
        users.save(new User("e.rossi", "Elena Rossi", "e.rossi@orgarchive.example",
                encoder.encode("C9%xG3!pJ6bUa"), "user", "1ae53893-c399-57c3-ba32-16e55b435249"));
        users.save(new User("admin", "Platform Administrator", "admin@orgarchive.example",
                encoder.encode("Adm!nQ8vL2n$R7p"), "admin", "aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b"));
        users.save(new User("c.wright", "Clara Wright", "c.wright@orgarchive.example",
                encoder.encode("R5@hV2#sK8dPm"), "user", "aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b"));
        users.save(new User("t.sato", "Taro Sato", "t.sato@orgarchive.example",
                encoder.encode("B4!nY7^fD1qZx"), "user", "aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b"));
        users.save(new User("i.novak", "Irena Novak", "i.novak@orgarchive.example",
                encoder.encode("J8%wC3@rM6aTs"), "user", "aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b"));
        users.save(new User("o.martin", "Owen Martin", "o.martin@orgarchive.example",
                encoder.encode("P2#zL9!cV4hQy"), "user", "aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b"));
        users.save(new User("n.okafor", "Nia Okafor", "n.okafor@orgarchive.example",
                encoder.encode("X7@tF3^mB9kRz"), "user", "768fc845-aff9-5b32-b626-a8f05a8778d2"));
        users.save(new User("d.berg", "Daniel Berg", "d.berg@orgarchive.example",
                encoder.encode("A6!vW2#qH8nLp"), "user", "768fc845-aff9-5b32-b626-a8f05a8778d2"));
        users.save(new User("k.silva", "Kira Silva", "k.silva@orgarchive.example",
                encoder.encode("E4%yJ9@pT1dSc"), "user", "768fc845-aff9-5b32-b626-a8f05a8778d2"));
        users.save(new User("v.hale", "Victor Hale", "v.hale@orgarchive.example",
                encoder.encode("U3#bN7!xG5rMw"), "user", "768fc845-aff9-5b32-b626-a8f05a8778d2"));
        documents.save(new Document("Atlas Consulting — Quarterly Research Review", "Archive record 1 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"report\",\"revision\":1}"));
        documents.save(new Document("Client Contact Roster — Atlas Consulting", "Archive record 2 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"reference\",\"revision\":2}"));
        documents.save(new Document("Northern Region Brief", "Archive record 3 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"projects\",\"revision\":3}"));
        documents.save(new Document("Regional Signals Summary", "Archive record 4 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"report\",\"revision\":4}"));
        documents.save(new Document("Desk Handover Memo", "Archive record 5 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"reference\",\"revision\":5}"));
        documents.save(new Document("Open Source Digest", "Archive record 6 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"projects\",\"revision\":6}"));
        documents.save(new Document("Consultant Review Checklist", "Archive record 7 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"report\",\"revision\":7}"));
        documents.save(new Document("Translation Index", "Archive record 8 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"reference\",\"revision\":8}"));
        documents.save(new Document("Maritime Pattern Note", "Archive record 9 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"projects\",\"revision\":9}"));
        documents.save(new Document("Quarterly Risk Matrix", "Archive record 10 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"report\",\"revision\":10}"));
        documents.save(new Document("Regional Activity Summary", "Archive record 11 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"reference\",\"revision\":11}"));
        documents.save(new Document("Collection Request Log", "Archive record 12 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"projects\",\"revision\":12}"));
        documents.save(new Document("Market Trends Appendix", "Archive record 13 for atlas-consulting. Approved internal reference material.", "{\"organizationUuid\":\"786298a5-8c02-56ce-ba8f-d2b41ec3333c\",\"category\":\"report\",\"revision\":13}"));
        documents.save(new Document("Harbor Delivery — Rotation Schedule", "Archive record 1 for harbor-delivery. Approved internal reference material.", "{\"organizationUuid\":\"1ae53893-c399-57c3-ba32-16e55b435249\",\"category\":\"report\",\"revision\":1}"));
        documents.save(new Document("Harbor Delivery — Fuel Reserves", "Archive record 2 for harbor-delivery. Approved internal reference material.", "{\"organizationUuid\":\"1ae53893-c399-57c3-ba32-16e55b435249\",\"category\":\"reference\",\"revision\":2}"));
        documents.save(new Document("Depot Inventory Sheet", "Archive record 3 for harbor-delivery. Approved internal reference material.", "{\"organizationUuid\":\"1ae53893-c399-57c3-ba32-16e55b435249\",\"category\":\"projects\",\"revision\":3}"));
        documents.save(new Document("Supply Route Overview", "Archive record 4 for harbor-delivery. Approved internal reference material.", "{\"organizationUuid\":\"1ae53893-c399-57c3-ba32-16e55b435249\",\"category\":\"report\",\"revision\":4}"));
        documents.save(new Document("Fleet Maintenance Plan", "Archive record 5 for harbor-delivery. Approved internal reference material.", "{\"organizationUuid\":\"1ae53893-c399-57c3-ba32-16e55b435249\",\"category\":\"reference\",\"revision\":5}"));
        documents.save(new Document("Warehouse Audit Notes", "Archive record 6 for harbor-delivery. Approved internal reference material.", "{\"organizationUuid\":\"1ae53893-c399-57c3-ba32-16e55b435249\",\"category\":\"projects\",\"revision\":6}"));
        documents.save(new Document("Vendor Approval Register", "Archive record 7 for harbor-delivery. Approved internal reference material.", "{\"organizationUuid\":\"1ae53893-c399-57c3-ba32-16e55b435249\",\"category\":\"report\",\"revision\":7}"));
        documents.save(new Document("Spare Parts Forecast", "Archive record 8 for harbor-delivery. Approved internal reference material.", "{\"organizationUuid\":\"1ae53893-c399-57c3-ba32-16e55b435249\",\"category\":\"reference\",\"revision\":8}"));
        documents.save(new Document("Regional Delivery Calendar", "Archive record 9 for harbor-delivery. Approved internal reference material.", "{\"organizationUuid\":\"1ae53893-c399-57c3-ba32-16e55b435249\",\"category\":\"projects\",\"revision\":9}"));
        documents.save(new Document("Generator Service Log", "Archive record 10 for harbor-delivery. Approved internal reference material.", "{\"organizationUuid\":\"1ae53893-c399-57c3-ba32-16e55b435249\",\"category\":\"report\",\"revision\":10}"));
        documents.save(new Document("Purchasing Summary", "Archive record 11 for harbor-delivery. Approved internal reference material.", "{\"organizationUuid\":\"1ae53893-c399-57c3-ba32-16e55b435249\",\"category\":\"reference\",\"revision\":11}"));
        documents.save(new Document("Cedar Partners — Meeting Template", "Archive record 1 for cedar-partners. Approved internal reference material.", "{\"organizationUuid\":\"aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b\",\"category\":\"report\",\"revision\":1}"));
        documents.save(new Document("Policy Review Calendar", "Archive record 2 for cedar-partners. Approved internal reference material.", "{\"organizationUuid\":\"aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b\",\"category\":\"reference\",\"revision\":2}"));
        documents.save(new Document("Leadership Meeting Minutes", "Archive record 3 for cedar-partners. Approved internal reference material.", "{\"organizationUuid\":\"aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b\",\"category\":\"projects\",\"revision\":3}"));
        documents.save(new Document("Communications Protocol", "Archive record 4 for cedar-partners. Approved internal reference material.", "{\"organizationUuid\":\"aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b\",\"category\":\"report\",\"revision\":4}"));
        documents.save(new Document("Planning Cycle Overview", "Archive record 5 for cedar-partners. Approved internal reference material.", "{\"organizationUuid\":\"aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b\",\"category\":\"reference\",\"revision\":5}"));
        documents.save(new Document("Travel Approval Guide", "Archive record 6 for cedar-partners. Approved internal reference material.", "{\"organizationUuid\":\"aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b\",\"category\":\"projects\",\"revision\":6}"));
        documents.save(new Document("Budget Review Summary", "Archive record 7 for cedar-partners. Approved internal reference material.", "{\"organizationUuid\":\"aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b\",\"category\":\"report\",\"revision\":7}"));
        documents.save(new Document("Secure Room Schedule", "Archive record 8 for cedar-partners. Approved internal reference material.", "{\"organizationUuid\":\"aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b\",\"category\":\"reference\",\"revision\":8}"));
        documents.save(new Document("Records Retention Memo", "Archive record 9 for cedar-partners. Approved internal reference material.", "{\"organizationUuid\":\"aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b\",\"category\":\"projects\",\"revision\":9}"));
        documents.save(new Document("Training Agenda", "Archive record 10 for cedar-partners. Approved internal reference material.", "{\"organizationUuid\":\"aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b\",\"category\":\"report\",\"revision\":10}"));
        documents.save(new Document("Weekly Decision Log", "Archive record 11 for cedar-partners. Approved internal reference material.", "{\"organizationUuid\":\"aa2262cf-c84c-5f89-8853-dc9b2ebb4a7b\",\"category\":\"reference\",\"revision\":11}"));
        documents.save(new Document("Site Team Rota", "Archive record 1 for siteworks-group. Approved internal reference material.", "{\"organizationUuid\":\"768fc845-aff9-5b32-b626-a8f05a8778d2\",\"category\":\"report\",\"revision\":1}"));
        documents.save(new Document("Site Visit Overview", "Archive record 2 for siteworks-group. Approved internal reference material.", "{\"organizationUuid\":\"768fc845-aff9-5b32-b626-a8f05a8778d2\",\"category\":\"reference\",\"revision\":2}"));
        documents.save(new Document("Equipment Issue Register", "Archive record 3 for siteworks-group. Approved internal reference material.", "{\"organizationUuid\":\"768fc845-aff9-5b32-b626-a8f05a8778d2\",\"category\":\"projects\",\"revision\":3}"));
        documents.save(new Document("Radio Check Procedure", "Archive record 4 for siteworks-group. Approved internal reference material.", "{\"organizationUuid\":\"768fc845-aff9-5b32-b626-a8f05a8778d2\",\"category\":\"report\",\"revision\":4}"));
        documents.save(new Document("Weather Contingency Plan", "Archive record 5 for siteworks-group. Approved internal reference material.", "{\"organizationUuid\":\"768fc845-aff9-5b32-b626-a8f05a8778d2\",\"category\":\"reference\",\"revision\":5}"));
        documents.save(new Document("Observation Report Template", "Archive record 6 for siteworks-group. Approved internal reference material.", "{\"organizationUuid\":\"768fc845-aff9-5b32-b626-a8f05a8778d2\",\"category\":\"projects\",\"revision\":6}"));
        documents.save(new Document("Vehicle Inspection Form", "Archive record 7 for siteworks-group. Approved internal reference material.", "{\"organizationUuid\":\"768fc845-aff9-5b32-b626-a8f05a8778d2\",\"category\":\"report\",\"revision\":7}"));
        documents.save(new Document("Site Safety Bulletin", "Archive record 8 for siteworks-group. Approved internal reference material.", "{\"organizationUuid\":\"768fc845-aff9-5b32-b626-a8f05a8778d2\",\"category\":\"reference\",\"revision\":8}"));
        documents.save(new Document("Shift Handover Guide", "Archive record 9 for siteworks-group. Approved internal reference material.", "{\"organizationUuid\":\"768fc845-aff9-5b32-b626-a8f05a8778d2\",\"category\":\"projects\",\"revision\":9}"));
        documents.save(new Document("Regional Projects Map", "Archive record 10 for siteworks-group. Approved internal reference material.", "{\"organizationUuid\":\"768fc845-aff9-5b32-b626-a8f05a8778d2\",\"category\":\"report\",\"revision\":10}"));
        documents.save(new Document("Project Calendar", "Archive record 11 for siteworks-group. Approved internal reference material.", "{\"organizationUuid\":\"768fc845-aff9-5b32-b626-a8f05a8778d2\",\"category\":\"reference\",\"revision\":11}"));
        documents.save(new Document("Meridian Legal — Confidential Client File", finalFlag + "\nArchive isolation failure confirmed.", "{\"organizationUuid\":\"d3302858-83b5-5ec5-aba6-1f7c9da45bd0\",\"category\":\"report\",\"revision\":1}"));
        documents.save(new Document("Client Routing Note", "Archive record 2 for meridian-legal. Approved internal reference material.", "{\"organizationUuid\":\"d3302858-83b5-5ec5-aba6-1f7c9da45bd0\",\"category\":\"reference\",\"revision\":2}"));
        documents.save(new Document("Confidential Records Index", "Archive record 3 for meridian-legal. Approved internal reference material.", "{\"organizationUuid\":\"d3302858-83b5-5ec5-aba6-1f7c9da45bd0\",\"category\":\"projects\",\"revision\":3}"));
    }
}
