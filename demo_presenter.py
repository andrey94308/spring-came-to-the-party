#!/usr/bin/env python3
"""Pause between the four local, intentionally vulnerable demo stages."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path


def wait_for_slide(slide: int, label: str) -> None:
    input(f"\n[Slide {slide}] {label}\nPress Enter to run this live demo … ")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True, help="Path to the demo repository")
    args = parser.parse_args()
    repo = args.repo.resolve()
    solve = repo / "solve"
    if not (solve / "solve_flag1.py").exists():
        parser.error(f"No solve scripts found in {solve}")

    sys.path.insert(0, str(solve))
    import requests  # type: ignore
    from common import basic_auth_header, read_mcp_entry_point  # type: ignore
    import solve_flag1  # type: ignore
    import solve_flag2  # type: ignore
    import solve_flag3  # type: ignore
    import solve_flag4  # type: ignore

    gateway = read_mcp_entry_point()
    print(f"Demo target: {gateway}")

    wait_for_slide(11, "Gateway SpEL and changed /users response")
    baseline = requests.get(f"{gateway}/users", timeout=10)
    print(f"Before: GET /users -> HTTP {baseline.status_code}")
    if baseline.ok:
        body = baseline.json()
        print(f"mode={body.get('mode')}, first user keys={list(body.get('users', [{}])[0])}")
    flag1, users = solve_flag1.run(gateway)
    print(f"Result: {flag1}; {len(users)} user records")

    wait_for_slide(13, "Offline wordlist and authenticated /notes")
    flag2, username, password = solve_flag2.run(gateway, users=users)
    print(f"Result: {flag2}; authenticated as {username}")

    wait_for_slide(14, "Normal document filter versus SQL injection")
    normal = requests.post(
        f"{gateway}/documents/search",
        headers=basic_auth_header(username, password),
        json={"query": "anything", "department": "intel-analysis"},
        timeout=10,
    )
    print(f"Normal search: HTTP {normal.status_code}")
    if normal.ok:
        print(f"Normal search returned {len(normal.json())} document(s)")
    flag3, _president_password, command_url = solve_flag3.run(
        gateway, username=username, password=password
    )
    print(f"Result: {flag3}; next service: {command_url}")

    wait_for_slide(15, "Login timing, OTP attempts, JWT and final response")
    flag4 = solve_flag4.run(command_url)
    print(f"Result: {flag4}")
    print("\nAll four demo stages completed. Return to Slide 16.")


if __name__ == "__main__":
    main()
