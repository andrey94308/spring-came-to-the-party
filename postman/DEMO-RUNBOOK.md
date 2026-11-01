# OrgArchive Postman runbook

1. Start the stack with `docker compose up -d --build --wait`. If an older seed is present, run `make demo-reset` once; it removes and recreates the lab volume.
2. Import `OrgArchive.postman_collection.json`. Requests 1–2 show the API index and `401` for anonymous users.
3. Request 3 creates a normal user and returns its new `organizationUuid`. Request 4 shows that the new organization has no documents.
4. Request 5 is the first unauthorized access: the normal user receives the admin-only user list through CVE-2025-22223. The list reveals organization UUIDs used later. Request 6 enables `debug=true` and exposes BCrypt hashes for all users.
5. Explain that Anna's deliberately weak password can be matched to her leaked BCrypt hash with a short offline wordlist. The collection already contains `annaPassword=sunshine`; no cracking or manual variable entry is part of the live run.
6. Requests 7–10 compare normal, foreign, injected, and parameterized document searches as Anna.

The request body contains only `organizationUuid`. The SQL-injection demonstration concerns that metadata filter. The UUID is a tenant identifier; authorization still uses the authenticated account. The safe endpoint is an application-level parameterized comparison, not a patched Spring AI build.
