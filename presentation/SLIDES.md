# MATO archive: from empty password to cross-department documents

## 1. The live question

Can a caller with no credentials reach a document in a department with no user account?

Demo starts at `GET /users` and ends at a `mato-special-ops` document.

## 2. The boundary map

Postman reaches only `edge-gateway:8080`. The archive authenticates against OpenLDAP and loads roles, departments, notes, and document metadata from MariaDB. Only the gateway port is published.

## 3. Baseline

Anonymous `/users` and `/notes` return 401. Admin cannot read notes or documents. Regular users cannot list users. Bob's normal request for `logistics-corps` returns `[]`.

## 4. Empty-password bind

`mr.president` with an empty Basic password receives the admin roster. The app calls affected Spring LDAP 3.3.7. OpenLDAP explicitly permits DN plus empty-password binds with `olcAllows: bind_anon_dn`. An unknown username is rejected after the bind because no database profile exists.

## 5. Hash leak and offline recovery

The admin-only roster deliberately includes BCrypt hashes. Copy Bob's hash and compare it with `solve/wordlist.txt` using `solve/crack_bob.py`. Exactly one regular password is in the list. The recovered password authenticates through LDAP.

## 6. Shared department notes

Bob and Maija receive identical `intel-analysis` notes. Renaud sees logistics notes. One intel note points to document search. The endpoint accepts no scope selector; the app loads the department from the authenticated database record.

## 7. Document search boundary

The ordinary SQL query binds Bob's authorized department and asks Spring AI's MariaDB converter for the requested filter. An ordinary foreign-department value cannot cross the boundary.

## 8. Injection and comparison

The requested value `intel-analysis' OR '1'='1` appears unescaped in the Spring AI 1.0.3 converter output. SQL evaluates the `OR` after the scoped `AND`, returning several departments and the final unassigned document. `/search-safe` binds both values and returns `[]` for the same input.

## 9. Where each failure sits

CVE-2026-41720: affected Spring LDAP API plus permissive LDAP server. Application choice: admin DTO leaks hashes. Weak seed: Bob's password is recoverable. CVE-2026-22730: Spring AI converter emits an unescaped quote. Application choice: SQL concatenation allows the predicate to break isolation.

## 10. Live sequence

Run the 12 Postman requests in order. Pause after request 3 for offline hash recovery. Set only the local `bobPassword` collection variable. Compare normal, foreign, injected, and safe search responses at the end.
