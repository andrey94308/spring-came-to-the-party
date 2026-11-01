# OrgArchive Postman runbook

1. Start the stack with `docker compose up -d --build --wait`. If an older seed is present, run `make demo-reset` once; it removes and recreates the lab volume.
2. Import `OrgArchive.postman_collection.json`. Requests 1–3 show the API index and `401` for anonymous users and notes.
3. Request 4 creates a new visitor in a new trial organization. Requests 5–6 show own notes and a `403` for another organization's notes.
4. Requests 7–9 show the method-authorization bypass. The ordinary visitor receives the admin roster; `debug=true` includes BCrypt hashes. A direct lookup still returns `403`, so the visitor's role remains `USER`.
5. Explain that Anna's deliberately weak password can be matched to the leaked BCrypt hash with a short offline wordlist. The collection already contains `annaPassword=sunshine`; no cracking or manual variable entry is part of the live run.
6. Requests 10–13 compare normal, foreign, injected, and parameterized document searches as Anna.

The accepted `query` field is currently unused. The SQL-injection demonstration concerns the `organization` metadata filter. The safe endpoint is an application-level parameterized comparison, not a patched Spring AI build.
