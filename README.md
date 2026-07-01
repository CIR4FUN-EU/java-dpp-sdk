# DPP SDK

![](docs/dpp-sdk-architecture.drawio.png)

## Overview

This monorepo contains:

- The DPP data model and SDK modules implemented in Java
- Demo and mock services for DPP repository and DPP registry for end-to-end interaction
- HTTP client modules for DPP-Repository and DPP-Registry
- Demo runtime
- The implementation follows the drafted standardised API standards specified by the CEN/CENELEC JTC24 committee as of 06/2026

Use this root README as the quick entry point. For module-specific details, use the README and docs inside each subproject.

## Repository Structure

```text
.
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
|-- .mvn/
|   `-- wrapper/
|-- dpp-datamodel/
|   |-- pom.xml
|   |-- mvnw
|   |-- mvnw.cmd
|   |-- dpp-core/
|   `-- dpp4fun/
|-- dpp-sdk-clients/
|   |-- pom.xml
|   |-- mvnw
|   |-- mvnw.cmd
|   |-- dpp-repo-payloads/
|   |-- dpp-repo-client/
|   |-- dpp-registry-payloads/
|   `-- dpp-registry-client/
`-- dpp-sdk-demo/
    |-- pom.xml
    |-- .env
    |-- docker-compose.yml
    |-- mvnw
    |-- mvnw.cmd
    |-- mock-dpp-repo/
    |-- mock-eu-registry/
    `-- dpp-integration-demo/
```

## Project Responsibilities

- `dpp-datamodel`: DPP domain model, validation, mapping, and JSON transport.
- `dpp-sdk-clients`: generic repository and registry HTTP clients plus API payload contracts.
- `dpp-sdk-demo`: mock repository, mock registry, integration demo, Docker runtime, Postman collections, and demo guides.

## Artifacts And Dependencies

Consumer-facing artifacts are built in this monorepo and can then be consumed from the local Maven repository after a local `clean install`. The demo subproject is runnable/demo code, not a reusable library dependency.

After building and installing the relevant subprojects locally, use these modules when consuming the monorepo from another project:

- `dpp.datamodel:dpp-core:0.3.0`
- `dpp.datamodel:dpp4fun:0.3.0`
- `dpp.client:dpp-repo-payloads:0.3.0`
- `dpp.client:dpp-repo-client:0.3.0`
- `dpp.client:dpp-registry-payloads:0.3.0`
- `dpp.client:dpp-registry-client:0.3.0`

Typical import choices:

- import `dpp-core` when you need the reusable core DPP domain model and validation types
- import `dpp4fun` when you need the furniture-specific SDK layer on top of `dpp-core`
- import only the specific `dpp-sdk-clients` modules your application needs
- do not depend on `dpp-sdk-demo` as a library; use it as runnable reference/demo code

Local install flow before consumption:

- run `.\mvnw.cmd -f dpp-datamodel/pom.xml clean install` to install `dpp-core` and `dpp4fun` locally
- run `.\mvnw.cmd -f dpp-sdk-clients/pom.xml clean install` to install the client modules locally
- then add the required dependencies in the consuming project

## Prerequisites

- JDK 17 or newer on `PATH`, or set `JAVA_HOME`
- Recommended JDK: Java 17.
- The project targets Java 17. Newer JDKs may work, but Java 17 is the supported baseline for development and validation.
- Docker Desktop or another Docker engine for Docker workflows
- No local Maven install is required when you use the root Maven wrapper
- The Maven wrapper downloads Maven automatically on first use

Current wrapper configuration:

- Maven Wrapper `3.3.4`
- Maven distribution `3.9.11`

## Quick Start: Build and Test Everything

Run from the repository root.

Windows:

```powershell
.\mvnw.cmd --version      # Show the Maven wrapper and Java runtime in use
.\mvnw.cmd clean test     # Rebuild and run the full reactor test suite
.\mvnw.cmd clean package  # Build all modules and create the packaged artifacts
.\mvnw.cmd clean verify   # Run the full verification lifecycle across the reactor
```

Linux/macOS:

```bash
./mvnw --version      # Show the Maven wrapper and Java runtime in use
./mvnw clean test     # Rebuild and run the full reactor test suite
./mvnw clean package  # Build all modules and create the packaged artifacts
./mvnw clean verify   # Run the full verification lifecycle across the reactor
```

## Build Individual Projects

The safest root-level commands for individual subprojects are `-f` builds against each subproject aggregator.

Windows:

```powershell
.\mvnw.cmd -f dpp-datamodel/pom.xml clean install   # Build and install the SDK/data-model artifacts locally
.\mvnw.cmd -f dpp-sdk-clients/pom.xml clean install # Build and install the generic client artifacts locally
.\mvnw.cmd -f dpp-sdk-demo/pom.xml clean package    # Build the demo modules and create the runnable jars
```

Linux/macOS:

```bash
./mvnw -f dpp-datamodel/pom.xml clean install   # Build and install the SDK/data-model artifacts locally
./mvnw -f dpp-sdk-clients/pom.xml clean install # Build and install the generic client artifacts locally
./mvnw -f dpp-sdk-demo/pom.xml clean package    # Build the demo modules and create the runnable jars
```

Reactor build order is:

1. `dpp-datamodel`
2. `dpp-sdk-clients`
3. `dpp-sdk-demo`

Subproject wrappers still exist, but the root wrapper is now the canonical entry point for monorepo builds.

## Run Demo Services as JARs

Package the repo first with the root wrapper or `-f dpp-sdk-demo/pom.xml clean package`.

Windows:

```powershell
java -jar dpp-sdk-demo\mock-eu-registry\target\mock-eu-registry-1.0.0-SNAPSHOT-exec.jar --debug=false     # Start the mock registry service
java -jar dpp-sdk-demo\mock-dpp-repo\target\mock-dpp-repo-1.0.0-SNAPSHOT-exec.jar --debug=false           # Start the mock repo service
java -jar dpp-sdk-demo\dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar http --debug=false # Run the HTTP integration demo flow
```

Linux/macOS:

```bash
java -jar dpp-sdk-demo/mock-eu-registry/target/mock-eu-registry-1.0.0-SNAPSHOT-exec.jar --debug=false     # Start the mock registry service
java -jar dpp-sdk-demo/mock-dpp-repo/target/mock-dpp-repo-1.0.0-SNAPSHOT-exec.jar --debug=false           # Start the mock repo service
java -jar dpp-sdk-demo/dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar http --debug=false # Run the HTTP integration demo flow
```

Additional integration demo modes:

- default run: no mode argument
- `sdk`
- `http`
- `all`

Note:

- The commands above work from the repo root with the default ports `8080` and `8081`.
- If you want `dpp-sdk-demo/.env` to override ports for direct JAR runs, start those processes from inside `dpp-sdk-demo`.

## Run with Docker

The committed Docker config lives under `dpp-sdk-demo` and uses `dpp-sdk-demo/.env` for the local ports.

This workflow requires source code, Java, the root Maven wrapper, and Docker.

Windows:

```powershell
.\mvnw.cmd -f dpp-sdk-demo/pom.xml clean package                                           # Build the demo jars used by the Docker images
docker compose -f dpp-sdk-demo/docker-compose.yml --env-file dpp-sdk-demo/.env up --build  # Build the local images and start the demo services
```

Linux/macOS:

```bash
./mvnw -f dpp-sdk-demo/pom.xml clean package                                           # Build the demo jars used by the Docker images
DOCKER_DEFAULT_PLATFORM=linux/amd64 docker compose -f dpp-sdk-demo/docker-compose.build.yml --env-file dpp-sdk-demo/.env up --build  # Build the local images and start the demo services
```

### Docker networking note

- From the host machine, use `localhost`.
- From one container to another, use the service name `mock-dpp-repo`.
- The registry container must reach the repo at `http://mock-dpp-repo:8080`, not `http://localhost:8080`.

