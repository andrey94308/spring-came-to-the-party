# OrgArchive

A local document archive for several organizations. Public signup assigns a new organization UUID, which stays in the user's account; document searches derive it from authentication. The lab runs MariaDB 11.8, which supports vectors, although this application currently uses metadata filtering rather than vector search. This lab demonstrates two independent library vulnerabilities in one Postman chain: a Spring Security method-authorization bypass and a Spring AI SQL injection.

## Run

If this workspace still has containers or a database volume from an earlier version, run `make demo-reset` once. This deletes that lab database and starts OrgArchive with a fresh schema and seed. For a fresh checkout, use:

```bash
docker compose up -d --build --wait
```

The public gateway is at <http://localhost:8080>. Import [OrgArchive.postman_collection.json](postman/OrgArchive.postman_collection.json) into Postman and run its 9 requests in order. Anna's weak lab password is already set in the collection's `annaPassword` variable, so the presentation does not pause for a cracking step. Request 6 exposes the users' BCrypt hashes. Signup generates a new username and password for the demo; the server assigns an internal organization UUID.

If an older OrgArchive database volume contains the previous seed, run `make demo-reset` once before the new collection. This deletes and reseeds the lab database. An optional offline helper remains at `solve/crack_anna.py` for verifying the password against the leaked hash outside the live demo.

`make demo-reset` deletes the lab database volume and reseeds it. `make test-java` runs the Java tests. `make test-local` starts Compose and verifies the complete HTTP chain with `postman/check_demo.py` (requires `solve/requirements.txt`).

To compare the authorization behavior with the fixed Spring Security release:

```bash
mvn -f services/archive-service/pom.xml -Dspring-security.version=6.4.4 -Ddemo.fixed-security=true -Dtest=ArchiveChainIntegrationTest test
```

## Chain

1. The gateway's public API index lists routes. Anonymous `/users` returns `401`.
2. A normal user signs up and sees an empty list of documents in their new organization. The client does not send an organization UUID.
3. `/users` requires HTTP authentication, while its intended admin rules for both list and direct lookup live on a generic controller interface shared with an internal organization controller. Spring Security 6.4.3 misses the inherited annotations ([CVE-2025-22223](https://spring.io/security/cve-2025-22223/)); a regular user can reach the user list and direct lookup. The `debug=true` list response includes BCrypt hashes, an additional application-level data exposure. Neither user response contains organization UUIDs. The organization routes are not published through the gateway. Spring Security 6.4.4 rejects both user queries for regular accounts.
4. Anna's deliberately weak password matches the leaked BCrypt hash. The collection already contains that lab password and uses Anna's account for document searches.
5. Anna searches for documents with `category=report` and sees only her organization's reports. Spring AI MariaDB Store 1.0.3 emits an unescaped metadata filter value ([CVE-2026-22730](https://spring.io/security/cve-2026-22730/)). Injecting SQL through `category` makes the vulnerable query return documents from other organizations. `/documents/search-safe` binds the category as a parameter and preserves isolation.

Only the gateway publishes an application port. The archive and MariaDB stay inside Compose. All seeded data is fictional.
