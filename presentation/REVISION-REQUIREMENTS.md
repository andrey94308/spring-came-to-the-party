# OrgArchive presentation revision — working notes

Status: collecting requirements. Do not edit the existing slides, speaker notes, or PDF yet. This file is temporary and can be deleted after the revision.

## Main direction

- Use only a few slides. The presenter may export them as a PDF and open that PDF inside the IDE, switching between it and the source code.
- Keep the introduction very short: at most one slide with the general context and the purpose of the demo.
- A recent PHP CVE could be mentioned as a brief opening example, but the exact incident and wording are undecided. Verify it before adding any factual claim.
- Remove the current slide 2, which discusses the vulnerability chain before the audience understands the project.
- Start with a clear explanation of OrgArchive: what the service does, how the components connect, what data it stores, how the endpoints work, and where authentication and authorization happen.
- Keep the useful structure of the current slide 3 as a starting point. Show the actual code in the IDE alongside the slides; the slides should support that walkthrough rather than duplicate it.
- Explain the vulnerabilities only after the project and its intended protection are clear. Then move to the live Postman chain.
- Keep the presentation material in German. Keep text and diagrams editable until the presenter chooses whether to export a PDF.

## Provisional presentation order

1. **Very short opening (optional).** The problem: a plausible system can fail when library vulnerabilities and application decisions combine. A PHP CVE reference is optional and still needs identification.
2. **OrgArchive structure and data.** Client/Postman → public edge gateway → internal archive service → MariaDB. Show `app_user` and `document`; explain `organizationUuid` as the logical tenant identifier.
3. **Endpoints and intended access.** Public signup; HTTP Basic on protected routes; intended admin-only `/users`; regular-user document search. Explain which checks happen in the HTTP filter and which happen on methods.
4. **IDE code walkthrough.** Switch from the PDF to the relevant files. Establish normal behavior and the intended tenant boundary before explaining either CVE.
5. **Vulnerabilities and demo.** Explain CVE-2025-22223, the `debug=true` hash exposure, and CVE-2026-22730 in that order. Run the Postman collection. Decide later whether these explanations need slides or can be handled mostly in code.
6. **Brief conclusion (optional slide).** Evaluate reachable CVEs and legacy application decisions together; prioritize updates according to the actual path through the system.

This is an order of topics, not a commitment to six slides. The final deck should be much shorter than the current nine-slide version.

## Current code map for the walkthrough

| Topic | File to open | Point to explain |
| --- | --- | --- |
| Public entry point | `services/edge-gateway/src/main/java/org/orgarchive/edge/ApiIndexController.java` | `GET /` lists routes for the demo; its `access` text is descriptive, not enforcement. |
| Routing | `services/edge-gateway/src/main/resources/application.yml` | The gateway forwards signup, users, and document routes to the archive service. |
| Signup | `services/archive-service/src/main/java/org/orgarchive/archive/user/SignupController.java` | Public registration creates a regular user, hashes the password, generates an internal organization UUID, and returns only the username. |
| HTTP authentication | `services/archive-service/src/main/java/org/orgarchive/archive/security/SecurityConfig.java` | HTTP Basic validates credentials against BCrypt hashes; `/signup` is public and protected routes require authentication. |
| Intended admin rule | `services/archive-service/src/main/java/org/orgarchive/archive/user/AdminRoleSecurityContract.java` | The generic interface declares `@PreAuthorize("hasRole('ADMIN')")` for both list and direct lookup. |
| User responses | `services/archive-service/src/main/java/org/orgarchive/archive/user/UserController.java` | Both responses omit organization UUIDs; `debug=true` adds password hashes to the list. Both methods implement the shared admin contract. |
| Internal organization queries | `services/archive-service/src/main/java/org/orgarchive/archive/user/OrganizationController.java` | A second implementation of the admin contract. The gateway does not publish `/organizations`, so it is absent from Postman. |
| Data model | `services/archive-service/src/main/java/org/orgarchive/archive/user/User.java` and `services/archive-service/src/main/java/org/orgarchive/archive/document/Document.java` | `app_user.organization_uuid` and `document.metadata.organizationUuid` represent the same logical tenant identifier; there is no organization table or foreign key. |
| Document search HTTP layer | `services/archive-service/src/main/java/org/orgarchive/archive/document/DocumentController.java` | Maps the three search endpoints and passes the authenticated username and metadata filters to the service. |
| Document search service | `services/archive-service/src/main/java/org/orgarchive/archive/document/DocumentSearchService.java` | Looks up the authenticated user's organization UUID and passes it with the filters to the repository. |
| Document persistence and search SQL | `services/archive-service/src/main/java/org/orgarchive/archive/document/DocumentRepository.java` | A concrete repository owns save, count, metadata filter conversion, the vulnerable search query, and the bound-parameter comparison. The organization UUID is always bound from the authenticated account. |
| Demo data | `services/archive-service/src/main/java/org/orgarchive/archive/seed/Seeder.java` | Multiple seeded organizations and documents make cross-organization access visible. |
| Live sequence | `postman/OrgArchive.postman_collection.json` | Signup → first unauthorized user list → debug hashes → Anna searches her own reports → SQL injection through `category` reveals other organizations' documents → safe comparison. |

