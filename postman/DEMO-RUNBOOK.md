# OrgArchive Postman runbook

1. Start the stack with `docker compose up -d --build --wait`. If an older seed is present, run `make demo-reset` once; it removes and recreates the lab volume.
2. Import `OrgArchive.postman_collection.json`. Requests 1–3 show the API index and `401` for anonymous users and notes.
3. Request 4 creates a normal user in a new trial organization. Request 5 shows that the user's own notes are empty.
4. Request 6 is the first unauthorized access: the normal user receives the admin-only user list through CVE-2025-22223. The list reveals organization names used later. Request 7 enables `debug=true` and exposes BCrypt hashes for all users.
5. Explain that Anna's deliberately weak password can be matched to her leaked BCrypt hash with a short offline wordlist. The collection already contains `annaPassword=sunshine`; no cracking or manual variable entry is part of the live run.
6. Requests 8–11 compare normal, foreign, injected, and parameterized document searches as Anna.

The request body contains only `organization`. The SQL-injection demonstration concerns that metadata filter. The safe endpoint is an application-level parameterized comparison, not a patched Spring AI build.
