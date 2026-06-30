# DPP SDK Demo

Partner-facing Java backend demo showing how `com.example.dppsdk:dpp4fun:0.3.0` and the split `dpp-sdk-clients` modules work together.

This phase is draft-prEN-18222-aligned. It is not a claim of final EN 18222 compliance or production readiness.

Use this file as the single setup and operations reference for building and running the demo. Use `DEMO_GUIDE.md` only as the live-demo script, walkthrough, and talking-points guide.

Monorepo note:

- If you are starting from the `Dpp-SDK` monorepo root, use the root `README.md` first for the full repository structure and top-level build commands.
- If you are starting from the `Dpp-SDK` monorepo root, prefer the root `mvnw` or `mvnw.cmd` as the canonical wrapper.
- The commands below are scoped to the `dpp-sdk-demo` subproject unless noted otherwise.

## Purpose

This `dpp-sdk-demo` subproject exists to show the reuse boundary between the SDK, the HTTP client library, and a small partner-facing backend demo:

- `dpp4fun` owns the `Dpp4Fun` product aggregate, furniture-specific builders/model classes, mapper support, validation, and `Dpp4FunJsonCodec`.
- `dpp-core` owns reusable core DPP models, validators, payload mapping, and shared utilities used transitively by `dpp4fun`.
- `dpp-sdk-clients` owns shared repo/registry payload contracts plus the low-level HTTP clients for the standard-style DPP APIs.
- This repo adds only Dpp4Fun demo fixtures, thin SDK adapters, mock services, runners, mock backend adapters, internal mock lookup DTOs, tests, and Postman collections.

Runtime truth note:

- The canonical runtime view for this demo is the `dpp-sdk-demo` subproject plus the locally installed `dpp4fun`, `dpp-core`, and `dpp-sdk-clients` artifacts used at build/run time.
- Treat Maven-resolved classes as the source of truth.

## Modules

- `mock-eu-registry`: mock registry HTTP service, default host URL `http://localhost:8081`.
- `mock-dpp-repo`: mock repo HTTP service, default host URL `http://localhost:8080`.
- `dpp-integration-demo`: command-line SDK and HTTP demo runner, including the demo DPP factory, thin SDK-to-client adapters, and the mock-only registry read-back helper used for internal visibility.

The integration demo is the piece that connects the other two parts of the project. It creates and validates demo DPPs, talks to the repo and registry mock APIs, and gives you one repeatable flow for local validation, Docker validation, and partner demos.

## Prerequisites

- Recommended JDK: Java 17.
- The demo build targets Java 17. Newer JDKs may work, but Java 17 is the supported baseline for development and validation.

If you are working inside this monorepo, build from the repo root first so the reactor installs the upstream SDK and client artifacts in the correct order.

Windows:

```powershell
.\mvnw.cmd -f dpp-datamodel/pom.xml clean install
.\mvnw.cmd -f dpp-sdk-clients/pom.xml clean install
```

Linux/MacOS:

```bash
./mvnw -f dpp-datamodel/pom.xml clean install
./mvnw -f dpp-sdk-clients/pom.xml clean install
```

If you are using `dpp-sdk-demo` outside this monorepo, you still need compatible `dpp-datamodel` and `dpp-sdk-clients` artifacts installed in your local Maven repository first.

## Configuration

Optional `dpp-sdk-demo/.env`:

```properties
MOCK_REPO_PORT=8080
MOCK_REGISTRY_PORT=8081
DPP_REPO_PORT=8080
DPP_REGISTRY_PORT=8081
DPP_IMAGE_REGISTRY=container-registry.gitlab.cc-asp.fraunhofer.de/digital-product-passport-sdk/dpp-sdk-demo
DPP_IMAGE_TAG=0.1.0
DPP_REPO_IMAGE_NAME=mock-dpp-repo
DPP_REGISTRY_IMAGE_NAME=mock-eu-registry

# Default mock-service backends. Memory remains the default for local app runs.
DPP_REPO_BACKEND=memory
DPP_REGISTRY_BACKEND=memory

# Mock repo PostgreSQL settings.
MOCK_REPO_POSTGRES_PORT=5433
MOCK_REPO_POSTGRES_DB=dpp_repo
MOCK_REPO_POSTGRES_USER=dpp_repo
MOCK_REPO_POSTGRES_PASSWORD=dpp_repo

# Mock registry PostgreSQL settings.
MOCK_REGISTRY_POSTGRES_PORT=5434
MOCK_REGISTRY_POSTGRES_DB=dpp_registry
MOCK_REGISTRY_POSTGRES_USER=dpp_registry
MOCK_REGISTRY_POSTGRES_PASSWORD=dpp_registry
```

