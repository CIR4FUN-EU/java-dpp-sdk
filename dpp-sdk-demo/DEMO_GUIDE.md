# Demo Guide

Use this as the single live-demo script and reference guide. It keeps the demo focused on how the SDK and client libraries are reused without copying their logic into the demo services.

For build, prerequisite, and basic run commands, use `README.md` first. This guide is only for presenting the demo flow and explaining what each step is meant to show.

## 1. Introduce The Split

Open `dpp-integration-demo` first, then the two mock service modules.

Say:

- `dpp4fun` owns the Dpp4Fun product model/builders, mapper support, validation, and `Dpp4FunJsonCodec`.
- `dpp-core` owns reusable core DPP model classes, validators, payload mapping, and utilities.
- `dpp-sdk-clients` owns the split repo/registry payload contracts, HTTP clients, and client error categories for the standard-style mock APIs.
- This demo adds only Dpp4Fun adapters, mock HTTP services, runners, Postman collections, mock backend adapters, and a small mock-only registry lookup helper.
- Mock HTTP services simulate external repo and registry systems; they are not part of the client library.
- The runtime truth for this repo is the demo code plus the installed `dpp4fun` / `dpp-core` / `dpp-sdk-clients` artifacts used by Maven.

How the SDK helps:

- The demo uses SDK `Dpp4Fun` and nested domain objects instead of defining local DPP models.
- `DemoDppFactory` builds repeatable valid and invalid examples with SDK builders.
- Common DPP fields live in `DppCore`; `Dpp4Fun` wraps that core and adds classification, characteristics, and bill of materials.
- `Dpp4FunValidationService` is reused by adapters, repo service, and tests; the registry service validates only registry metadata fields.
- `Dpp4FunJsonCodec` is the JSON transport path for services and client adapters.
- SDK mapper support is shown once by round-tripping domain DPPs through Java payloads with nested `coreDpp`; flat payload accessors still work, and JSON output remains flat.

How `dpp-sdk-clients` helps:

- HTTP flows use public `HttpDppRepoClient` and `HttpDppRegistryClient` clients.
- The demo supplies only thin Dpp4Fun-specific `DppCodec<T>` and `DppValidator<T>` adapters.
- The minimum client integration surface is small: base URL plus a `DppCodec<T>` and `DppValidator<T>`.
- Client-side validation, mapping errors, HTTP errors, and network errors stay separated by the client library.
- The clients use the standard-style repo and registry endpoint paths.
- Shared repo and registry request/response DTOs now come from the split upstream payload modules instead of local demo duplicates.

Files to show:

- `dpp-integration-demo/src/main/java/demo/producer/support/DemoDppFactory.java`
- `dpp-integration-demo/src/main/java/demo/producer/support/Dpp4FunDppCodecAdapter.java`
- `dpp-integration-demo/src/main/java/demo/producer/support/Dpp4FunDppValidatorAdapter.java`
- `dpp-integration-demo/src/main/java/demo/producer/SdkCapabilityDemoRunner.java`
- `dpp-integration-demo/src/main/java/demo/producer/HttpServiceDemoRunner.java`

## 2. Run The SDK Capability Demo

Run without backend services:

```Powershell
# Windows
java -jar dpp-integration-demo\target\dpp-integration-demo-0.4.0.jar sdk --debug=false
```

```bash
# Linux/MacOS
java -jar dpp-integration-demo/target/dpp-integration-demo-0.4.0.jar sdk --debug=false
```

Point out:

- The runner builds repeatable DPP examples through SDK builders in `DemoDppFactory`.
- One complete DPP is assembled step by step by building `PassportMetadata`, `Nameplate`, and `Documentation` into `DppCore`, then passing that core to `Dpp4Fun`.
- An intentionally incomplete DPP omits a required root field and shows the SDK builder exception.
- `Dpp4FunValidationService` accepts valid DPPs and rejects semantic errors.
- Immutable updates and deletes create new DPP objects with `toBuilder()` instead of mutating the original.
- SDK mapper support round-trips domain and payload objects.
- `Dpp4FunJsonCodec` serializes, parses, and validates transport JSON. The JSON payload remains flat.

Construction shape:

```java
DppCore coreDpp = new DppCore.Builder()
        .passportMetadata(metadata)
        .nameplate(nameplate)
        .documentation(documentation)
        .build();

Dpp4Fun dpp = new Dpp4Fun.Builder()
        .coreDpp(coreDpp)
        .classification(classification)
        .characteristics(characteristics)
        .billOfMaterials(billOfMaterials)
        .build();
```

Immutable edit shape:

