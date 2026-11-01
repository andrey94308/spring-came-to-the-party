.PHONY: up down build test-java test-local demo-reset clean logs
SERVICES := edge-gateway archive-service

up:
	docker compose up -d --build --wait

down:
	docker compose down

build:
	docker compose build

test-java:
	@for d in $(SERVICES); do (cd services/$$d && mvn -B -q test) || exit 1; done

test-local: up
	python3 postman/check_demo.py

demo-reset:
	docker compose down -v --remove-orphans
	docker compose up -d --build --wait

clean:
	docker compose down -v --remove-orphans
	@for d in $(SERVICES); do (cd services/$$d && mvn -B -q clean) || exit 1; done

logs:
	docker compose logs -f