Current default:

- The application defaults remain memory mode for both `mock-dpp-repo` and `mock-eu-registry`.
- Memory mode does not need PostgreSQL configuration.
- PostgreSQL is used only when `DPP_REPO_BACKEND=postgres` or `DPP_REGISTRY_BACKEND=postgres` is set for the relevant service.

Backend behavior:

- `DPP_REPO_BACKEND=memory`: process-local in-memory storage, simplest demo and development mode.
- `DPP_REPO_BACKEND=postgres`: durable PostgreSQL-backed storage through the standalone `dpp4fun-postgres` module.
- `DPP_REGISTRY_BACKEND=memory`: process-local in-memory registry metadata, default demo mode.
- `DPP_REGISTRY_BACKEND=postgres`: durable PostgreSQL-backed registry metadata using the local mock-registry backend seam.
- HTTP paths and response shapes stay the same in both modes.
- The repo service keeps JSON mapping, validation, JSON Merge Patch, and fine-granular element logic in the mock service layer.
- The registry service keeps request validation, duplicate checks, repo `HEAD` verification, and response mapping in the mock service layer.

Environment loading:

- Docker Compose reads `dpp-sdk-demo/.env` automatically when you run Compose from `dpp-sdk-demo`, or when you pass `--env-file dpp-sdk-demo/.env` from the monorepo root.
- `mock-dpp-repo` and `mock-eu-registry` both import `optional:file:.env[.properties]`, so local JAR runs read `dpp-sdk-demo/.env` when you start them from inside `dpp-sdk-demo`.
- For IDE or Maven runs, either run from `dpp-sdk-demo`, configure the same environment variables in the IDE, or pass equivalent Spring properties.
- `.env.example` shows a Docker/PostgreSQL-oriented sample configuration with separate repo and registry database settings.

## Build

Change into `dpp-sdk-demo` before running the wrapper commands below:

Windows:

```powershell
# Run from dpp-sdk-demo
.\mvnw.cmd clean package
```

Linux/MacOS:

```bash
# Run from dpp-sdk-demo
./mvnw clean package
```

Common verification commands:

```powershell
# Windows
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
.\mvnw.cmd clean package
.\mvnw.cmd -pl mock-dpp-repo -am test
.\mvnw.cmd -pl mock-eu-registry -am test
```

```bash
# Linux/MacOS
./mvnw clean test
./mvnw clean verify
./mvnw clean package
./mvnw -pl mock-dpp-repo -am test
./mvnw -pl mock-eu-registry -am test
```

Wrapper note:

- The Maven wrapper jar is committed in this repo.
- On a machine that does not already have the Maven 3.9.11 distribution cached, the first wrapper run may download it from Maven Central.
- The PostgreSQL integration tests use Testcontainers, so Docker must be available for the Docker-backed registry and repo tests to execute instead of being skipped by `@Testcontainers(disabledWithoutDocker = true)`.

## Run Options

Both mock services have two storage modes:

- Memory mode: no PostgreSQL server is needed. This is the default and the simplest mode for demos and development.
- PostgreSQL mode: each service connects to its own PostgreSQL server/database. This is demo persistence only, not production compliance or security.

Use these quick choices:

- Mode 1, memory-only demo: run the JARs normally with `DPP_REPO_BACKEND=memory` and `DPP_REGISTRY_BACKEND=memory`, or omit both settings.
- Mode 2, full Docker Compose with PostgreSQL: run `docker compose up --build` from `dpp-sdk-demo`; Compose starts `mock-dpp-repo`, `mock-eu-registry`, `mock-repo-postgres`, and `mock-registry-postgres`.
- Mode 3, PostgreSQL containers only: start only `mock-repo-postgres` and `mock-registry-postgres`, then run the app JARs locally against `localhost`.

Connection names:

- From inside Docker Compose, use `jdbc:postgresql://mock-repo-postgres:5432/dpp_repo` for the repo and `jdbc:postgresql://mock-registry-postgres:5432/dpp_registry` for the registry.
- From a local JAR, IDE, Postman helper, or host-side tool, use `jdbc:postgresql://localhost:5433/dpp_repo` for the repo and `jdbc:postgresql://localhost:5434/dpp_registry` for the registry.

### 1. Run Locally With Java

Build first, then start the services in this order before the default standards demo flow. Run these commands from `dpp-sdk-demo` so the optional subproject `.env` is picked up.

Start the registry:

```powershell
java -jar mock-eu-registry\target\mock-eu-registry-1.0.0-SNAPSHOT-exec.jar --debug=false # Windows
```

