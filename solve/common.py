"""Shared helpers for the four solve scripts.

The solve scripts are deliberately self-contained — a player should be able
to run any one of them on its own and see the corresponding flag printed.
"""

from __future__ import annotations

import base64
import json
import os
import re
import sys
from pathlib import Path

import requests

# Default targets when running locally via docker-compose.
DEFAULT_GATEWAY = os.environ.get("GATEWAY_URL", "http://localhost:8080")
DEFAULT_COMMAND = os.environ.get("COMMAND_URL", "http://localhost:8082")

REPO_ROOT = Path(__file__).resolve().parent.parent
WORDLIST_PATH = Path(__file__).resolve().parent / "wordlist.txt"


def banner(stage: str, msg: str) -> None:
    print(f"[{stage}] {msg}", flush=True)


def section(stage: str, title: str) -> None:
    """Print a visually-separated section header for a major step."""
    bar = "─" * max(4, 60 - len(title) - len(stage) - 5)
    print(f"\n[{stage}] ── {title} {bar}", flush=True)


def detail(stage: str, label: str, value: object) -> None:
    """Print a labeled debug value, indented under its section."""
    text = str(value)
    if len(text) > 240:
        text = text[:237] + "..."
    print(f"[{stage}]    • {label}: {text}", flush=True)


def dump(stage: str, label: str, value: object) -> None:
    """Print a full, untruncated payload inside a fenced block."""
    if isinstance(value, (dict, list)):
        body = json.dumps(value, indent=2, ensure_ascii=False)
    else:
        body = str(value)
    bar = "─" * 60
    print(f"[{stage}]    ┌─ {label} {bar[:max(4, 60 - len(label) - 5)]}", flush=True)
    for line in body.splitlines() or [""]:
        print(f"[{stage}]    │ {line}", flush=True)
    print(f"[{stage}]    └{bar}", flush=True)


def read_mcp_entry_point() -> str:
    """Read the URL out of the .mcp.json the President's assistant leaked."""
    configured_url = os.environ.get("GATEWAY_URL")
    if configured_url:
        return configured_url
    mcp = json.loads((REPO_ROOT / ".mcp.json").read_text())
    return mcp["mcpServers"]["presidential-archive"]["url"]


def find_flag(haystack: str) -> str | None:
    """Return the first FLAG{...} or NUKE{...} substring."""
    m = re.search(r"(?:FLAG|NUKE)\{[^}]+\}", haystack)
    return m.group(0) if m else None


def basic_auth_header(user: str, password: str) -> dict[str, str]:
    encoded = base64.b64encode(f"{user}:{password}".encode()).decode()
    return {"Authorization": f"Basic {encoded}"}


def bearer(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def die(msg: str, code: int = 1) -> None:
    print(f"FATAL: {msg}", file=sys.stderr)
    sys.exit(code)
