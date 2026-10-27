# Implementation Notes — Spring Came to the Party

> Working journal of implementation decisions and test results.

---

## 2026-04-28 — Project bootstrap

- Started from an existing repository snapshot.
- Initial repository state contained only a placeholder `README.md`.
- Decided on stack: Java 21, Spring Boot 3.4.5, Maven, Docker Compose, Python for solve scripts. Reasons: latest CVEs target this generation, Maven has more documentation than Gradle for the Spring Boot ecosystem.
- Added `.gitignore`, replaced placeholder `README.md` with a player-facing description.
- Created scaffolding folders: `app1-gateway/`, `internal-service/`, `app2-auth/`, `solve/`, `docs/`.
- Built the services **without** vulnerabilities first, then layered the CVE demonstrations in afterwards.

### Design decisions

- **No frontend**: deliberately removed CORS / browser complexity. All player interaction is via `curl` / Python — also makes the official solve script trivial to run as `make test-local`.
- **`.mcp.json` as the entry point**: simulates the realistic "AI assistant accidentally commits a secret" scenario (we cite the public Claude Code incident here).
- **One vulnerability per service, chained linearly**: forces the player to actually progress through stages, prevents skipping ahead.

---

## 2026-05-01 — End-to-end working

### Vulnerable dependency versions pinned

| Component                      | Version             | CVE                                                                       |
| ------------------------------ | ------------------- | ------------------------------------------------------------------------- |
| `spring-cloud-starter-gateway` | 4.2.0 (BOM-managed) | CVE-2025-41243 (4.2.x ≤ 4.2.4 affected)                                   |
| `spring-security-*`            | 6.4.4 (override)    | CVE-2025-22234 (the `spring-security.version` BOM property is overridden) |
| `spring-ai-mariadb-store`      | 1.0.3               | CVE-2026-22730                                                            |

### Implementation notes

- `MariaDBFilterExpressionConverter` constructor takes a `metadataFieldName` (we pass `"metadata"`). The converter emits SQL of the form `JSON_VALUE(metadata, '$.<key>') = '<value>'` — values are `String.format("'%s'", v)` with no escaping. That's the CVE-2026-22730 surface in 1.0.3.
- The CVE-2025-41243 SpEL payload `@environment.getPropertySources()[1].source['APP_ENV']='dev'` does NOT evaluate against Spring Boot 3.4.5's `ApplicationReactiveWebEnvironment` — SpEL's reflective accessor doesn't resolve the inherited `getPropertySources()` from the runtime class. Solution: seed `APP_ENV` into `System.getProperties()` instead, and use `@systemProperties['APP_ENV']='dev'` in the payload (this works against the real vulnerable SCG dependency). Vulnerability write-up: <https://blog.z3r.ru/posts/spring-cloud-gateway-spel-vuln/>.

#### `restrictive-property-accessor` — what it actually gates

`spring.cloud.gateway.restrictive-property-accessor.enabled` (default `true` in SCG 4.2.x) installs a `RestrictivePropertyAccessor` into the SpEL evaluation context that route definitions are parsed in. It only intercepts the **`PropertyAccessor`** chain — i.e. the SpEL AST nodes that resolve `obj.foo` style dot-access on a target object. When enabled, those reads/writes are restricted to a small whitelist (essentially safe `String`/primitive properties), which is what stops the canonical PoC payload `@environment.getPropertySources()[1].source['APP_ENV']='dev'`: the `.getPropertySources()` method invocation and the `.source` dot-read both have to clear the accessor.

What the restrictive accessor does **not** intercept:

| SpEL node                                    | Example                        | Gated by accessor?   |
| -------------------------------------------- | ------------------------------ | -------------------- |
| `BeanResolver` (`@bean`)                     | `@systemProperties`            | No                   |
| `Indexer` (`[key]`) on Map/Properties/List   | `@systemProperties['APP_ENV']` | No                   |
| Direct method invocation on the target value | `'abc'.length()`               | No (separate gating) |
| `PropertyAccessor` (`.field` / `.getter`)    | `@env.systemProperties`        | **Yes**              |

This is why our payload `#{@systemProperties['APP_ENV']='dev'}` succeeds even with the restrictive accessor at its default `true`: it never touches `PropertyAccessor`. It's a pure `BeanResolver → Indexer → assignment` chain. The accessor is bypassed by SpEL syntax choice, not by being disabled.

A payload that _does_ require disabling the restrictive accessor first (the canonical CVE PoC):

```spel
#{@environment.getPropertySources()[1].source['APP_ENV']='dev'}
```