```java
Characteristics updated = dpp.getCharacteristics().toBuilder()
        .productName("Cir4Fun Platform Bed - Edited")
        .build();

Dpp4Fun edited = dpp.toBuilder()
        .characteristics(updated)
        .build();
```

Immutable delete shape:

```java
PassportMetadata metadata = dpp.getPassportMetadata().toBuilder()
        .externalDocumentationLink(null)
        .build();

DppCore withoutDocs = dpp.getCoreDpp().toBuilder()
        .passportMetadata(metadata)
        .documentation(null)
        .build();

BillOfMaterials smallerBom = dpp.getBillOfMaterials().toBuilder()
        .removeMaterial(material)
        .build();
```

Validation and mapping semantics are unchanged; only the domain construction path now goes through `DppCore`.

## 3. Start The HTTP Services

Start the registry and repo services using the relevant commands from `README.md`.

Presenter note:

- Use the local JAR workflow from `README.md` when you want the services visible on the host without Docker.
- Use the local container build workflow from `README.md` when you want to demonstrate the current Dockerized maintainer path.
- Both services default to the in-memory backend, which needs no PostgreSQL server. Use the PostgreSQL run options in `README.md` when you want the same HTTP APIs backed by PostgreSQL, either with only the databases in Docker and the apps local or with all four containers running through Docker Compose.
- In both modes, the repo keeps validation, JSON Merge Patch, and fine-granular element-path handling in the mock service layer, and the registry keeps request validation, duplicate checks, repo `HEAD` verification, and response mapping there. PostgreSQL only changes persistence.

Base URLs for the live demo:

- Registry: `http://localhost:${DPP_REGISTRY_PORT}` or default `http://localhost:8081`
- Repo: `http://localhost:${DPP_REPO_PORT}` or default `http://localhost:8080`

Swagger UI / OpenAPI URLs:

- Mock repo Swagger UI: `http://localhost:8080/swagger-ui.html`
- Mock repo OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Mock registry Swagger UI: `http://localhost:8081/swagger-ui.html`
- Mock registry OpenAPI JSON: `http://localhost:8081/v3/api-docs`

Use Swagger UI when you want to test GET, POST, PATCH, DELETE, and HEAD requests directly against the local mock services without importing a Postman collection.

Container networking caveat:

- The registry container must reach the repo API container at `http://dpp-repo-api:${DPP_REPO_PORT}`.
- `localhost` inside the registry container points back to the registry container itself.
- The registry handles that internal container-to-container hop automatically when the submitted public repo URL matches the configured public repo base URL.
- In PostgreSQL Docker mode, the repo and registry also talk to separate database containers: `dpp-repo-db` and `dpp-registry-db`.

Registry verification config note:

- The registry service reads the repo port from `dpp-sdk-demo/.env` when present and otherwise falls back to `8080`.
- That shared config drives the seeded registry record and the registry's internal verification base URL.
- `POST /registerDPP` still starts from the `repoUrl` inside the request payload.
- If that payload matches the configured public repo base URL, the registry internally rewrites only the verification hop to its configured verification base URL.
- That lets one public repo URL work for both localhost and Dockerized runs.
- If verification still fails, the registry returns a repo-verification error that names the public repo URL and, when different, the internal verification URL.

Runner URL resolution behavior:

- Without explicit URL arguments, the HTTP demo runner checks Docker-style service names first:
  - Registry: `http://dpp-registry-api:${DPP_REGISTRY_PORT}`
  - Repo: `http://dpp-repo-api:${DPP_REPO_PORT}`
- If those checks fail, it falls back to `http://localhost:${DPP_REGISTRY_PORT}` and `http://localhost:${DPP_REPO_PORT}`.
- If both the Docker-style and localhost health checks fail for a service, the runner throws an exception and quits before the demo flow continues.
- `dpp-sdk-demo/.env` controls those ports when present; if `.env` is missing, the old `8081` and `8080` defaults still apply.

## 4. Run The HTTP Client Demo

Run the integration-demo command from `README.md`.

Presenter note:

- Use the default run for the standard HTTP lifecycle flow.
- Use `all` when you want to show the SDK capability checks first and then reuse the same SDK-built DPPs in the HTTP flow.

Point out:

