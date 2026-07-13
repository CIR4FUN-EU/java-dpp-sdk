# DPP SDK Demo

## Purpose

`dpp-sdk-demo` is the runnable demo area of the monorepo. It shows how the datamodel, PostgreSQL module, and HTTP clients work together through:

- a mock DPP repository service
- a mock registry service
- a command-line integration demo runner
- Postman collections and OpenAPI endpoints for manual testing

This module is demo/runtime code. It is not a reusable SDK library.

Parent coordinates from `dpp-sdk-demo/pom.xml`:

- `groupId`: `demo`
- `artifactId`: `dpp-sdk-demo`
- `version`: `0.4.0`

## Module Map

| Module | What it provides | Default local URL |
| --- | --- | --- |
| `mock-dpp-repo` | Mock repository lifecycle, fine-granular, events, and health endpoints | `http://localhost:8080` |
| `mock-eu-registry` | Mock registry metadata registration and lookup endpoints | `http://localhost:8081` |
| `dpp-integration-demo` | CLI runner for SDK-only and HTTP demo flows | n/a |

## Prerequisites

- Java 17
- local Maven wrapper use from this repo
- upstream artifacts installed when building this subproject in isolation

If you build from the monorepo root, the reactor order already installs what the demo consumes.

If you build `dpp-sdk-demo` on its own, install these upstream artifacts first:

```powershell
.\mvnw.cmd -f ..\dpp-datamodel\pom.xml clean install
.\mvnw.cmd -f ..\dpp-postgres\pom.xml clean install
.\mvnw.cmd -f ..\dpp-sdk-clients\pom.xml clean install
```

```bash
./mvnw -f ../dpp-datamodel/pom.xml clean install
./mvnw -f ../dpp-postgres/pom.xml clean install
./mvnw -f ../dpp-sdk-clients/pom.xml clean install
```

The demo repo module depends on `dpp4fun`, `dpp4fun-postgres`, and the client payload/client artifacts.

## Build

Run from `dpp-sdk-demo`.

Build the whole demo:

```powershell
.\mvnw.cmd clean package
```

```bash
./mvnw clean package
```

Build only the mock repository:

```powershell
.\mvnw.cmd -pl "mock-dpp-repo" -am test
```

```bash
./mvnw -pl "mock-dpp-repo" -am test
```

Build only the mock registry:

```powershell
.\mvnw.cmd -pl "mock-eu-registry" -am test
```

```bash
./mvnw -pl "mock-eu-registry" -am test
```

Build only the integration demo runner:

```powershell
.\mvnw.cmd -pl "dpp-integration-demo" -am test
```

```bash
./mvnw -pl "dpp-integration-demo" -am test
```

## Run The Mock Services

The commands below are relative to the `dpp-sdk-demo` directory. If your current
directory is the monorepo root, use the root-relative alternatives shown below.

### Mock Registry

```powershell
java -jar mock-eu-registry/target/mock-eu-registry-0.4.0-exec.jar --debug=false
```

```bash
java -jar mock-eu-registry/target/mock-eu-registry-0.4.0-exec.jar --debug=false
```

From the monorepo root:

```powershell
java -jar .\dpp-sdk-demo\mock-eu-registry\target\mock-eu-registry-0.4.0-exec.jar --debug=false
```

```bash
java -jar ./dpp-sdk-demo/mock-eu-registry/target/mock-eu-registry-0.4.0-exec.jar --debug=false
```

### Mock Repository

```powershell
java -jar mock-dpp-repo\target\mock-dpp-repo-0.4.0-exec.jar --debug=false
```

```bash
java -jar mock-dpp-repo/target/mock-dpp-repo-0.4.0-exec.jar --debug=false
```

From the monorepo root:

```powershell
java -jar .\dpp-sdk-demo\mock-dpp-repo\target\mock-dpp-repo-0.4.0-exec.jar --debug=false
```

```bash
java -jar ./dpp-sdk-demo/mock-dpp-repo/target/mock-dpp-repo-0.4.0-exec.jar --debug=false
```

Both services import `optional:file:.env[.properties]`, so starting them from `dpp-sdk-demo` lets them read an optional local `.env` file if present. Use `dpp-sdk-demo/.env.example` as the tracked template when you want overrides. If you do not create `.env`, the built-in defaults still apply.

## Run The Integration Demo

### SDK-Only Mode

Does not require the mock services.

```powershell
java -jar dpp-integration-demo\target\dpp-integration-demo-0.4.0.jar sdk --debug=false
```

```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-0.4.0.jar sdk --debug=false
```

### HTTP / Default Mode

Uses the HTTP clients against the running mock services.

```powershell
java -jar dpp-integration-demo\target\dpp-integration-demo-0.4.0.jar http --debug=false
```

```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-0.4.0.jar http --debug=false
```

Running with no explicit mode also falls back to the HTTP standards flow:

```powershell
java -jar dpp-integration-demo\target\dpp-integration-demo-0.4.0.jar --debug=false
```

```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-0.4.0.jar --debug=false
```

### Full / All Mode

Runs the SDK capability checks first, then reuses that flow for the HTTP demo.

```powershell
java -jar dpp-integration-demo\target\dpp-integration-demo-0.4.0.jar all --debug=false
```

```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-0.4.0.jar all --debug=false
```

### Explicit Base URL Overrides

```powershell
java -jar dpp-integration-demo\target\dpp-integration-demo-0.4.0.jar http http://localhost:8091 http://localhost:8090 --debug=false
```

```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-0.4.0.jar http http://localhost:8091 http://localhost:8090 --debug=false
```

The argument order is `registryUrl` then `repoUrl`.

