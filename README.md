# OrgArchive

A local records platform for several organizations. People can create a workspace, share notes with colleagues, and search their organization's documents. This lab demonstrates two independent library vulnerabilities in one Postman chain: a Spring Security method-authorization bypass and a Spring AI SQL injection.

## Run

If this workspace still has containers or a database volume from an earlier version, run `make demo-reset` once. This deletes that lab database and starts OrgArchive with a fresh schema and seed. For a fresh checkout, use:

```bash
docker compose up -d --build --wait
```

The public gateway is at <http://localhost:8080>. Import [OrgArchive.postman_collection.json](postman/OrgArchive.postman_collection.json) into Postman and run the requests in order. After request 8, copy Bob's `passwordHash` and recover the lab password offline:

```bash
python3 -m pip install -r solve/requirements.txt
python3 solve/crack_bob.py '$2a$10$...'
```

Set the recovered value in the collection's local `bobPassword` variable, then continue. The signup request generates a new visitor username and password each run.

`make demo-reset` deletes the lab database volume and reseeds it. `make test-java` runs the Java tests. `make test-local` starts Compose and verifies the complete HTTP chain with `postman/check_demo.py` (requires `solve/requirements.txt`).

To compare the authorization behavior with the fixed Spring Security release:

```bash
mvn -f services/archive-service/pom.xml -Dspring-security.version=6.4.4 -Ddemo.fixed-security=true -Dtest=ArchiveChainIntegrationTest test
```

## Chain

1. The gateway's public API index lists routes. Anonymous `/users` and `/notes` return `401`.
2. The visitor signs up for a new organization. Own notes are empty; another organization's notes return `403`.
3. `/users` requires HTTP authentication, while its admin role rule lives on a generic controller interface. Spring Security 6.4.3 misses the inherited annotation ([CVE-2025-22223](https://spring.io/security/cve-2025-22223/)); a regular user reaches the admin listing. The debug response includes BCrypt hashes, an additional application-level data exposure. A directly annotated `/users/{username}` still rejects that user. Spring Security 6.4.4 rejects the list call too.
4. Bob's deliberately weak password is recovered from his hash. Bob and Maija see the same organization notes.
5. Spring AI MariaDB Store 1.0.3 emits an unescaped metadata filter value ([CVE-2026-22730](https://spring.io/security/cve-2026-22730/)). The vulnerable document query returns records from other organizations. `/documents/search-safe` binds both values and preserves isolation.

Only the gateway publishes an application port. The archive and MariaDB stay inside Compose. All seeded data is fictional.