## Questions to resolve while reading the code

- Which of these files should actually be shown in the IDE, and in what order, to keep the walkthrough short?
- How much of the HTTP Basic, method security, and UUID filtering should be explained before showing the first CVE?
- Should the database diagram appear on the structure slide, or should the two relevant fields be explained directly in code?
- Which recent PHP CVE, if any, is the intended opening reference?
- After the code walkthrough, how many vulnerability slides are still useful before Postman?
- Does the Spring AI MariaDB converter belong in this product's normal document-search path? If not, should the application concept change or should the second CVE change?

## Code review finding: document search and the converter

- `MariaDBFilterExpressionConverter` is a Spring AI component for translating a generic metadata-filter expression into a MariaDB SQL condition. In a normal Spring AI application it supports metadata filtering during a `MariaDBVectorStore` similarity search. It does not convert UUIDs and is not needed simply because tenants use UUIDs. [Spring AI MariaDB Vector Store reference](https://docs.spring.io/spring-ai/reference/1.0/api/vectordbs/mariadb.html#metadata-filtering).
- The search path now accepts optional `category` and `minRevision` metadata filters. `DocumentRepository` converts them to a SQL fragment and appends it after a bound server-side organization UUID condition. It does not use a vector store, embeddings, or similarity search. A category value containing SQL can bypass the organization condition in the vulnerable build.
- **Assessment:** the concern is valid. The converter is currently present to reproduce CVE-2026-22730, not because the product naturally needs Spring AI. This is likely to stand out when explaining the code in the IDE. The vulnerability is real, but the integration is a simplified demonstration rather than a typical Spring AI application. [Spring advisory](https://spring.io/security/cve-2026-22730/).
- **Possible direction A:** make OrgArchive a genuine semantic document-search service using `MariaDBVectorStore` and metadata filters. This would give the converter a natural role, but it adds embeddings, a model or embedding provider, vector storage, and more demo complexity.
- **Implemented direction B:** retain ordinary document search, add `category` and `minRevision` metadata filters, and use the converter to translate these filters. This makes filtering more plausible, though direct use of Spring AI's converter without its vector store still needs an architectural explanation.
- **Possible direction C:** keep the simple document archive and replace the second CVE with a vulnerability that naturally fits its existing stack.
- **Implementation revised:** document persistence and SQL search live together in one concrete repository. The client no longer sends an organization UUID or receives it at signup. The metadata filter uses `category` and optional `minRevision`; the second CVE remains reachable through `category`. Do not change the slides until the broader presentation revision.

### Follow-up clarification: vector search is not required by the converter

- MariaDB is a relational database. Newer MariaDB releases also have native vector storage and similarity-search features; a separate vector database is not inherently required. Spring AI documents MariaDB 11.7+ for its `MariaDBVectorStore`. Our Compose stack currently uses MariaDB 11.4 and does not use vector features. [Spring AI reference](https://docs.spring.io/spring-ai/reference/1.0/api/vectordbs/mariadb.html), [MariaDB vector reference](https://mariadb.com/docs/server/reference/sql-structure/vectors/).
- `Filter.Expression` represents a metadata predicate, such as `organizationUuid == X AND category == 'report' AND revision >= 2`. Spring AI's MariaDB converter turns such a predicate into MariaDB SQL over document metadata. The expression can contain multiple fields and operators; it is separate from vector similarity itself. [Spring AI metadata-filter reference](https://docs.spring.io/spring-ai/reference/1.0/api/vectordbs.html#metadata-filters).
- Therefore, the converter **can technically run without vector storage**, as it does in our MariaDB 11.4 app. It does not create embeddings or search vectors. However, it is distributed as part of Spring AI's MariaDB vector-store module, so importing that module solely to filter one UUID still needs a credible product reason.
- The current ordinary-search design accepts `category` and `minRevision` filters and uses the Spring AI expression model to compile them. Direct use of a vector-store component remains a deliberate design choice. A full `MariaDBVectorStore` integration is the stronger explanation if we want the CVE to appear in its typical environment; it would require MariaDB 11.7+, embeddings, and more setup.
- **Current status:** explanation recorded; no decision to change implementation yet.

### Follow-up clarification: metadata filters are exact predicates

- In OrgArchive, `document.title` and `document.content` are the document's main data. `document.metadata` is a JSON string with descriptive fields, currently `organizationUuid`, `category`, and `revision`. Metadata means “data describing the document”; it is not a semantic representation of its text.
- A metadata-filter request can ask for `category == 'report' AND revision >= 2`, while the server applies the signed-in user's organization UUID separately. The request cannot choose its authorization boundary. The current API accepts `category` and `minRevision`, or `{}` to return all documents in the current user's organization.
- `Filter.Expression` is a structured predicate. It can combine comparisons with `AND`, `OR`, `IN`, and other operators. `MariaDBFilterExpressionConverter` translates that structure into MariaDB SQL against JSON metadata. It is a translator between Spring AI's common filter language and MariaDB's syntax, not a similarity algorithm. [Spring AI filter reference](https://docs.spring.io/spring-ai/reference/1.0/api/vectordbs.html#metadata-filters).
- A metadata filter is exact according to its operators: `category == 'report'` does not match `"reports"` or an approximate category. `revision >= 2` is a numeric range, not fuzzy search. Semantic similarity, when used, comes from document embeddings and vector search; the filter can then narrow those results. Our current app has no embedding or vector search.
- **Design implication:** adding several filterable fields would explain why a structured expression is useful, but it would not by itself explain why this ordinary archive depends on a converter from Spring AI's vector-store module. Keep that architectural question open.

Add answers and further requirements here as we go through the code. Do not use this draft as permission to change the current presentation yet.

## Code review finding: metadata filter construction

- The organization UUID is loaded from the authenticated account and bound as the first JDBC parameter. A request with no metadata filters runs only that organization condition; it does not call the Spring AI converter or append a dummy `1 = 1` expression.
- The optional `category` filter means an exact string match. `minRevision` means `revision >= minRevision`, so it includes the specified revision and all later ones. Each supplied filter becomes a `Filter.Expression`; multiple expressions are joined with `AND` before conversion.
- `/documents/search-safe` builds the same logical predicates directly as SQL with JDBC placeholders. Its parameters are bound in order: organization UUID, category if present, then minimum revision if present. The category value is data, so an SQL payload cannot change the predicate. The vulnerable search instead appends the converter's SQL string after the bound organization condition.

## Code review finding: `AdminRoleSecurityContract` and the user endpoints

- `UserController implements AdminRoleSecurityContract<String, String, UserDto>`. Java therefore requires `UserController` to provide `list(String filter, boolean debug)` and `get(String id)`; `@Override` asks the compiler to verify those methods. The interface does not provide reusable implementation code.
- Spring MVC reads `@GetMapping` on the interface method and combines it with `@RequestMapping("/users")` on the controller, giving `GET /users`. No application code calls `list()` directly; Spring calls it when that HTTP route is requested.
- The intended `ADMIN` rule is `@PreAuthorize` on the interface method. With `@EnableMethodSecurity`, Spring Security is meant to find it when invoking the concrete method. The affected Spring Security versions can miss an annotation on a parameterized interface or overridden method when the concrete target method has no annotation. This is CVE-2025-22223. It is a Spring annotation-resolution problem, not ordinary Java inheritance of annotations. [Spring advisory](https://spring.io/security/cve-2025-22223/).
- **Revision implemented:** `AdminRoleSecurityContract<F,K,R>` now declares both `list` and `get`, each with `@PreAuthorize("hasRole('ADMIN')")`. `UserController` and `OrganizationController` both implement the contract. The organization routes exist only inside archive-service; the gateway does not forward them, and Postman does not use them.
- Consequence: in the vulnerable Spring Security version, the inherited admin check can be skipped on **both** `GET /users` and `GET /users/{username}`. The direct lookup no longer demonstrates a protected comparison. The fixed version must reject both for a regular user. Neither user response includes organization UUIDs.
- **Pending presentation revision:** the current slides and `SPEAKER-NOTES.de.md` still say that the direct user lookup is protected and that there is no organization controller. Update them only when the planned presentation revision begins.
- **Naming decision:** rename the shared interface to `AdminRoleSecurityContract` so its name makes the intended role requirement explicit. This is an authorization contract; HTTP Basic authentication is configured separately in `SecurityConfig`.

### Assessment of the shared admin contract

- Spring supports controller interfaces and method-security annotations on interfaces. A shared contract for admin-only `list`/`get` methods across user and organization controllers is a legitimate pattern. `@EnableMethodSecurity` is enabled; the HTTP filter requires authentication, and the interface expresses the additional ADMIN-role requirement.
- The first authorization bypass is caused by CVE-2025-22223 in affected Spring Security versions: it may fail to locate `@PreAuthorize` on parameterized interfaces when the concrete method lacks its own annotation. Our tests show USER access in the vulnerable build and rejection in the fixed 6.4.4 build. [Spring advisory](https://spring.io/security/cve-2025-22223/).
- This does not mean every design choice is beyond criticism. `AdminRoleSecurityContract` combines HTTP mappings, a security rule, and a `debug` parameter that has different meanings for users and organizations. The organization controller is internal and was reintroduced chiefly to give the contract a second implementation. These are presentation/architecture tradeoffs, not the CVE itself.
- The complete exploit also depends on separate weaknesses: `debug=true` exposes password hashes, Anna has a deliberately weak password, and the Spring AI converter is vulnerable to SQL injection. Spring Security's CVE explains the first unauthorized admin access, not the entire chain.
