# DPP SDK

![](docs/dpp-sdk-architecture.drawio.png)

## Overview

This monorepo contains:

- The DPP data model and SDK modules implemented in Java
- Demo and mock services for DPP repository and DPP registry for end-to-end interaction
- HTTP client modules for DPP-Repository and DPP-Registry
- Demo runtime
- Optional PostgreSQL persistence support for `Dpp4Fun`

Selected mock/client API contracts are partially aligned with EN 18222:2026. This repository does not claim final EN compliance, certification, or legal conformity.

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
|   |-- dpp-core/
|   `-- dpp4fun/
|-- dpp-postgres/
|   |-- pom.xml
|   |-- dpp-postgres-core/
|   `-- dpp4fun-postgres/
|-- dpp-sdk-clients/
|   |-- pom.xml
|   |-- dpp-repo-payloads/
|   |-- dpp-repo-client/
|   |-- dpp-registry-payloads/
|   `-- dpp-registry-client/
`-- dpp-sdk-demo/
    |-- pom.xml
    |-- .env.example
    |-- docker-compose.yml
    |-- mock-dpp-repo/
    |-- mock-eu-registry/
    `-- dpp-integration-demo/
```

## Project Responsibilities

- `dpp-datamodel`: DPP domain model, validation, mapping, and JSON transport.
- `dpp-postgres`: optional PostgreSQL persistence for `Dpp4Fun`, including reusable core/version support and the Dpp4Fun-specific relational repository.
- `dpp-sdk-clients`: generic repository and registry HTTP clients plus API payload contracts.
- `dpp-sdk-demo`: mock repository, mock registry, integration demo, Docker runtime, Postman collections, and demo guides.

## Choose What You Need

| Goal | Go to | Relevant module |
| --- | --- | --- |
| Build DPP objects, validate, map, or serialize JSON | [`dpp-datamodel/README.md`](dpp-datamodel/README.md) | `dpp-core`, `dpp4fun` |
| Understand the SDK model structure and validation rules | [`dpp-datamodel/MODEL_GUIDE.md`](dpp-datamodel/MODEL_GUIDE.md) | `dpp-core`, `dpp4fun` |
| Use repository or registry HTTP clients | [`dpp-sdk-clients/README.md`](dpp-sdk-clients/README.md) | `dpp-repo-*`, `dpp-registry-*` |
| Run mock repo, mock registry, or the integration demo | [`dpp-sdk-demo/README.md`](dpp-sdk-demo/README.md) | `mock-dpp-repo`, `mock-eu-registry`, `dpp-integration-demo` |
| Use PostgreSQL storage for `Dpp4Fun` | [`dpp-postgres/README.md`](dpp-postgres/README.md) | `dpp-postgres-core`, `dpp4fun-postgres` |

## Artifacts And Dependencies

Consumer-facing artifacts are built in this monorepo and can then be consumed from the local Maven repository after a local `clean install`. The demo subproject is runnable/demo code, not a reusable library dependency.

After building and installing the relevant subprojects locally, use these modules when consuming the monorepo from another project:

- `dpp.datamodel:dpp-core:0.4.0`
- `dpp.datamodel:dpp4fun:0.4.0`
- `dpp.postgres:dpp-postgres-core:0.4.0`
- `dpp.postgres:dpp4fun-postgres:0.4.0`
- `dpp.client:dpp-repo-payloads:0.4.0`
- `dpp.client:dpp-repo-client:0.4.0`
- `dpp.client:dpp-registry-payloads:0.4.0`
- `dpp.client:dpp-registry-client:0.4.0`

Typical import choices:

- import `dpp-core` when you need the reusable core DPP domain model and validation types
- import `dpp4fun` when you need the furniture-specific SDK layer on top of `dpp-core`
- import `dpp-postgres-core` or `dpp4fun-postgres` only when you need PostgreSQL-backed persistence
- import only the specific `dpp-sdk-clients` modules your application needs
- do not depend on `dpp-sdk-demo` as a library; use it as runnable reference/demo code

## Optional PostgreSQL Persistence

The root-level `dpp-postgres` module is optional.

- Use it when an application wants durable relational PostgreSQL persistence for `Dpp4Fun`.
- It contains:
  - `dpp-postgres-core`
  - `dpp4fun-postgres`
- It stores one stable passport identity plus append-only version history and lightweight lifecycle-event records.
- It is not required for users who only need models, validation, and JSON handling from `dpp-datamodel`.
- `mock-dpp-repo` can run with either backend:
  - `dpp.repo.backend=memory`
  - `dpp.repo.backend=postgres`
