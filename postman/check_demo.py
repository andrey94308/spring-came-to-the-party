"""Check the four HTTP outcomes used by the Postman presentation."""

import base64
import json
import urllib.request


BASE = "http://localhost:8080"
AUTH = base64.b64encode(b"bob:sunshine").decode("ascii")


def search(path: str, department: str) -> list[dict]:
    request = urllib.request.Request(
        f"{BASE}/documents/{path}",
        data=json.dumps({"department": department}).encode("utf-8"),
        headers={"Authorization": f"Basic {AUTH}", "Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        assert response.status == 200
        return json.load(response)


def departments(documents: list[dict]) -> set[str]:
    return {json.loads(document["metadata"])["department"] for document in documents}


normal = search("search", "intel-analysis")
other = search("search", "mato-special-ops")
injected = search("search", "intel-analysis' OR '1'='1")
safe = search("search-safe", "intel-analysis' OR '1'='1")

assert departments(normal) == {"intel-analysis"}, departments(normal)
assert other == [], departments(other)
assert "mato-special-ops" in departments(injected), departments(injected)
assert safe == [], departments(safe)

for name, documents in [("normal", normal), ("other", other), ("injected", injected), ("safe", safe)]:
    print(f"{name}: {len(documents)} documents; departments={sorted(departments(documents))}")
