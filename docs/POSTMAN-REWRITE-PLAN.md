# Knowledge sharing rewrite: Postman-first demo

Status: proposed implementation plan (30 October 2026). The existing 18-slide deck and solver remain reference material until the new demo is verified.

## Goal

Show a deliberately vulnerable Spring application as a controlled experiment. The audience sees a normal request, predicts the response to one changed input, then compares the actual response and the responsible code. Do not present the four flags as a necessary attack chain.

## Main live experiment: document search and SQL injection

This is the strongest candidate for a short interactive demo because it begins with a familiar feature: an authenticated employee searches documents in their department. It uses a real vulnerable Spring AI converter version, but the surrounding application must enforce a meaningful access rule first.

1. **Normal baseline in Postman.** Authenticate as `bob`; search for `intel-analysis`; show only documents that `bob` may read.
2. **Audience question.** Change `department` to `mato-special-ops`. The expected response must be empty or forbidden because `bob` is not entitled to that department.
3. **Input change.** Send the SQL injection value through the same field. Show the extra document returned because the converter emits unescaped SQL and the application incorporates that fragment into the query.
4. **Explain the boundary.** Compare the request field, converted WHERE fragment, and returned rows. Identify the real CVE separately from the application's responsibility to keep authorization independent of user input.
5. **Show the fix.** Run the same requests against a fixed path or patched version; demonstrate that the unauthorized document stays inaccessible.

Implementation requirement: first add server-side authorization for document departments so a plain `mato-special-ops` request is denied. Then verify that the chosen injected value still bypasses the vulnerable query. Do not rely on the mere presence of an SQLi library to imply that this particular access control is bypassed. Keep the normal and vulnerable behavior testable through HTTP, not only unit tests of the converter.

## Optional second experiment: authentication flow

Use `command-control` only as a separate example if time allows. A normal flow should require successful password login before OTP verification, and rate limiting should be demonstrable. The current endpoint issues a JWT from OTP alone; that is a custom application flaw, not a continuation of the SQLi scenario. The current custom login timing leak should not be labelled a reproduction of CVE-2025-22234.

## Gateway experiment

Keep CVE-2025-41243 as an appendix or optional technical deep dive. The current `X-Debug-Trace` header can be spoofed by the client, so the CVE does not cause a necessary transition. If this experiment stays, strip inbound trusted headers at the gateway and replace the hash-leaking debug response with a more credible effect. Explain that the publicly writable Actuator is an intentionally unsafe prerequisite, not normal practice.

## Postman deliverables

- Importable collection with named requests: baseline, unauthorized department, injected value, and fixed behavior.
- Environment with local URLs and demo credentials, with no production secrets.
- Short presenter runbook with reset steps, expected response fields, and a fallback screenshot for live-demo failure.
- Only 3–5 supporting slides: intent and caveat; service/data map; the request-to-SQL path; observed result and fix; discussion.

## Acceptance checks before writing the new deck

- A normal user cannot retrieve the special document by merely naming its department.
- The selected exploit request demonstrates the actual vulnerable converter path and a security impact not available through the normal request.
- The fixed behavior blocks the same request.
- Postman collection works from a clean local startup and its output matches the runbook.
- Each demo claim clearly distinguishes a dependency CVE, an application bug, and an intentionally unsafe lab condition.
