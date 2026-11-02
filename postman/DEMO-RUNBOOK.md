# OrgArchive Postman runbook

1. Run `make up` to start MariaDB, then start `ArchiveApplication` in IntelliJ (or run `make run`). The app runs at `http://localhost:8080` and seeds the database on its first start. If an older seed blocks startup, run `make demo-reset` and restart the app.
2. Import `OrgArchive.postman_collection.json`. Request 1 reads the public OpenAPI specification at `/v3/api-docs` to show the paths and methods. Request 2 shows `401` for anonymous `/users` access.
3. Request 3 creates a normal user; the response contains only the username. Request 4 shows that the new organization has no documents, without sending its UUID.
4. Request 5 is the first unauthorized access: the normal user receives the admin-only user list through CVE-2025-22223. The list does not include organization UUIDs. Request 6 enables `debug=true` and exposes BCrypt hashes for all users.
5. Explain that Anna's deliberately weak password can be matched to her leaked BCrypt hash with a short offline wordlist. The collection already contains `annaPassword=sunshine`; no cracking or manual variable entry is part of the live run.
6. Request 7 searches as Anna for `category=report` and returns only her organization's reports. Request 8 injects SQL through that category filter and reveals documents from other organizations. Request 9 sends the same input to the parameterized comparison endpoint and returns no matches.

The search request can contain `category` and `minRevision` metadata filters, or `{}` for all documents in the authenticated user's organization. The organization UUID is an internal tenant identifier and is never accepted as a search parameter. The safe endpoint is an application-level parameterized comparison, not a patched Spring AI build.