```bash
java -jar mock-eu-registry/target/mock-eu-registry-1.0.0-SNAPSHOT-exec.jar --debug=false # Linux/MacOS
```

Start the repo:

```powershell
java -jar mock-dpp-repo\target\mock-dpp-repo-1.0.0-SNAPSHOT-exec.jar --debug=false # Windows
```

```bash
java -jar mock-dpp-repo/target/mock-dpp-repo-1.0.0-SNAPSHOT-exec.jar --debug=false # Linux/MacOS
```

Use this when you want the two mock services running directly on the host with the configured `.env` ports.

Memory mode is the default:

```properties
DPP_REPO_BACKEND=memory
DPP_REGISTRY_BACKEND=memory
MOCK_REPO_PORT=8080
MOCK_REGISTRY_PORT=8081
```

You can omit both backend variables and get the same memory-mode behavior.

For PostgreSQL mode with the apps running locally, start both PostgreSQL containers first:

```powershell
docker compose up mock-repo-postgres mock-registry-postgres
```

Then run `mock-dpp-repo` locally with host-side datasource values:

```properties
DPP_REPO_BACKEND=postgres
MOCK_REPO_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/dpp_repo
SPRING_DATASOURCE_USERNAME=dpp_repo
SPRING_DATASOURCE_PASSWORD=dpp_repo
```

Run `mock-eu-registry` locally with its own datasource values:

```properties
DPP_REGISTRY_BACKEND=postgres
MOCK_REGISTRY_PORT=8081
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/dpp_registry
SPRING_DATASOURCE_USERNAME=dpp_registry
SPRING_DATASOURCE_PASSWORD=dpp_registry
```

### 2. Run Local Containers From Locally Built Images

This is the maintainer workflow when you want Dockerized services built from the current checkout.

```powershell
.\mvnw.cmd clean package
docker compose up --build
```

What this does:

- Builds the executable service JARs locally with Maven.
- Builds the registry-qualified repo and registry images from those packaged JARs.
- Starts `mock-dpp-repo`, `mock-eu-registry`, `mock-repo-postgres`, and `mock-registry-postgres`.
- Connects each mock service to its own PostgreSQL server/database.

Important:

- `docker compose build` builds images only.
- `docker compose up` or `up -d` runs containers.
- `up --build` does both.

### 3. Publish Images

Use this when you want to push the current repo and registry images to the GitLab project container registry.

```powershell
.\mvnw.cmd clean package
docker compose -f docker-compose.build.yml build
docker login container-registry.gitlab.cc-asp.fraunhofer.de
docker compose -f docker-compose.build.yml push
```

With the committed `.env`, the pushed image names are:

- `container-registry.gitlab.cc-asp.fraunhofer.de/digital-product-passport-sdk/dpp-sdk-demo/mock-dpp-repo:0.1.0`
- `container-registry.gitlab.cc-asp.fraunhofer.de/digital-product-passport-sdk/dpp-sdk-demo/mock-eu-registry:0.1.0`

If you want to test a different tag, override `DPP_IMAGE_TAG` before both push and pull. The pushed tag and pulled tag must match.

### 4. Run Containers From Already Published Images

Use this when the images already exist in the GitLab registry and you want a pull-only workflow.

```powershell
docker login container-registry.gitlab.cc-asp.fraunhofer.de
docker compose pull
docker compose up -d
docker compose ps
```

If you want to run a different published tag without editing `.env`, override it for the current shell first:

```powershell
$env:DPP_IMAGE_TAG="test-1"
docker compose pull
docker compose up -d
```

This is also how you pull an already existing image tag for a clean retest on another machine.

Published-image PostgreSQL mode uses the default Compose file:

```powershell
docker login container-registry.gitlab.cc-asp.fraunhofer.de
docker compose pull
docker compose up -d
docker compose ps
```

This starts the published mock repo and registry images plus both PostgreSQL containers.

## Validate / Test

After startup, you can validate the running mocks in three ways:

- Run the integration demo.
- Run the Postman collections.
- Use the repo and registry services directly as lightweight mock/test doubles for local development and manual API testing.

The detailed walkthrough, what each step demonstrates, and the presenter talking points belong in `DEMO_GUIDE.md`. Keep this README as the source of truth for commands, collection names, and runtime configuration.

### Integration Demo

With registry and repo already running:

```powershell
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar --debug=false # Windows
```

```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar --debug=false # Linux/MacOS
```

The default run is the standards end-to-end flow. For a fuller stakeholder demo, run SDK capability checks first and then reuse those same SDK-built DPPs in the HTTP flow:

