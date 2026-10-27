# Spring Came to the Party

A deliberately vulnerable Spring demo for security knowledge sharing.

## Scenario

The President's AI assistant has accidentally leaked some classified material into a public repository. Your job is to follow the trail, escalate through MATO's network, and recover the nuclear launch codes before someone less friendly does.

There are **four flags**. Find them all.

## Getting Started

```bash
make build
make test-local
```

You will need:

- Docker (with `docker compose` v2)
- `make`

Python is included in the `demo-runner` container and is not required on the host.

## Make Targets

| Target | What it does |
|--------|--------------|
| `make clean`       | Stop containers, remove volumes, clean Maven build output |
| `make build`       | Build all Docker images |
| `make test-java`   | Run per-module Java unit/integration tests |
| `make test-local`  | Bring up the stack and run the end-to-end solve script |
| `make demo`        | Run the interactive presentation flow against the running stack |

On Windows, the same demo can be run without `make`:

```bash
docker compose up -d --build --wait
docker compose --profile demo build demo-runner
docker compose --profile demo run --rm demo-runner
```

For a non-interactive check of all four stages, replace the last command with:

```bash
docker compose --profile demo run --rm demo-runner python solve/solve_all.py
```

## Stack

- Java 21 + Spring Boot 3.4.5 (Maven)
- Spring Cloud Gateway 4.2.0 (CVE-2025-41243)
- Spring Security 6.4.4 (CVE-2025-22234)
- Spring AI MariaDB Store 1.0.3 (CVE-2026-22730)
- MariaDB 11.4
- Docker Compose

## Repository Layout

```
.
├── services/
│   ├── edge-gateway/        # Public-facing Spring Cloud Gateway (port 8080)
│   ├── archive-service/     # /users, /notes, /documents/search (port 8081, internal)
│   └── command-control/     # /login, /verify-otp, /warheads (port 8082)
├── infra/
│   └── mariadb/             # MariaDB init SQL
├── solve/                   # Python solve scripts (one per flag, plus solve_all.py)
└── docs/                    # Architecture, exploit-flow diagrams, working notes
```

## Browse the APIs

Once the stack is running (`make up`):

| Service | OpenAPI | Swagger UI |
|---|---|---|
| edge-gateway     | <http://localhost:8080/v3/api-docs> | <http://localhost:8080/swagger-ui.html> |
| command-control  | <http://localhost:8082/v3/api-docs> | <http://localhost:8082/swagger-ui.html> |

The archive-service is internal-only on the docker network; players reach it through the edge-gateway.

## Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — service map, inter-service contracts, trust model.
- [`docs/EXPLOIT-FLOWS.md`](docs/EXPLOIT-FLOWS.md) — sequence diagram per flag.
- [`docs/notes.md`](docs/notes.md) — working journal with implementation notes for the final report.

