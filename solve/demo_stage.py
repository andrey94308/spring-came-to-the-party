#!/usr/bin/env python3
"""Run one presentation experiment and persist its result for the next one."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from common import DEFAULT_COMMAND, DEFAULT_GATEWAY, die
import solve_flag1
import solve_flag2
import solve_flag3
import solve_flag4


STATE_FILE = Path("/state/demo.json")


def load_state() -> dict:
    if not STATE_FILE.exists():
        return {}
    return json.loads(STATE_FILE.read_text(encoding="utf-8"))


def save_state(state: dict) -> None:
    STATE_FILE.parent.mkdir(parents=True, exist_ok=True)
    STATE_FILE.write_text(json.dumps(state, indent=2) + "\n", encoding="utf-8")
    print(f"\nSaved demo state: {STATE_FILE}")


def require(state: dict, *keys: str) -> None:
    missing = [key for key in keys if key not in state]
    if missing:
        die(f"missing saved data: {', '.join(missing)}. Run the previous demo stage first.")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("stage", choices=("1", "2", "3", "4"))
    args = parser.parse_args()
    state = load_state()

    if args.stage == "1":
        flag, users = solve_flag1.run(DEFAULT_GATEWAY)
        state.update(flag1=flag, users=users)
    elif args.stage == "2":
        require(state, "users")
        flag, username, password = solve_flag2.run(DEFAULT_GATEWAY, users=state["users"])
        state.update(flag2=flag, username=username, password=password)
    elif args.stage == "3":
        require(state, "username", "password")
        flag, president_password, command_url = solve_flag3.run(
            DEFAULT_GATEWAY, username=state["username"], password=state["password"]
        )
        state.update(flag3=flag, president_password=president_password, command_url=command_url)
    else:
        command_url = state.get("command_url", DEFAULT_COMMAND)
        for attempt in range(2):
            try:
                flag = solve_flag4.run(command_url)
                break
            except SystemExit:
                if attempt == 1:
                    raise
                print("OTP window changed during the first attempt; retrying once.")
        state["flag4"] = flag

    save_state(state)


if __name__ == "__main__":
    main()