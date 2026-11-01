# OrgArchive architecture

```text
Postman -> edge-gateway :8080 -> archive-service :8081 -> MariaDB
                                     |                   |
                                     |                   +-- users, documents
                                     +-- Spring Security HTTP Basic
                                     +-- Spring AI metadata filter converter
```

The gateway exposes a public API index at `GET /` and routes the other API paths to the archive. The archive authenticates HTTP Basic credentials against BCrypt hashes in MariaDB. Public signup creates a `ROLE_USER` account with a new server-generated `organizationUuid` and returns that UUID in the signup response. There is no separate login endpoint. Only the gateway publishes a host port.

`/users` requires an authenticated request. Its intended admin rule for both list and direct lookup is declared on the generic `AdminRoleSecurityContract` interface, which is the affected method-security shape in Spring Security 6.4.3. Neither user response exposes organization UUIDs. The same contract is implemented by an internal `/organizations` controller; the gateway does not route that path, so its organization UUIDs are not available through the public API. `/documents` requires a regular user role and derives the authorized organization UUID from the stored account; a request field cannot change membership. The UUID is a tenant identifier, not a secret or an authorization token.

The normal admin user list omits password hashes. A deliberately flawed `debug=true` response includes them. The document search combines a bound `organizationUuid` condition with Spring AI's vulnerable metadata expression; the safe comparison binds both values. Spring AI is a separate Spring library for AI applications and vector-store integrations. This demo uses only its MariaDB metadata-filter converter, without an AI model or vector search; MariaDB itself is not the component named by CVE-2026-22730.
