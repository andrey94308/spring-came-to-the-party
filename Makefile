.PHONY: clean build test-local demo demo-reset demo-1 demo-2 demo-3 demo-4 up down logs test-java

SERVICES := edge-gateway archive-service command-control

# Stop containers, remove volumes, clean Maven build output.
clean:
	docker compose down -v --remove-orphans || true
	@for d in $(SERVICES); do \
		if [ -f services/$$d/pom.xml ]; then \
			(cd services/$$d && mvn -B -q clean) || exit 1; \
		fi; \
	done

# Build all Docker images.
build:
	docker compose build

# Run the per-module Java unit / integration tests.
test-java:
	@for d in $(SERVICES); do \
		echo "=== mvn test :: $$d ==="; \
		(cd services/$$d && mvn -B -q test) || exit 1; \
	done

# Bring up the stack and run the end-to-end solve script. Asserts all 4 flags.
test-local: build
	docker compose up -d --wait
	docker compose --profile demo build demo-runner
	docker compose --profile demo run --rm demo-runner python solve/solve_all.py
	docker compose down

# Run the interactive presentation flow against an already-running stack.
demo:
	docker compose --profile demo build demo-runner
	docker compose --profile demo run --rm demo-runner

# Reset the gateway and the saved data before a new presentation.
demo-reset:
	docker compose up -d --wait
	rm -rf demo-state

# Run exactly one experiment. Each stage saves data for the following one.
demo-1:
	docker compose --profile demo run --rm demo-runner python solve/demo_stage.py 1

demo-2:
	docker compose --profile demo run --rm demo-runner python solve/demo_stage.py 2

demo-3:
	docker compose --profile demo run --rm demo-runner python solve/demo_stage.py 3

demo-4:
	docker compose --profile demo run --rm demo-runner python solve/demo_stage.py 4

up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f
