# OrgArchive architecture

```text
Postman -> archive-service on localhost:8080 -> MariaDB in Docker on localhost:3306
                    |
                    +-- Spring Security HTTP Basic and role checks
                    +-- Spring AI metadata filter converter
```

The archive service runs directly from IntelliJ on the host. It publishes a generated OpenAPI specification at `GET /v3/api-docs`, listing its routes and HTTP methods. The specification documents HTTP Basic authentication; access is enforced separately by Spring Security. MariaDB alone runs in Docker. The archive authenticates HTTP Basic credentials against BCrypt hashes in MariaDB. Public signup creates a `ROLE_USER` account with a new server-generated `organizationUuid`, stored only in the user record; the response contains the username. There is no separate login endpoint.

`/users` requires an authenticated request. Its intended admin rule for both list and direct lookup is declared on the generic `AdminRoleSecurityContract` interface, which is the affected method-security shape in Spring Security 6.4.3. Neither user response exposes organization UUIDs. The same contract is implemented by `/organizations`, which is now reachable directly and listed in OpenAPI; the Postman chain does not use it. `/documents` requires a regular user role and derives the authorized organization UUID from the stored account; a request field cannot change membership. The UUID is a tenant identifier, not a secret or an authorization token.

The normal admin user list omits password hashes. A deliberately flawed `debug=true` response includes them. Document search always binds the authenticated user's organization UUID and accepts optional metadata filters `category` and `minRevision`. The vulnerable search converts those filters to SQL using Spring AI; the safe comparison binds filter values as JDBC parameters. Spring AI is a separate Spring library for AI applications and vector-store integrations. This demo uses only its MariaDB metadata-filter converter, without an AI model or vector search; MariaDB itself is not the component named by CVE-2026-22730.

The Compose stack and integration tests use MariaDB 11.8, which has native vector support. Compose enables `MARIADB_AUTO_UPGRADE` so an existing lab volume receives the server's required system-table upgrade. The application does not yet create vector columns, store embeddings, or call `MariaDBVectorStore`; changing the database version alone does not add those features. `document.organization_uuid` is a separate indexed column; the `metadata` JSON contains only search attributes such as `category` and `revision`. On startup, `DocumentOrganizationMigration` moves organization UUIDs out of legacy metadata without resetting the database volume.

For document search, `DocumentController` handles the HTTP routes and role check. `DocumentSearchService` resolves the allowed organization from the authenticated user's stored account. The concrete `DocumentRepository` contains document persistence, the JDBC search queries, and Spring AI filter conversion. The vulnerable and safe search paths use the same repository boundary.