```powershell
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar all --debug=false # Windows
```

```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar all --debug=false # Linux/MacOS
```

SDK-only mode does not require the backend services:

```powershell
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar sdk --debug=false # Windows
```

```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar sdk --debug=false # Linux/MacOS
```

Explicit HTTP-only mode:

```powershell
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar http --debug=false # Windows
```

```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar http --debug=false # Linux/MacOS
```

Optional base URL overrides:

```powershell
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar http http://localhost:8091 http://localhost:8090 --debug=false # Windows
```

```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar http http://localhost:8091 http://localhost:8090 --debug=false # Linux/MacOS
```

Default URL resolution for the HTTP demo runner:

- If you do not pass explicit service URLs, the runner checks Docker-style service names first:
  - Registry: `http://mock-eu-registry:${DPP_REGISTRY_PORT}`
  - Repo: `http://mock-dpp-repo:${DPP_REPO_PORT}`
- If those health checks fail, it falls back to:
  - Registry: `http://localhost:${DPP_REGISTRY_PORT}`
  - Repo: `http://localhost:${DPP_REPO_PORT}`
- If neither candidate is reachable for a service, the runner throws an exception and exits before the demo flow starts.

### Postman

Import:

- `postman/dpp-lifecycle-api.verified-export-shape.postman_collection.json`
- `postman/dpp-registry-api.verified-export-shape.postman_collection.json`
- `postman/dpp-fine-granular-api.import-safe.postman_collection.json`

Base URLs:

- Registry: `http://localhost:${DPP_REGISTRY_PORT}` or default `http://localhost:8081`
- Repo: `http://localhost:${DPP_REPO_PORT}` or default `http://localhost:8080`

Postman does not read `dpp-sdk-demo/.env` automatically. If you changed ports in `.env`, update the Postman collection variables to match.

For the actual request-by-request explanation and collection flow, use `DEMO_GUIDE.md`. Keep the import names and base URL values in this README as the operational reference.

The collections now use isolated default demo identifiers for ad-hoc requests, and their cleanup delete requests use separate delete-only defaults until a create request overwrites them with a fresh runtime DPP. That keeps Postman, Swagger UI, and the HTTP demo runner from deleting each other's default records.

### Quick Smoke Checks

Windows:

```powershell
curl.exe http://localhost:8080/health
curl.exe http://localhost:8081/health
```

Linux/MacOS:

```bash
curl http://localhost:8080/health
curl http://localhost:8081/health
```

### Swagger UI / OpenAPI

When the mock services are running, Swagger UI can be used to inspect the APIs and send test requests directly to the local mock services.

- Mock repo Swagger UI: `http://localhost:8080/swagger-ui.html`
- Mock repo OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Mock registry Swagger UI: `http://localhost:8081/swagger-ui.html`
- Mock registry OpenAPI JSON: `http://localhost:8081/v3/api-docs`

Swagger UI exposes the repo lifecycle, fine-granular, and event endpoints, plus the registry registration and metadata lookup endpoints.

## Runtime Notes

From the host machine, Postman, or a browser:

- Repo: `http://localhost:${DPP_REPO_PORT}`
- Registry: `http://localhost:${DPP_REGISTRY_PORT}`

From the registry container to the repo container:

- Use `http://mock-dpp-repo:${DPP_REPO_PORT}`
- Do not use `http://localhost:${DPP_REPO_PORT}`

Why:

- `localhost` inside the `mock-eu-registry` container means the registry container itself, not the host and not the repo container.
- The registry handles that internally for the standard public repo URL path, but the network distinction still matters when you debug requests manually.

Compose file roles:

- `docker-compose.yml`: default build/pull/run workflow for the demo services plus `mock-repo-postgres` and `mock-registry-postgres`.
- `docker-compose.build.yml`: compatibility copy for existing local image build/push workflows.
- `docker-compose.postgres.yml`: compatibility override that still exposes the dedicated PostgreSQL service names and app wiring.

## Mock Boundary

This repo provides demo-focused mock services and a producer/demo runner. It is intentionally not production-ready.

Mocked:

- EU registry behavior
- DPP repo behavior
- Persistence, using default in-memory storage or optional PostgreSQL-backed repo and registry adapters

Not implemented:

- Real EU registry integration
- Production-grade database hardening or compliance persistence
- Authentication or OAuth
- Event streaming
- Production resilience, retries, auditing, or monitoring
- AAS adapter
- Backup identifier/operator/provider placeholders
- Full official draft data-model field coverage
