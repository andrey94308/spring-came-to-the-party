.PHONY: up down build run test-java test-local demo-reset clean logs

up:
	docker compose up -d --wait

down:
	docker compose down

build:
	mvn -B -q -f services/archive-service/pom.xml package -DskipTests

run: up
	mvn -B -q -f services/archive-service/pom.xml spring-boot:run

test-java:
	mvn -B -q -f services/archive-service/pom.xml test

test-local: up
	@curl -fsS http://localhost:8080/v3/api-docs >/dev/null || { echo "Start ArchiveApplication in IntelliJ or run 'make run' first"; exit 1; }
	python3 postman/check_demo.py

demo-reset:
	docker compose down -v --remove-orphans
	docker compose up -d --wait

clean:
	docker compose down -v --remove-orphans
	mvn -B -q -f services/archive-service/pom.xml clean

logs:
	docker compose logs -f
