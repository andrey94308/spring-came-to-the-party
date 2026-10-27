# Spring knowledge sharing — approved outline

Audience: Wien IT colleagues (developers and other technical staff). Language: German B1–B2, established technical terms in English. Format: first-person research walkthrough with four terminal demonstrations. Target: 25–40 minutes depending on demo pace and discussion.

Technical source of truth: the current repository state as inspected on 27 October 2026. The presentation is a content and visual aid, not a source of current implementation details.

No supplied images are strict input assets. All slide text, diagrams, arrows, and labels are native editable PowerPoint objects. Live terminal demonstrations are presenter actions, not separate slides. Presenter notes contain the German talk track and precise switch/return cues.

## Slide 1: Wenn Schwachstellen zusammenwirken
- Role: cover; 0:30.
- Research walkthrough of vulnerabilities in a Spring demo system.
- Title should not claim four CVEs or a strictly linear exploit chain.
- Visual: restrained title treatment, no academic identifiers.

## Slide 2: Warum Updates wichtig sind
- Role: motivation; 1:30.
- Libraries, frameworks, and runtimes can become vulnerable without a change to application code.
- Teams need to know deployed versions and whether affected functionality is reachable.
- Updating and checking configuration are part of normal engineering work.
- Visual: compact component stack or dependency-to-service relationship.

## Slide 3: Ein aktuelles Beispiel: PHP
- Role: current context; 1:00.
- PHP published fixes on 21 October 2026 for CVE-2026-109618 and CVE-2026-109619.
- One sentence only on their potential impact; avoid claims of widespread successful exploitation.
- Bridge: the demo does not use PHP; it asks the same practical questions about reachability and impact.
- Source for speaker notes: https://www.php.net/archive/2026.php#2026-10-21-5

## Slide 4: Warum heute Spring?
- Role: audience relevance; 0:30.
- Many projects at Wien IT use Java and Spring, according to the presenter.
- The talk will examine a controlled Spring example with vulnerable components and application-level weaknesses.
- Visual: short transition, no claims about Wien IT systems being vulnerable.

## Slide 5: Agenda
- Role: agenda; 0:30.
- Motivation and short scenario.
- Project structure and normal flows.
- Four experiments with Live Demos.
- Results and engineering takeaways.

## Slide 6: Die Geschichte in einer Minute
- Role: scenario; 1:00.
- Fictional MATO archive setting and a leaked `.mcp.json` that reveals the Gateway URL.
- Explain flags as markers that a stage was reached, without dwelling on game mechanics.
- Goal: discover what an outsider can learn or access in the demo system.
- Visual: one simple entry-point illustration.

## Slide 7: Die Idee des Demo-Systems
- Role: concept; 1:00.
- Controlled template project for studying several weaknesses together.
- Archive holds users, private notes, and searchable documents; a separate service protects a final resource.
- The question is how one observation enables the next experiment.

## Slide 8: Drei Services und eine Datenbank
- Role: architecture; 2:00.
- `edge-gateway`: public entry for archive endpoints and management endpoint.
- `archive-service`: internal, stores users, notes, and documents in MariaDB.
- `command-control`: separate host-published service for login, OTP, JWT, and protected resource.
- Visual: service map with public/internal boundaries and route labels.

## Slide 9: Der normale Ablauf
- Role: baseline; 2:00.
- `/users` normally returns a limited public roster.
- `/notes` requires Basic Auth and returns the authenticated user's notes.
- `/documents/search` requires Basic Auth and filters by supplied `department` value.
- `command-control` exposes login/OTP and checks a JWT at the final resource. Do not imply that current code binds OTP verification to a successful login.
- Visual: four request/response pairs with minimal text.

## Slide 10: Meine Forschungsfrage
- Role: method; 1:00.
- For each experiment: starting access, changed input, observed result, next possible action.
- Use real dependency versions and current code behavior, distinguishing component CVEs from application weaknesses.
- Visual: reusable four-step research loop.

## Slide 11: Experiment 1 — Gateway und SpEL
- Role: case study; 2:00 talk + 2:00 terminal.
- Public Gateway Actuator accepts route configuration; Spring evaluates a SpEL value during route construction.
- The payload changes `mato.gateway.audit.debug-trace` through `@systemProperties`.
- Terminal cue: show the route registration/refresh and the changed `/users` response using the existing first-stage solver or a presenter-friendly staged runner.
- Source advisory: https://spring.io/security/cve-2025-41243/

## Slide 12: Warum erscheinen Password Hashes?
- Role: observed result and data flow; 1:30.
- Gateway reads changed setting and adds `X-Debug-Trace: true` to proxied requests.
- Archive Service trusts the header and includes `passwordHash` in `/users` output.
- Show normal vs debug response and the broken trust boundary.

## Slide 13: Experiment 2 — Weak Password
- Role: case study; 1:30 talk + 2:00 terminal.
- One leaked BCrypt hash corresponds to a weak password found in a short wordlist.
- Basic Auth then opens that user's `/notes`, which points toward document search.
- Terminal cue: show offline wordlist match and authenticated notes response without rerunning earlier stages on screen.
- State explicitly that this stage is not a CVE.

## Slide 14: Experiment 3 — SQL Injection
- Role: case study; 2:30 talk + 2:00 terminal.
- `department` enters Spring AI's MariaDB filter converter and becomes part of SQL.
- The vulnerable converter in version 1.0.3 permits a changed filter value to return extra rows.
- Terminal cue: compare normal and changed `/documents/search` results; point to the additional document and the URL it reveals.
- Do not claim the application enforces department-based authorization beyond its actual filter.
- Source advisory: https://spring.io/security/cve-2026-22730/

## Slide 15: Experiment 4 — Login Timing und OTP
- Role: case study; 3:00 talk + up to 4:00 terminal.
- Custom `LoginController` bypasses the configured `AuthenticationManager`; absent users return earlier than existing users whose password is checked with BCrypt.
- `/verify-otp` has a four-digit code and no rate limit; a valid code yields JWT for the final resource.
- Terminal cue: show timing samples, OTP attempt progress, JWT acquisition, and final response.
- Caveat: the current OTP endpoint does not require proof of successful `/login`; timing and password are not technically mandatory for OTP verification.
- Related Spring Security advisory: https://spring.io/security/cve-2025-22234/

## Slide 16: Die gesamte Attack Chain
- Role: synthesis; 1:30.
- One map of what each stage reveals and what the presenter tested next.
- Distinguish genuinely dependent steps from the independently exploitable OTP weakness.
- Show successful end-to-end test outcome without exposing real secrets or claiming production impact.

## Slide 17: Was ich daraus mitnehme
- Role: engineering takeaways; 2:00.
- Inventory and patch dependencies and runtimes; restrict management endpoints.
- Validate trust in forwarded headers and review the whole input-to-SQL path.
- Use standard authentication flows and rate-limit OTP attempts.
- No assertions about the security state of Wien IT systems.

## Slide 18: Fragen
- Role: closing/Q&A; 0:30 plus available discussion time.
- Invite questions about the system, experiments, and preventive checks.

## Terminal demonstration plan

1. After slide 11: Gateway SpEL route registration and `/users` before/after.
2. After slide 13: offline hash match and authenticated `/notes`.
3. After slide 14: normal versus changed document search.
4. After slide 15: login timing measurements, OTP attempts, JWT, final response.

The bundled standalone scripts rerun earlier stages when required inputs are absent. Prepare a sequential presenter runner or terminal runbook that retains intermediate results, so live segments remain short and non-repetitive. This runbook is a presenter aid, not slide content.
