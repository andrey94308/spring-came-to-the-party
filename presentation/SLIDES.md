# OrgArchive presentation outline

1. **OrgArchive.** Shared notes and documents for separate organizations. The live attack is shown later in Postman.
2. **Service architecture.** Public edge gateway, internal archive service, MariaDB, HTTP Basic, and Spring AI's metadata converter.
3. **API surface and access.** Public index and signup; authenticated user routes; intended admin listings. The public index is a presentation aid, not an authorization mechanism.
4. **CVE-2025-22223.** Spring Security 6.4.3 misses an inherited `@PreAuthorize` on the generic `AdminQuery<F,R>` interface. HTTP authentication still applies, so a signed-in regular user reaches the admin listing. Fixed in 6.4.4.
5. **Application mistakes after the bypass.** The optional admin debug response includes BCrypt password hashes. Bob's deliberately weak lab password can be recovered offline. The attacker keeps `ROLE_USER`; direct `/users/bob` remains forbidden.
6. **Organization boundaries.** Notes and document scope come from the authenticated user's database record. Ordinary cross-organization requests fail or return no documents.
7. **Search query construction.** The application binds the user's organization but appends the Spring AI converter's metadata expression to the SQL statement.
8. **CVE-2026-22730.** Spring AI MariaDB Store 1.0.3 emits an unescaped filter value. An injected `OR` escapes the `AND` scope and returns documents from other organizations. Fixed in 1.0.4.
9. **Safe comparison and fixes.** Bind both values, upgrade the affected libraries, keep role checks on concrete methods, and omit password hashes from responses. `/search-safe` is a local comparison, not a patched Spring AI build.
10. **Postman demonstration.** Sixteen ordered requests: discovery and signup; role bypass, hash recovery and notes; ordinary, injected and safe document searches.

Without an API index, an attacker may discover likely routes through documentation, client code, observed traffic, crawling, and targeted guesses. None of these guarantees a complete endpoint list. Access checks must work even when a route is known.
