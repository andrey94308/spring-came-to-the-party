# Postman-first rewrite: from no credentials to cross-department documents

Status: implementation plan based on the current repository (30 October 2026).

## Goal

Build one Postman chain around the existing MATO archive:

```text
no credentials
    -> Spring LDAP empty-password authentication bypass
    -> authenticated as mr.president (admin)
    -> admin-only /users roster with BCrypt password hashes
    -> offline crack of bob's deliberately weak password
    -> login as bob
    -> notes shared by bob's department
    -> document search scoped to bob's department
    -> SQL injection returns documents from every department
```

In this lab, the existing `department` string is the organization or tenant key. Keep the API field, database field, and JSON metadata key named `department`. Isolation comes from the authenticated user's server-side record, never from a caller-supplied department. Retain the recognizable names `intel-analysis`, `logistics-corps`, `command-staff`, and `mato-special-ops`.

The baseline must show `401` for anonymous `/users` and `/notes`, `403` for an admin reading notes, and `403` for a regular user listing users. A normal request for a different department returns no documents. The injected request reaches the existing Spring AI converter and returns the document in `mato-special-ops`, where no seeded user belongs.

## What the project already has

| Existing implementation | Reuse | Change |
|---|---|---|
| Gateway routes `/users/**`, `/notes/**`, `/documents/**` to the internal archive service | Keep the services, static route shape, and port 8080 | Remove writable/public Actuator and `X-Debug-Trace` machinery |
| JPA `User`, `Note`, `Document`, repositories, controllers, Spring Security, HTTP Basic | Extend these classes and current API paths | Replace database password authentication with LDAP bind and database-backed roles |
| `User` has username, profile fields, BCrypt hash, role, department, `has2fa` | Keep the identity, hash, role, department, and existing names | Stop using the hash for HTTP authentication; retire `has2fa` with the OTP story |
| Java `Seeder` inserts BCrypt users, notes, and documents | Extend it and keep `WEAK_USER_PASSWORD` for `bob` | Add more coherent records and matching LDAP identities; remove command-control clues |
| `/notes` has a DTO without a scope selector | Keep endpoint and DTO | Replace `Note.username`/`findByUsername` with department scope from the authenticated user |
| `/documents/search` uses Spring AI 1.0.3 `MariaDBFilterExpressionConverter`, JSON metadata, and a server-side JDBC parameter | Keep the exact vulnerable query path, metadata shape, and request field | Restrict document endpoints to regular users |
| `/documents/search-safe` binds both values as parameters | Keep as the live safe-query comparison | No separate fixed Spring AI build is needed |
| Converter and MariaDB integration tests; four-request Postman collection | Adapt and extend them | Add LDAP, role, and shared-note checks |
| Compose, Makefile, runbooks, solver, slides | Keep build/reset conventions where useful | Replace the old four-flag/command-control narrative after the new chain works |

The current seed has four users, five notes, and six documents. `infra/mariadb/init/01-schema.sql` is a no-op: Hibernate `ddl-auto: update` creates tables, and the Java `Seeder` runs only when the user table is empty. A clean database volume or explicit migration is required for repeatable schema and seed changes.

## Vulnerabilities and dependency choice

