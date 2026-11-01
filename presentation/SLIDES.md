# OrgArchive presentation outline

1. OrgArchive: shared records for organizations.
2. A known gateway URL and its public API index.
3. Anonymous `/users` and `/notes` return `401`.
4. Sign up: a normal account in a new trial organization.
5. Notes isolation: own organization succeeds, another returns `403`.
6. Spring Security CVE-2025-22223: inherited admin rule missed on a generic controller interface.
7. Admin debug roster exposes BCrypt hashes; direct user lookup still denies the visitor. Recover Bob's lab password offline.
8. Bob and Maija share notes; another organization sees different notes.
9. Normal document search stays in Bob's organization.
10. Spring AI CVE-2026-22730: injected metadata filter crosses organization boundaries.
11. Parameterized safe query returns no foreign documents.
12. Lessons: enforce roles at a reliable boundary, avoid hash exposure, and bind user input in SQL.
