# OrgArchive

A local document archive for several organizations. Public signup assigns a new organization UUID, which stays in the user's account; document searches derive it from authentication. The lab runs MariaDB 11.8, which supports vectors, although this application currently uses metadata filtering rather than vector search. This lab demonstrates two independent library vulnerabilities in one Postman chain: a Spring Security method-authorization bypass and a Spring AI SQL injection.

## Run

Start only MariaDB in Docker:

```bash
make up
```

Run `org.orgarchive.archive.ArchiveApplication` from IntelliJ, or use `make run`. Its defaults connect to MariaDB at `localhost:3306` and serve the API at <http://localhost:8080>. Hibernate creates the tables and `Seeder` inserts demo data on the first application start; the database container does not seed the records itself.

Import [OrgArchive.postman_collection.json](postman/OrgArchive.postman_collection.json) into Postman and run its 9 requests in order. The service publishes an OpenAPI specification at <http://localhost:8080/v3/api-docs>; the first request reads its paths and methods. This documents routes but does not grant access to them. Anna's weak lab password is already set in the collection's `annaPassword` variable, so the presentation does not pause for a cracking step. Request 6 exposes the users' BCrypt hashes. Signup generates a new username and password for the demo; the server assigns an internal organization UUID.

If an old or partial lab seed prevents startup, run `make demo-reset` and restart `ArchiveApplication`. This deletes the lab database volume; the Java seeder fills the new database when the app starts. An optional offline helper remains at `solve/crack_anna.py` for verifying Anna's password against the leaked hash. `make test-java` runs the Java tests. With the application running locally, `make test-local` verifies the HTTP chain (requires `solve/requirements.txt`).

To compare the authorization behavior with the fixed Spring Security release:

```bash
mvn -f services/archive-service/pom.xml -Dspring-security.version=6.4.4 -Ddemo.fixed-security=true -Dtest=ArchiveChainIntegrationTest test
```

## Chain

1. The public OpenAPI specification lists routes. Anonymous `/users` returns `401`.
2. A normal user signs up and sees an empty list of documents in their new organization. The client does not send an organization UUID.
3. `/users` requires HTTP authentication, while its intended admin rules for both list and direct lookup live on a generic controller interface also implemented by the organization controller. Spring Security 6.4.3 misses the inherited annotations ([CVE-2025-22223](https://spring.io/security/cve-2025-22223/)); a regular user can reach these admin queries. The `debug=true` user-list response includes BCrypt hashes, an additional application-level data exposure. Neither user response contains organization UUIDs. The organization routes are listed in OpenAPI but are not used in the Postman chain. Spring Security 6.4.4 rejects the admin queries for regular accounts.
4. Anna's deliberately weak password matches the leaked BCrypt hash. The collection already contains that lab password and uses Anna's account for document searches.
5. Anna searches for documents with `category=report` and sees only her organization's reports. Spring AI MariaDB Store 1.0.3 emits an unescaped metadata filter value ([CVE-2026-22730](https://spring.io/security/cve-2026-22730/)). Injecting SQL through `category` makes the vulnerable query return documents from other organizations. `/documents/search-safe` binds the category as a parameter and preserves isolation.

The Java application runs on the host; Compose publishes only MariaDB on the local loopback interface. All seeded data is fictional.