The first step is **CVE-2026-41720**: Spring LDAP accepts an empty-password bind when the LDAP server permits unauthenticated binds. The [Spring advisory](https://spring.io/security/cve-2026-41720/) lists 4.0.0–4.0.3 and 3.3.0–3.3.7 as affected; 4.0.4 and 3.3.8 are the respective OSS fixes.

Prefer `org.springframework.ldap:spring-ldap-core:4.0.3`, the latest affected 4.0.x release. The current archive service uses Spring Boot 3.4.5, while the [LDAP 4.0.3 POM](https://repo.maven.apache.org/maven2/org/springframework/ldap/spring-ldap-core/4.0.3/spring-ldap-core-4.0.3.pom) declares Spring Framework 7 dependencies. Try migrating **only `archive-service`** to Boot 4; leave the gateway on its current Boot/Cloud versions. Verify that Spring AI 1.0.3 and the other archive integrations still compile and reproduce the HTTP-level SQL injection. If that migration disrupts the existing demo or expands the rewrite substantially, retain Boot 3.4.5 and pin Spring LDAP 3.3.7. Document the choice and verify the runtime dependency tree. Do not combine LDAP 4 with the existing Boot 3 dependency graph.

Do not present this as a generic Spring Security flaw. Authentication must exercise an affected Spring LDAP API (`AbstractContextSource`, `LdapTemplate`, or `LdapClient`) against an LDAP server configured to allow unauthenticated binds. Spring Security's standard LDAP provider may reject the empty password before it reaches Spring LDAP; if it does, use a small `AuthenticationProvider` that delegates the bind to Spring LDAP. The provider itself must have no branch that accepts an empty password. A known admin plus empty password succeeds; an unknown username plus empty password fails.

The final step is **CVE-2026-22730** in Spring AI's `MariaDBFilterExpressionConverter`. Keep the already pinned `spring-ai-mariadb-store:1.0.3` and the converter-to-JDBC path. The [Spring advisory](https://spring.io/security/cve-2026-22730/) identifies 1.0.4 as fixed. The existing `/documents/search-safe` endpoint demonstrates parameter binding without another application image. Label it as an **application-level safe query**, not as a reproduction of the Spring AI patch.

A fixed application build, fixed LDAP image, or patched Spring AI image is **not required**. A one-off dependency-only check with LDAP 4.0.4 (or 3.3.8 on Boot 3) is optional; if done, keep application code and Boot generation identical.

## Target architecture

```text
Postman -> edge-gateway :8080 -> archive-service :8081
                                      |          |
                                      v          v
                                   OpenLDAP   MariaDB
                                   identities users, notes, documents
```

Only the gateway is published for application traffic. MariaDB, LDAP, and archive-service stay on the Compose network. Remove `command-control` and its host port after the new flow passes. The gateway and archive service need not use the same Boot generation because they communicate over HTTP.

## Data model and expanded seed

Use `User.department` as the tenant key; no new `Organization` table is needed. Keep stored role values `admin` and `user`, mapped to `ROLE_ADMIN` and `ROLE_USER`. Reuse `mr.president` as the admin identity. Its current `command-staff` department may remain profile data but must confer no note or document access. Each regular user requires a nonblank department. Reject a successful LDAP bind if the user is absent from MariaDB or has an invalid role/department.

Change `Note.username` to `Note.department` and query by the authenticated user's department. This is the one required schema change for shared notes. Keep the existing note DTO (`id`, `title`, `body`). For the lab, use a clean-volume rebuild or an explicit migration; Hibernate `update` alone will not reliably remove the old column or backfill rows.

Keep `Document.metadata` as JSON text with a `department` key, the converter's key, and the current `DocumentDto`. Do not add a duplicate top-level department column. Keep `mato-special-ops` without a user account. Replace its current command-control URL, encoded president password, and OTP clues with a final document/flag for this chain.

Extend the current Java seeder to a realistic, repeatable dataset:

| Department | Users | Notes | Documents | Role in the demo |
|---|---:|---:|---:|---|
| `intel-analysis` | 5–6, including `bob` and `m.lehtinen` | 8–12 | 12–18 | Bob's normal scope; one shared note points to document search |
| `logistics-corps` | 4–5, including `r.dubois` | 6–10 | 10–15 | Clearly different tenant |
| `command-staff` | 3–4 regular users plus `mr.president` as admin | 6–10 | 10–15 | Ordinary data that admin still cannot read |
| `field-operations` | 3–4 regular users | 6–10 | 10–15 | Additional isolation check |
| `mato-special-ops` | 0 | 0 | 2–4 | Unassigned department; one document holds the final flag |

Target roughly 16–20 users, 25–40 notes, and 45–65 documents. Use varied titles, bodies, metadata, and harmless content so Postman responses resemble an archive. Preserve a few recognizable existing rows. Exactly one regular password (`bob`, from `WEAK_USER_PASSWORD`) should be easy to find in the supplied short wordlist; all others should be distinct and absent from it. Remove the current note that states Bob's password in plaintext, so the hash-cracking step remains necessary. Seed the same usernames and passwords in OpenLDAP and BCrypt hashes in MariaDB. Hashes are for the deliberate admin API leak, not for HTTP authentication.

Make both seeds deterministic from a clean Compose volume. Avoid a half-seeded state where `users.count() > 0` skips notes or documents after a partial startup. Document one reset for both MariaDB and LDAP. Assertions should use named fixtures and department membership rather than brittle total row counts.

## Authentication and authorization

Keep HTTP Basic for Postman:

1. Spring Security reads Basic credentials.
2. The provider binds through Spring LDAP.
3. After successful bind, the application loads the matching MariaDB user.
4. The database role becomes `ROLE_ADMIN` or `ROLE_USER`; the server-side department scopes notes and documents.
5. Headers, query parameters, and JSON never set the user's role or authorized department.

For the vulnerable run, `mr.president` plus an empty password reaches an LDAP unauthenticated bind. Both the permissive LDAP setting and affected Spring LDAP behavior are required. Non-empty passwords still work. The real admin password must not appear in the collection or presenter output.

| Endpoint | Anonymous | `ROLE_ADMIN` | `ROLE_USER` |
|---|---:|---:|---:|
| `GET /users` | `401` | `200` with hashes | `403` |
| `GET /notes` | `401` | `403` | `200`, own department only |
| `POST /documents/search` | `401` | `403` | `200`, normal requests scoped to own department |
| `POST /documents/search-safe` | `401` | `403` | `200`, own department only |
| `POST /documents/search-debug`, if retained | `401` | `403` | `200`, converter fragment only, no data |
| `/actuator/**`, Swagger, other paths | no public route | deny unless explicitly required | deny unless explicitly required |

Use explicit request matchers and `anyRequest().denyAll()`, plus method-level checks for sensitive data. Delete the public/debug user response split and `X-Debug-Trace` switch. Use the existing `UserDebugDto` fields as the basis for one deliberately unsafe **admin-only** roster DTO; remove `has2fa` and old Flag 1 output. A second DTO without `passwordHash` is optional discussion material, not a required endpoint.

`GET /notes` takes no username or department selector. Two `intel-analysis` users see the same notes; `r.dubois` sees only `logistics-corps` notes. Scope comes from the authenticated username or a trusted principal.

Keep the `POST /documents/search` request field `department`. The current query already binds the authorized department and appends the vulnerable converter output:

```sql
WHERE JSON_VALUE(metadata, '$.department') = ?
  AND <MariaDBFilterExpressionConverter output for requested department>
```

For `bob`, a plain `logistics-corps` filter returns `[]`. The input `intel-analysis' OR '1'='1` makes the unescaped converter predicate true for all rows because `AND` has higher precedence than `OR`. Exercise normal, foreign, injected, and `/search-safe` requests through HTTP. The accepted `query` field is currently unused; retain it only for collection compatibility and explain that this is a department-filter demo, not full-text search.

## Response contracts

Keep existing URL paths and JSON shapes where possible. `GET /users` as admin returns an array containing records like:

```json
{"username":"bob","displayName":"Bob Brennan","email":"bob@mato.int","role":"user","department":"intel-analysis","passwordHash":"$2a$..."}
```

`GET /notes` returns the current array of `{ "id", "title", "body" }` records, now with department-based membership. Document requests retain the current form:

```json
{"query":"briefing","department":"intel-analysis"}
```

`POST /documents/search` and `/search-safe` return the existing document DTO (`id`, `title`, `content`, `metadata`). Metadata identifies the department. The injected response includes documents from several departments and the final `mato-special-ops` document; `/search-safe` with the same input returns no cross-department documents.

## Live chain and Postman collection

Extend `postman/MATO-document-search.postman_collection.json` instead of creating a parallel collection. Reuse its normal, foreign-department, injected, and safe search requests:

1. **Baseline:** anonymous `GET /users` and `GET /notes` both return `401`.
2. **LDAP bypass:** `GET /users` as `mr.president` with an empty Basic password returns `200` and hashes. `GET /notes` with the same credentials returns `403`.
3. **Offline hash recovery:** copy `bob`'s hash and compare it with `solve/wordlist.txt` using a small local helper. Do not add a cracking HTTP endpoint or pre-populate the recovered password in the shared Postman environment.
4. **Shared notes:** `GET /notes` as `bob` and `m.lehtinen` returns the same `intel-analysis` notes, including the search hint. `GET /users` as `bob` returns `403`. A `logistics-corps` user sees disjoint notes.
5. **Document boundary:** as `bob`, search `intel-analysis`, then `logistics-corps` (empty). Send `intel-analysis' OR '1'='1` to `/documents/search`; several departments and `mato-special-ops` appear. Send the same input to the existing `/documents/search-safe`; no cross-department documents appear.

Show the generated predicate through `/search-debug` only if it is access-controlled; otherwise use an integration-test assertion or presenter notes. A fixed LDAP build and patched Spring AI build are optional discussion material, not live collection steps.

## Implementation order

1. **Preserve the working SQL-injection experiment.** Keep `DocumentController`, Spring AI 1.0.3, its converter test, search/safe endpoints, and MariaDB integration tests as the baseline. Pin and verify the runtime JAR.
2. **Expand seeds and share notes.** Extend `Seeder`, convert notes to department scope, keep existing identities and document metadata, replace the final document, and make reset deterministic. Keep the database runnable after this step.
3. **Add LDAP.** Add OpenLDAP and LDIF seeds to Compose. Try archive-only Boot 4 + LDAP 4.0.3; use Boot 3.4.5 + LDAP 3.3.7 if the existing SQL-injection path cannot be preserved. Verify the LDAP version and empty-bind behavior through HTTP.
4. **Tighten roles.** Adapt `ArchiveUserDetailsService`/`SecurityConfig`, make `/users` admin-only and notes/documents user-only, remove the debug header from archive and gateway, and keep useful DTOs.
5. **Replace old assets, then clean up.** Extend the collection; adapt `IntegrationFlagsTest`, `Cve202622730ConverterTest`, Makefile, README, solver, runbooks, architecture diagram, and slides. Once the new chain passes, remove `command-control`, obsolete Compose variables, old flags, Actuator exploit code, and related tests. Keep the safe SQL comparison.

## Automated checks

- The vulnerable runtime contains Spring LDAP 4.0.3, or 3.3.7 on the documented Boot 3 path, and Spring AI MariaDB Store 1.0.3.
- Permissive LDAP plus `mr.president:""` authenticates; unknown username plus empty password does not. A valid non-empty regular password authenticates.
- Anonymous `/users` and `/notes` return `401`; admin `/users` returns `200` with hashes; admin `/notes` returns `403`; user `/users` returns `403`.
- `bob` and `m.lehtinen` receive the same `intel-analysis` notes; `r.dubois` receives only `logistics-corps` notes. Spoofed headers and query parameters do not change scope.
- A normal document filter for another department returns no rows. The injected filter returns several departments and the unassigned final document through HTTP. The same input on `/search-safe` returns no cross-department documents.
- Generated SQL retains a server-bound authorized-department parameter and a converter-generated requested-department predicate. The converter test demonstrates the unescaped quote in Spring AI 1.0.3.
- A clean Compose reset restores LDAP and MariaDB; Postman tests use stable fixtures and pass from a clean checkout.

## Acceptance criteria

- `docker compose up --build` starts gateway, archive service, LDAP, and MariaDB; only the gateway is published for application traffic.
- The old command-control, OTP/JWT branch, public writable Actuator, and gateway debug-header mechanism are absent.
- The existing document-search SQL-injection route and safe parameterized comparison still work.
- The archive has roughly 16–20 users, 25–40 shared notes, and 45–65 documents across several populated departments plus unassigned `mato-special-ops`.
- An empty password authenticates the known admin only under vulnerable LDAP conditions; the admin can list hashes but cannot read department notes or documents.
- Cracking `bob`'s deliberately weak hash yields a valid LDAP password. Bob and another `intel-analysis` user share notes; a different department sees different notes.
- Plain cross-department document requests return no rows; the injected request returns documents from multiple departments and the final unassigned document.
- The collection, helper, tests, and reset instructions work from a clean checkout. A separate fixed build is not required.

## Presentation message

The chain crosses four boundaries: Spring LDAP plus a permissive LDAP server authenticates the empty password; an intentionally unsafe admin DTO leaks BCrypt hashes; a recovered regular password grants legitimate access to shared department notes; and the Spring AI converter plus application SQL composition breaks document isolation. The existing `/search-safe` endpoint shows how parameter binding preserves isolation. Name the library CVEs, unsafe application choices, and LDAP server precondition separately.