Here `.getPropertySources()` (method call resolved via PropertyAccessor) and `.source` (dot read on `MapPropertySource`) both go through the gate. The two-step exploit — POST a route that flips `restrictive-property-accessor.enabled` off, then POST the env-flip route — is for this shape of payload. With the indexer-only payload we use, the unlock step is dead code, so `solve_flag1.py` no longer performs it.

- The CVE-2025-22234 73-byte timing path also doesn't manifest the way the public write-ups describe — in 6.4.4 the BCrypt encoder rejects >72 bytes BEFORE the dummy hash runs in _both_ the absent and present-user paths, so timings stay equal. To expose the same observable leak (username enumeration via response time), the login controller does the user lookup + password match itself, with no dummy-hash mitigation. Both `UserDetailsService` and `BCryptPasswordEncoder` are still the stock Spring Security 6.4.4 components — we only bypass the `DaoAuthenticationProvider` mitigation, which is exactly how the CVE manifests in apps that roll their own login flow.

### Test posture

- **internal-service**: `Cve202622730ConverterTest` (unit; pins the SQL-injection surface in the converter). `IntegrationFlagsTest` covers `/users` prod vs dev, `/notes` auth + Flag 2 placement, `/documents/search` honest-vs-injection paths, and actuator exposure. The integration test needs a Docker daemon Testcontainers can talk to — Docker 29.1 on Docker Desktop currently returns stub info for the testcontainers strategies; the same scenarios are covered end-to-end by the Python solve scripts.
- **app1-gateway**: 3 tests — actuator/gateway/routes reachable unauth, actuator/env reachable unauth, `APP_ENV` is in a SpEL-writable place.
- **app2-auth**: 6 tests — timing leak gap is observable (median ~100 ms vs ~2 ms in CI), OTP rejects wrong codes, OTP+JWT round-trip unlocks `/warheads` with Flag 4, `/warheads` rejects forged tokens.

### `make test-local` outcome

Stack boots → Flag 1 (SpEL) → Flag 2 (BCrypt + notes) → Flag 3 (SQL injection) → Flag 4 (timing + OTP brute-force, ~3.5 s on this dev box). All four flags recovered.

---

## 2026-05-01 — MATO rename + audit-trace refactor

Renamed everything to a consistent MATO (parody-NATO) theme so the repository can be understood top-down:

| Old                 | New                         |
| ------------------- | --------------------------- |
| `app1-gateway/`     | `services/edge-gateway/`    |
| `internal-service/` | `services/archive-service/` |
| `app2-auth/`        | `services/command-control/` |
| `mariadb/`          | `infra/mariadb/`            |

Replaced the `APP_ENV` / `X-App-Env: dev` switch (which made the CVE-2025-41243 stage feel like a contrived dev/prod toggle) with a **MATO audit / debug-trace** flag (`mato.gateway.audit.debug-trace`). The gateway forwards `X-Debug-Trace: true` downstream when the flag is on; archive-service returns the verbose response (passwordHash + Flag 1) only on that header. The CVE payload is now `#{@systemProperties['mato.gateway.audit.debug-trace']='true'}` — same indexer-syntax bypass of the restrictive PropertyAccessor as before.

Other realism touches:

- Themed seed data (`@mato.int` emails, departments `intel-analysis` / `logistics-corps` / `command-staff`, `mato-special-ops` for the SQL-injection target, `OPERATION ZIRCON` for the classified document title).
- Added springdoc OpenAPI / Swagger UI on archive-service and command-control. Tried adding it on the gateway too, but `springdoc-openapi-starter-webflux-ui` collides with Spring Cloud Gateway's actuator route registration (the `/actuator/gateway/routes` endpoint disappears) — pulled the dep back out.
- New docs: `docs/ARCHITECTURE.md` (service map + trust model + config surface) and `docs/EXPLOIT-FLOWS.md` (one Mermaid sequence diagram per flag with the actual HTTP requests and the bug explanation). Both render natively in IntelliJ markdown preview / GitHub.

### Test posture after rename

All 10 non-Testcontainers tests still pass:

| Module          | Tests                                                                                                                                                    |
| --------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| edge-gateway    | 3/3 — actuator/gateway/routes, /actuator/env, audit-trace flag is SpEL-writable                                                                          |
| archive-service | 2/2 — `Cve202622730ConverterTest` (the SQL-injection surface in the converter); `IntegrationFlagsTest` skipped (Testcontainers/Docker-Desktop-29 compat) |
| command-control | 6/6 — timing leak gap (median ~100ms vs ~2ms), OTP rejects, OTP+JWT round-trip + Flag 4, /warheads forged-token rejection                                |

Deleted code that was sometimes `dev`-confusing: the unused `_post_route("unlock", UNLOCK_PAYLOAD)` step in `solve_flag1.py` was already removed in the previous commit.