## Useful URLs

- Repo service: `http://localhost:8080`
- Registry service: `http://localhost:8081`
- Repo health: `http://localhost:8080/health`
- Registry health: `http://localhost:8081/health`
- Repo Swagger UI: `http://localhost:8080/swagger-ui.html`
- Repo OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Registry Swagger UI: `http://localhost:8081/swagger-ui.html`
- Registry OpenAPI JSON: `http://localhost:8081/v3/api-docs`

Swagger UI can be used to inspect and send test requests directly to the local mock services when they are running.

## Documentation Map

- `CHANGELOG.md`
- `LICENSE`
- `dpp-datamodel/README.md`
- `dpp-datamodel/DPP_SDK_OVERVIEW.md`
- `dpp-datamodel/SDK_USAGE.md`
- `dpp-datamodel/MODEL_GUIDE.md`
- `dpp-datamodel/VALIDATION_GUIDE.md`
- `dpp-datamodel/VALIDATION_RULES.md`
- `dpp-datamodel/LOCAL_CONSUMPTION.md`
- `dpp-sdk-clients/README.md`
- `dpp-sdk-clients/docs/pren-18222-api-alignment.md`
- `dpp-sdk-clients/dpp-repo-payloads/README.md`
- `dpp-sdk-clients/dpp-repo-client/README.md`
- `dpp-sdk-clients/dpp-registry-payloads/README.md`
- `dpp-sdk-clients/dpp-registry-client/README.md`
- `dpp-sdk-demo/README.md`
- `dpp-sdk-demo/DEMO_GUIDE.md`

## Current Limitations

- This repository is a pre-release reference/demo repository.
- `mock-dpp-repo` and `mock-eu-registry` are mock/demo services, not production services.
- Real persistence, security, and real EU registry integration are not implemented here.

## Detailed Docs

For details, use the README and docs inside each subproject and module. This root README is only the quick entry point.