- `mock-eu-registry` can run with either backend:
  - `dpp.registry.backend=memory`
  - `dpp.registry.backend=postgres`
- Memory remains the default mock backend.
- As the repository is configured right now, both mock services still default to memory mode unless you explicitly set the PostgreSQL backend property.
- You only need to run a PostgreSQL server if you choose PostgreSQL persistence. For the default memory mode, you do not need PostgreSQL running.

### Backend choice for the mock services

Use the in-memory backends explicitly:

```properties
dpp.repo.backend=memory
dpp.registry.backend=memory
```

Use the PostgreSQL backends explicitly:

```properties
dpp.repo.backend=postgres
dpp.registry.backend=postgres
```

What this means in practice:

- If you do nothing, both `mock-dpp-repo` and `mock-eu-registry` start in memory mode.
- If you set either PostgreSQL backend property, that service must have a reachable PostgreSQL datasource.
- The HTTP APIs stay the same in both modes. Only the storage backend changes.

### Quick Run: Mock Repo And Mock Registry With PostgreSQL

Run these from the monorepo root.

Use Docker Compose for both PostgreSQL servers plus Dockerized mock services. For PostgreSQL-enabled mock services, build from the root reactor so `dpp-postgres` is included.

Windows:

```powershell
.\mvnw.cmd clean package
docker compose -f dpp-sdk-demo/docker-compose.yml up --build
```

Focused faster option:

```powershell
.\mvnw.cmd -pl "dpp-postgres/dpp4fun-postgres,dpp-sdk-demo/mock-dpp-repo,dpp-sdk-demo/mock-eu-registry" -am clean package
docker compose -f dpp-sdk-demo/docker-compose.yml up --build
```

Linux/macOS:

```bash
# Ensure execution permissions:
chmod +x mvnw
./mvnw clean package
docker compose -f dpp-sdk-demo/docker-compose.yml up --build
```

Focused faster option:

```bash
# Ensure execution permissions:
chmod +x mvnw
./mvnw -pl "dpp-postgres/dpp4fun-postgres,dpp-sdk-demo/mock-dpp-repo,dpp-sdk-demo/mock-eu-registry" -am clean package
docker compose -f dpp-sdk-demo/docker-compose.yml up --build
```

Connection rule:

- inside Docker Compose, the repo API container uses `jdbc:postgresql://dpp-repo-db:5432/dpp_repo`
- inside Docker Compose, the registry API container uses `jdbc:postgresql://dpp-registry-db:5432/dpp_registry`
- from a local JAR or IDE, use `jdbc:postgresql://localhost:5433/dpp_repo` for the repo and `jdbc:postgresql://localhost:5434/dpp_registry` for the registry

Detailed non-Docker demo runtime options are in [`dpp-sdk-demo/README.md`](dpp-sdk-demo/README.md). PostgreSQL module usage, storage layout, and lifecycle-event details are in [`dpp-postgres/README.md`](dpp-postgres/README.md).
Docker Compose keeps the mock repo and mock registry PostgreSQL data in separate named volumes. `docker compose down` keeps that data; `docker compose down -v` removes it.

## Entry-Point Example

The main SDK flow is:

1. build a canonical `Dpp4Fun`
2. validate it
3. serialize it when needed
4. send it through the repository client
5. deserialize and validate on the receiving side
6. optionally persist it with PostgreSQL

### Build, Validate, Serialize, And Use The Repo Client

