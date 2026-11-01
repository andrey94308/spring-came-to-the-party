# OrgArchive architecture

```text
Postman -> edge-gateway :8080 -> archive-service :8081 -> MariaDB
                                     |                   |
                                     |                   +-- users, notes, documents
                                     +-- Spring Security HTTP Basic
                                     +-- Spring AI metadata filter converter
```

The gateway exposes a public API index at `GET /` and routes the other API paths to the archive. The archive authenticates HTTP Basic credentials against BCrypt hashes in MariaDB. Public signup creates a `ROLE_USER` account in a new server-assigned organization. Only the gateway publishes a host port.

`/users` requires an authenticated request. Its admin role rule is inherited from a generic `AdminQuery` interface, which is the affected method-security shape in Spring Security 6.4.3. The direct `/users/{username}` method has its own admin annotation and remains protected. `/notes` and `/documents` require regular user roles and derive the authorized organization from the stored account; a request field cannot change membership.

The normal admin roster omits password hashes. A deliberately flawed `debug=true` response includes them. The document search combines a bound organization condition with Spring AI's vulnerable metadata expression; the safe comparison binds both values.
