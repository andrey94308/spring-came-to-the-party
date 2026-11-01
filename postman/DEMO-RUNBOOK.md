# Live Postman runbook

1. `make demo-reset` for a clean MariaDB and LDAP seed, then import `MATO-document-search.postman_collection.json`.
2. Run requests 1–4. Anonymous endpoints return 401; `mr.president` with an empty Basic password receives the admin roster and its BCrypt hashes, but notes return 403.
3. Copy Bob's `passwordHash` from request 3. Run `python3 solve/crack_bob.py '<hash>'`, then set the recovered word in the collection's local `bobPassword` variable. Do not put it in a shared environment.
4. Run requests 5–8. Bob and Maija receive identical `intel-analysis` notes. Renaud gets different logistics notes. Bob cannot list users.
5. Run requests 9–12. Normal searches stay in Bob's department. The injected `department` value returns several departments including unassigned `mato-special-ops`. The same value on `/search-safe` returns an empty array.

The `query` field is accepted but unused. The lab centers on metadata filtering. The safe endpoint is an application-level parameterized query, not a patched Spring AI build.