```java
import dpp.repo.client.HttpDppRepoClient;
import dpp.repo.client.core.DppCodec;
import dpp.repo.client.core.DppValidator;
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.ProductClassification;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;

import java.time.LocalDate;
import java.util.UUID;

// 1. Build the reusable core identity block shared by DPP types.
PassportMetadata metadata = new PassportMetadata.Builder()
        .uniqueProductIdentifier(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        .addPassportUpdateDate(LocalDate.of(2026, 6, 29))
        .qrCodeOrDigitalTag("https://example.com/dpp/11111111-1111-1111-1111-111111111111")
        .build();

Organization manufacturer = new Organization.Builder()
        .name("Cir4Fun Furniture GmbH")
        .role(OrganizationRole.MANUFACTURER)
        .build();

Nameplate nameplate = new Nameplate.Builder()
        .gtinCode("04012345678901")
        .manufacturer(manufacturer)
        .build();

// DppCore groups the common passport metadata and nameplate fields.
DppCore coreDpp = new DppCore.Builder()
        .passportMetadata(metadata)
        .nameplate(nameplate)
        .build();

// 2. Add the Dpp4Fun-specific classification and product characteristics.
ProductClassification classification = new ProductClassification.Builder()
        .sector("Furniture")
        .group("Home furniture")
        .category("Beds")
        .addTag("demo")
        .build();

Characteristics characteristics = new Characteristics.Builder()
        .productName("Cir4Fun Platform Bed")
        .brand("Cir4Fun")
        .productType("Bed")
        .dimensions(new Dimensions.Builder()
                .width(90.0)
                .height(80.0)
                .depth(120.0)
                .unit("cm")
                .build())
        .weight(24.5)
        .addFeature("repairable")
        .build();

// 3. Build the canonical immutable Dpp4Fun aggregate.
Dpp4Fun dpp = new Dpp4Fun.Builder()
        .coreDpp(coreDpp)
        .classification(classification)
        .characteristics(characteristics)
        .build();

// 4. Run semantic validation before transport or persistence.
Dpp4FunValidationService validator = new Dpp4FunValidationService();
validator.validate(dpp);

// 5. Serialize to the current Dpp4Fun transport JSON shape and read it back again.
Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
String json = codec.toJson(dpp);
Dpp4Fun parsed = codec.fromJson(json);
validator.validate(parsed);

// 6. Adapt the SDK codec/validator to the generic HTTP repo client interfaces.
DppCodec<Dpp4Fun> clientCodec = new DppCodec<>() {
    @Override
    public String toJson(Dpp4Fun value) {
        return codec.toJson(value);
    }

    @Override
    public Dpp4Fun fromJson(String value) {
        return codec.fromJson(value);
    }
};

DppValidator<Dpp4Fun> clientValidator = validator::validate;

// 7. Use the high-level repository client against a running repo service.
HttpDppRepoClient<Dpp4Fun> repoClient = new HttpDppRepoClient<>(
        "http://localhost:8080",
        clientCodec,
        clientValidator
);

// 8. Store and read back the full DPP through the versioned mock repo API.
repoClient.createDpp(parsed);
Dpp4Fun fromRepo = repoClient.readDppById(dpp.getDppId());
```

How to read this example:

- `dpp-datamodel` gives you the builders, the validation service, and the JSON codec.
- `dpp-sdk-clients` gives you the high-level HTTP client.
- The client stays generic, so you supply a codec and validator for your concrete DPP type.
- The same `Dpp4Fun` object can then be sent to the mock repo or any compatible repo implementation.

### Deserialize, Validate, And Persist With PostgreSQL

```java
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.postgres.dpp4fun.Dpp4FunPostgresRepository;
import org.postgresql.ds.PGSimpleDataSource;

Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
Dpp4FunValidationService validator = new Dpp4FunValidationService();

Dpp4Fun parsed = codec.fromJson(json);
validator.validate(parsed);

PGSimpleDataSource dataSource = new PGSimpleDataSource();
dataSource.setURL("jdbc:postgresql://localhost:5432/dpp");
dataSource.setUser("postgres");
dataSource.setPassword("postgres");

Dpp4FunPostgresRepository repository = new Dpp4FunPostgresRepository(dataSource);
repository.create(parsed, new PostgresDppOperationContext("create-demo", java.time.Instant.now()));
```

This PostgreSQL persistence path is optional. If you are only using the mock repo in its default memory mode, you do not need a PostgreSQL server.

For module-level install and consumption steps, use [`dpp-datamodel/README.md`](dpp-datamodel/README.md) and [`dpp-sdk-clients/README.md`](dpp-sdk-clients/README.md).

## Prerequisites

- JDK 17 or newer on `PATH`, or set `JAVA_HOME`
- Recommended JDK: Java 17. The project targets Java 17. Newer JDKs may work, but Java 17 is the supported baseline for development and validation.
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
.\mvnw.cmd -pl "dpp-postgres/dpp4fun-postgres,dpp-sdk-demo/mock-dpp-repo,dpp-sdk-demo/mock-eu-registry" -am test  # Focused PostgreSQL + mock backend validation
```

Linux/macOS:

```bash
# Ensure the wrapper is executable:
chmod +x mvnw
# Troubleshooting for CRLF carriage-return issues on some shells:
# perl -pi -e 's/\r$//' mvnw

