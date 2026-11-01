# OrgArchive Postman runbook

1. Start the stack with `docker compose up -d --build --wait`. Use `make demo-reset` only when a fresh database is desired; it removes the lab volume.
2. Import `OrgArchive.postman_collection.json`. Requests 1–3 show the API index and `401` for anonymous users and notes.
3. Request 4 creates a new visitor in a new trial organization. Requests 5–6 show own notes and a `403` for another organization's notes.
4. Requests 7–9 show the method-authorization bypass. The ordinary visitor receives the admin roster; `debug=true` includes BCrypt hashes. A direct lookup still returns `403`, so the visitor's role remains `USER`.
5. Copy Bob's hash from request 8. Run `python3 solve/crack_bob.py '<hash>'` after installing `solve/requirements.txt`. Set the recovered word as the collection's local `bobPassword` variable.
6. Requests 10–12 compare notes shared within an organization and isolated between organizations. Requests 13–16 compare normal, foreign, injected, and parameterized document searches.

The accepted `query` field is currently unused. The SQL-injection demonstration concerns the `organization` metadata filter. The safe endpoint is an application-level parameterized comparison, not a patched Spring AI build.