- The runner uses public `HttpDppRepoClient` for `CreateDPP`, `ReadDPPById`, `ReadDPPByProductId`, `ReadDPPVersionByProductIdAndDate`, `ReadDPPIdsByProductIds`, `UpdateDPPById`, `DeleteDPPById`, `ReadDataElement`, and `UpdateDataElement`.
- The runner uses public `HttpDppRegistryClient` for `POST /registerDPP`.
- The runner uses a small demo-local helper for `GET /registry/dpps/...` because those internal mock lookup endpoints are not part of the split upstream registry client.
- This helper is mock/demo-only visibility plumbing and is not part of the public `dpp-registry-client` contract.
- The client validates before sending.
- No additional custom helper clients are used beyond that mock/demo-only registry read-back helper.
- The repo service receives JSON, maps it with SDK `Dpp4FunJsonCodec`, validates it with `Dpp4FunValidationService`, and stores it through the selected backend. The default backend is memory; PostgreSQL is optional.
- Full DPP reads return a wrapped payload that the HTTP client maps back into an SDK domain object.
- The registry verifies the repo reference with `HEAD /dpps/{dppId}` before storing metadata; it does not fetch the full DPP JSON.
- Fine-granular reads and updates return wrapped raw JSON element payloads.
- Validation, mapping, HTTP, and network errors are separated.
- The network error step is intentional; it uses an unreachable registry URL to show client exception separation.

## 5. Show Registry Registration

Use the registry Postman collection:

1. `RegisterDPP`
2. `Read mock registry record by registry id`
3. `Read mock registry record by dpp id`

Before running it, set the collection variables for your network context:

- `repoBaseUrl`
  - Used by Postman for host-side repo create/delete calls and written into the registration payload as `repoUrl`.
  - Keep this at `http://localhost:${DPP_REPO_PORT}` for normal local or Docker-host access.
  - The registry will internally rewrite the verification hop when Docker requires a different internal hostname.

Say:

- The producer/client can store a DPP in the repo service and then call `POST /registerDPP` directly.
- During registration the registry calls `{repoUrl}/dpps/{dppIdentifier}` with HEAD and stores metadata only if the repo confirms the DPP is active.
- The registry stores only registration metadata, not the full DPP JSON.
- The registry trusts the repo as the validated DPP store and does not run SDK validation itself.
- The repo service does not automatically call the registry in this demo.
- The registry can use memory or a local PostgreSQL backend in this demo, but it still does not call a real EU service.
- The two `GET /registry/dpps/...` endpoints are mock/internal convenience helpers for tests and debugging.

## 6. Show Invalid And Malformed Input

Use the demo runner and Postman collections:

- Invalid DPP: shown by the HTTP runner and repo collection.
- Malformed JSON: shown by the repo/lifecycle Postman collection and service tests.
- Not found: shown by the HTTP runner and Postman collections.
- Network failure: intentionally shown by the HTTP runner with an unreachable registry URL.

Specific Postman requests to use:

- Registry collection: `RegisterDPP missing required field`, `Read missing registry record`
- Registry collection: `RegisterDPP missing repo DPP`, `RegisterDPP repo unavailable`
- Lifecycle collection: `CreateDPP malformed JSON`, `Verify DPP exists by HEAD`, `Verify missing DPP by HEAD`, `Read missing DPP`, `DeleteDPPById`
- Fine granular collection: `ReadDataElement`, `UpdateDataElement invalid path`

Say:

- Invalid DPPs are validation failures.
- Malformed JSON is a mapping/parsing failure in the repo flow; the registry collection currently demonstrates required-field validation and missing-record behavior instead.
- The typed client runner works with domain objects, so malformed raw JSON belongs in Postman and service tests.

## Postman Collections

Use the import names, base URLs, and startup details from `README.md`. This section is only the presenter-facing reminder of what the collections demonstrate.

The repo and registry mocks still start with default in-memory records for ad-hoc manual inspection, but the main happy-path collections do not depend on that shared startup state.

- The lifecycle collection creates a fresh DPP and deletes it during the flow.
- The fine-granular collection creates its own DPP before the element reads/updates and deletes it at the end.
- The registry collection creates its own repo DPP before registration and deletes that repo DPP at the end.
- The registry mock does not expose a metadata delete endpoint, so repeated registry collection runs stay independent by using fresh DPP and product identifiers each time.
- Their default ad-hoc variables are also isolated from Swagger UI and the demo runner, and each collection cleanup delete falls back to its own delete-only seeded DPP until a create step replaces that variable with a fresh runtime ID.

The collections cover the standard-style mock APIs only. No `/exists` endpoint is used; repo-backed registry verification uses `HEAD /dpps/{dppId}`.

## Scope

This demo is intentionally small.

Mocked:

- EU registry behavior
- DPP repository behavior
- Persistence, using default in-memory storage or optional PostgreSQL-backed repo and registry adapters

Not implemented:

- Real EU registry integration
- Authentication or OAuth
- Retry framework
- Production resilience, retries, auditing, or monitoring
- AAS adapter
- Backup identifier/operator/provider placeholders
- Full official draft data-model field coverage
