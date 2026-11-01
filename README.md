# Spring Came to the Party

A local, deliberately vulnerable MATO archive lab. The Postman chain starts without credentials and ends with a document from an unassigned department.

## Run

```bash
docker compose up --build
```

The gateway is at <http://localhost:8080>. Import `postman/MATO-document-search.postman_collection.json` and run requests in order. After request 3, copy Bob's `passwordHash` and run:

```bash
python3 -m pip install -r solve/requirements.txt
python3 solve/crack_bob.py '$2a$10$...'
```

Set the recovered value as the collection's local `bobPassword` variable, then continue. The `query` field is retained for request compatibility but unused; this lab demonstrates the `department` filter.

`make demo-reset` removes **both** MariaDB and LDAP volumes and rebuilds the seed. Use this after switching from an older version of the lab. `make test-java` runs the Java tests; `make test-local` checks the HTTP chain.

## Chain

1. Spring LDAP 3.3.7 accepts the empty-password LDAP bind when OpenLDAP is deliberately configured with `olcAllows: bind_anon_dn` ([CVE-2026-41720](https://spring.io/security/cve-2026-41720/)). The archive uses Boot 3.4.5 to preserve the existing Spring AI integration; LDAP 4.0.3 would require a Boot 4 migration.
2. The intentionally unsafe admin-only `/users` DTO discloses BCrypt hashes. Bob's password is the only supplied wordlist match.
3. Bob and Maija see shared `intel-analysis` notes. Authorization uses the database role and department.
4. Spring AI MariaDB Store 1.0.3 emits an unescaped filter value ([CVE-2026-22730](https://spring.io/security/cve-2026-22730/)). The application concatenates it into SQL after a bound authorized-department predicate. `/documents/search-safe` binds both values and preserves isolation; it is an application-level comparison, not the Spring AI patch.

The archive, LDAP, and MariaDB are internal Compose services. Only the gateway publishes an application port. All data is disposable lab data.