## Ports And Base URLs

From `application.properties`:

- `mock-dpp-repo` listens on `MOCK_REPO_PORT`, falling back to `DPP_REPO_PORT`, then `8080`
- `mock-eu-registry` listens on `MOCK_REGISTRY_PORT`, falling back to `DPP_REGISTRY_PORT`, then `8081`

Default local URLs:

- Repo: `http://localhost:8080`
- Registry: `http://localhost:8081`
- Repo health: `http://localhost:8080/health`
- Registry health: `http://localhost:8081/health`
- Repo Swagger UI: `http://localhost:8080/swagger-ui.html`
- Repo OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Registry Swagger UI: `http://localhost:8081/swagger-ui.html`
- Registry OpenAPI JSON: `http://localhost:8081/v3/api-docs`

Docker-style service-name resolution is also implemented in `HttpServiceDemoRunner`:

- Registry: `http://dpp-registry-api:${DPP_REGISTRY_PORT}`
- Repo: `http://dpp-repo-api:${DPP_REPO_PORT}`

## Postman Collections

Import these files from `dpp-sdk-demo/postman`:

- `dpp-lifecycle-api.verified-export-shape.postman_collection.json`
- `dpp-registry-api.verified-export-shape.postman_collection.json`
- `dpp-fine-granular-api.import-safe.postman_collection.json`

Default base URLs:

- repo: `http://localhost:8080`
- registry: `http://localhost:8081`

Postman does not read your optional local `.env` automatically. Update collection variables yourself if you change ports from the defaults or from values copied from `.env.example`.

## Signs Of Success

Successful service startup:

- `GET /health` returns `UP`
- Swagger UI loads on ports `8080` and `8081`

Successful integration demo:

- the runner prints `DPP4Fun SDK + dpp-sdk-clients Standards Demo`
- create, read, update, fine-granular, registry registration, and delete steps complete without an uncaught exception
- the runner ends with `HTTP services demo complete`

## Demo Use Cases

### SDK-Only Walkthrough

`SdkCapabilityDemoRunner` demonstrates:

- building demo `Dpp4Fun` objects
- validating them through `Dpp4FunValidationService`
- mapping through `Dpp4FunMapper`
- serializing and deserializing through `Dpp4FunJsonCodec`

### Repository Storage Flow

`HttpServiceDemoRunner` demonstrates these repository client calls against `mock-dpp-repo`:

- `createDpp`
- `readDppById`
- `readDppByProductId`
- `updateDppById`
- `readDppVersionByIdAndDate`
- `readDppIdsByProductIds`
- `deleteDppById`

The three GET routes that return complete DPPs default to a project-defined compressed summary when `representation` is omitted. Typed Java client reads request `representation=full`; the compressed payload is not claimed as an EN 18223-defined representation.

The HTTP demo prints the compressed summary beside the typed full-DPP read so their different payload purposes are visible.

### Registry Registration Flow

The HTTP demo then registers the stored DPP metadata through `POST /v1/registerDPP` using `HttpDppRegistryClient`.

The public registration request uses `uniqueProductIdentifier`, `digitalProductPassportId`, `uniqueEconomicOperatorIdentifier`, and `dppApiEndpoint`; the response uses `registrationId`. Backup-operator behavior is not implemented.

The registry verifies that the referenced DPP exists in the repository before accepting the registration.

### Internal Mock API Boundary

Standard-facing operations use `/v1/...`. Demo-only operations use `/internal/...`: the repository exposes active-ID listing, HEAD verification, and lifecycle events there, while the registry exposes metadata listing and lookup by registration ID or DPP ID. `/`, `/health`, `/swagger-ui`, and `/v3/api-docs` remain conventional infrastructure routes. This is a hard cutover; the former unprefixed internal routes are not aliases.

### Fine-Grained Read And Update

The HTTP demo also calls:

- `readDataElement(dppId, "$.characteristics.productName")`
- `updateDataElement(dppId, "$.characteristics.productName", ...)`

The repo supports a bounded RFC 9535-compatible singular subset: `$`, dot members, quoted bracket members, and non-negative array indexes. It does not claim full RFC 9535 support; wildcard, descendant, union, slice, filter, function, and negative-index selectors return HTTP 501. Malformed paths return 400, valid paths with no matching element return 404, and direct JSON PATCH values are persisted only after full-DPP validation succeeds.

The HTTP demo prints the dot-member path `$.characteristics.productName`, the array-index path `$.billOfMaterials.materials[0].name`, and the product-name value before and after replacement.

### Error Handling Examples

The integration demo intentionally exercises:

- registry rejection for a missing repository DPP
- client-side validation failure before sending an invalid DPP
- repository 404 handling for a missing DPP id
- network failure handling for an unreachable registry URL

## Storage Modes

Default mode is memory:

```properties
DPP_REPO_BACKEND=memory
DPP_REGISTRY_BACKEND=memory
```

The mock repository can also use PostgreSQL through `dpp4fun-postgres`:

```properties
DPP_REPO_BACKEND=postgres
```

The mock registry also has a PostgreSQL-backed demo seam, but that persistence implementation lives inside `mock-eu-registry`, not in the standalone `dpp-postgres` module.

## Boundaries

- `mock-dpp-repo` and `mock-eu-registry` are mock/demo services, not production services.
- In-memory storage is not durable persistence.
- The registry stores metadata only, not full DPP JSON.
- `dpp-integration-demo` is a runnable walkthrough, not reusable SDK logic.
- This module does not prove production security, operational readiness, or real EU registry integration.

## Related Docs

- [`DEMO_GUIDE.md`](DEMO_GUIDE.md)