./mvnw --version      # Show the Maven wrapper and Java runtime in use
./mvnw clean test     # Rebuild and run the full reactor test suite
./mvnw clean package  # Build all modules and create the packaged artifacts
./mvnw clean verify   # Run the full verification lifecycle across the reactor
./mvnw -pl "dpp-postgres/dpp4fun-postgres,dpp-sdk-demo/mock-dpp-repo,dpp-sdk-demo/mock-eu-registry" -am test  # Focused PostgreSQL + mock backend validation
```

## Module Build And Run Instructions

Use the module READMEs for individual build, install, and non-Docker run commands:

- [`dpp-datamodel/README.md`](dpp-datamodel/README.md)
- [`dpp-postgres/README.md`](dpp-postgres/README.md)
- [`dpp-sdk-clients/README.md`](dpp-sdk-clients/README.md)
- [`dpp-sdk-demo/README.md`](dpp-sdk-demo/README.md)

## Swagger UI And OpenAPI

Useful URLs once the mock services are running:

- Repo service: `http://localhost:8080`
- Registry service: `http://localhost:8081`
- Repo health: `http://localhost:8080/health`
- Registry health: `http://localhost:8081/health`
- Repo Swagger UI: `http://localhost:8080/swagger-ui.html`
- Repo OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Registry Swagger UI: `http://localhost:8081/swagger-ui.html`
- Registry OpenAPI JSON: `http://localhost:8081/v3/api-docs`

Swagger UI is the easiest interactive entry point once the mock services are running:

- use the repo Swagger UI to explore create, read, update, delete, fine-granular, lifecycle-event, and health endpoints
- use the registry Swagger UI to explore registration and mock metadata lookup endpoints
- use the OpenAPI JSON endpoints when you want machine-readable API descriptions for tooling or import

## Postman Collections

Import these collections from `dpp-sdk-demo/postman`:

- `dpp-lifecycle-api.verified-export-shape.postman_collection.json`
- `dpp-registry-api.verified-export-shape.postman_collection.json`
- `dpp-fine-granular-api.import-safe.postman_collection.json`

Default base URLs:

- repo: `http://localhost:8080`
- registry: `http://localhost:8081`

Use Postman when you want to:

- run the repository lifecycle flow manually
- test fine-granular element reads and updates
- test registry registration and mock metadata lookup behavior
- exercise malformed input, missing-resource, and validation-error cases outside the Java demo runner

Postman does not read your optional local `.env` automatically. Update collection variables yourself if you change ports from the defaults or from values copied from `.env.example`.

For the presenter-oriented walkthrough of those flows, use [`dpp-sdk-demo/DEMO_GUIDE.md`](dpp-sdk-demo/DEMO_GUIDE.md).

## Documentation Map

- `CHANGELOG.md`: release-oriented summary of notable repository changes
- `LICENSE`: project license
- [`dpp-datamodel/README.md`](dpp-datamodel/README.md): datamodel build, consumption, and usage guidance
- [`dpp-datamodel/MODEL_GUIDE.md`](dpp-datamodel/MODEL_GUIDE.md): consolidated SDK model structure and validation reference
- [`dpp-postgres/README.md`](dpp-postgres/README.md): PostgreSQL module structure, storage layout, lifecycle events, and repository usage
- [`dpp-sdk-clients/README.md`](dpp-sdk-clients/README.md): generic client modules, payload contracts, and endpoint coverage
- [`dpp-sdk-demo/README.md`](dpp-sdk-demo/README.md): build and run the mock services and integration demo
- [`dpp-sdk-demo/DEMO_GUIDE.md`](dpp-sdk-demo/DEMO_GUIDE.md): live-demo walkthrough and presenter notes
- [`docs/dpp-sdk-architecture.drawio.png`](docs/dpp-sdk-architecture.drawio.png): repository architecture diagram

## Current Limitations

- This repository is pre-release reference/demo code. It does not prove production readiness, final EN compliance, certification, or legal compliance.
- `mock-dpp-repo` and `mock-eu-registry` are mock/demo services, not production services.
- The registry stores metadata only; it does not store full DPP JSON.
- In-memory demo backends are not durable persistence.
- The registry client supports `POST /v1/registerDPP`. Demo-only repository verification/events/listing and registry metadata lookups are hard-cut over to `/internal/...`; the former unprefixed internal routes are removed and these endpoints are not part of `dpp-sdk-clients`.
- Full-DPP GET routes default to a project-defined compressed summary; EN 18223 payload conformity is not claimed.
- Fine-granular paths implement a bounded RFC 9535-compatible singular subset rather than full RFC 9535 JSONPath.
- Real EU registry integration, production security hardening, and operational readiness are not implemented here.
