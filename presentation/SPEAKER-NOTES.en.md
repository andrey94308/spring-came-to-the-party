# OrgArchive — speaker outline (EN)

Discussion draft. These are prompts and key phrases, not a script to read aloud.

## 1. Short opening

- Show how two library vulnerabilities and application decisions form one exploit chain.
- I built a small demo project for this: OrgArchive.
- First the design and intended protection. Then the code and Postman.

## 2. What OrgArchive does

- Organizations store documents. A user sees documents from their own organization only.
- An administrator is responsible for accounts. In this prototype, they can list users or read one user's details.
- Each user has an internal `organizationUuid`. Documents carry the same identifier in JSON metadata, alongside `category` and `revision`.
- MariaDB 11.8 supports vectors. A future version could search for similar documents using text embeddings.
- There are no vectors or embeddings yet. Today we search by metadata. Spring AI translates `Filter.Expression` into a MariaDB SQL condition.
- Vector search concerns document content. Metadata provides exact additional filters.

## 3. System and route map

- Client → Archive Service on `localhost:8080` → MariaDB in Docker.
- `POST /signup` — public registration; the server creates a regular user and assigns a new organization UUID.
- `GET /users`, `GET /users/{id}` — authentication required; intended role: `ADMIN`.
- `GET /organizations`, `GET /organizations/{id}` — a second set of admin queries; not used in Postman.
- `POST /documents/search` — authentication and `USER` role; the organization comes from the stored account, not the request.
- `/documents/search-safe` and `/documents/search-debug` — comparison routes for the demo.
- `GET /` — a public index of all service routes; its labels describe the intended access rules.

## 4. Intended security

- Show `SecurityConfig`: HTTP Basic; `/` and `/signup` are public. Other routes require authentication.
- Passwords are stored as BCrypt hashes. Roles are not hashed; Spring Security uses them to check `USER` and `ADMIN`.
- Show `AdminRoleSecurityContract`: a shared interface for `list` and `get`; both methods declare `@PreAuthorize("hasRole('ADMIN')")`.
- Show `DocumentController` and `DocumentSearchService`: `@PreAuthorize("hasRole('USER')")`; the server loads the organization's UUID.
- Normally, the client cannot select another organization. Signup does not return the UUID.

## 5. Visible weakness in application code

- Show `UserController`: `GET /users?debug=true` returns BCrypt password hashes.
- The risk can look limited at first: the route is intended for administrators and the passwords are not plain text.
- Similar old debug decisions do occur in real projects. This one is intentional in the demo.

## 6. Vulnerabilities and chain

- **CVE-2025-22223:** the affected Spring Security version misses the role rule on a parameterized interface. Authentication is still required; a regular user reads the admin user list without the `ADMIN` role.
- `debug=true` reveals Anna's hash. Her weak password can be found offline with a short wordlist. It is already set in Postman.
- Anna searches her own reports with `category=report`. Normal search stays inside her organization.
- **CVE-2026-22730:** the Spring AI converter puts the `category` value into SQL without proper escaping. An injected `OR` bypasses the organization condition and returns other organizations' documents.
- `search-safe` binds values as SQL parameters. The same input cannot alter the query structure.

## 7. Demo and takeaway

- Postman: anonymous request → signup → first access to `/users` → hashes → authenticate as Anna → normal search → SQL injection → safe comparison.
- Check both the affected version and whether the vulnerable path is reachable in your application.
- Old application decisions can amplify a new CVE. Assess the whole chain and update dependencies in time.
