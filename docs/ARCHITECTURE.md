# Architecture

```text
Postman → edge-gateway :8080 → archive-service :8081
                                  ↙               ↘
                              OpenLDAP          MariaDB
                              identities        users, shared notes, documents
```

Only port 8080 is published. The gateway forwards the three archive path families. Archive authentication calls Spring LDAP 3.3.7's `LdapContextSource.getContext` with the supplied Basic password. OpenLDAP deliberately permits a DN plus empty password with `olcAllows: bind_anon_dn`. A successful bind is mapped to a MariaDB profile and role; users absent from MariaDB are rejected.

`ROLE_ADMIN` lists the deliberately unsafe `/users` roster with BCrypt hashes. `ROLE_USER` reads notes and searches documents. Department scope comes from the authenticated user's database record. `mato-special-ops` has no user.

The vulnerable search combines a bound authorized department with Spring AI MariaDB Store 1.0.3's converter output. A quote in the requested department changes SQL precedence. The safe comparison binds both values.
