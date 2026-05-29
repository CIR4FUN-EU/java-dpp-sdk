# Demo Guide

Use this as the single live-demo script and reference guide. It keeps the demo focused on how the SDK and client libraries are reused without copying their logic into the demo services.

For build, prerequisite, and basic run commands, use `README.md` first. This guide is for presenting the demo flow and explaining what each step is meant to show.

## 1. Introduce The Split

Open `dpp-integration-demo` first, then the two mock service modules.

Say:

- `dpp4fun` owns the Dpp4Fun product model/builders, mapper support, validation, and `Dpp4FunJsonCodec`.
- `dpp-core` owns reusable core DPP model classes, validators, payload mapping, and utilities.
- `dpp-sdk-clients` owns the split repo/registry payload contracts, HTTP clients, and client error categories for the standard-style mock APIs.
- This demo adds only Dpp4Fun adapters, mock HTTP services, runners, Postman collections, in-memory stores, and a small mock-only registry lookup helper.
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
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar sdk --debug=false
```

```bash
# Linux/MacOS
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar sdk --debug=false
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

Start the registry and repo services as described in `README.md`.

For the current containerized maintainer workflow, `README.md` now includes:

```powershell
.\mvnw.cmd clean package
docker compose -f docker-compose.build.yml up --build
```

For publishing to this GitLab project's container registry, `README.md` now includes:

```powershell
.\mvnw.cmd clean package
docker compose -f docker-compose.build.yml build
docker login container-registry.gitlab.cc-asp.fraunhofer.de
docker compose -f docker-compose.build.yml push
```

That works because `dpp-sdk-demo/.env` carries the GitLab registry path and image names used by both compose files.

Base URLs for the live demo:

- Registry: `http://localhost:${DPP_REGISTRY_PORT}` or default `http://localhost:8081`
- Repo: `http://localhost:${DPP_REPO_PORT}` or default `http://localhost:8080`

Container networking caveat:

- The registry container must reach the repo container at `http://mock-dpp-repo:${DPP_REPO_PORT}`.
- `localhost` inside the registry container points back to the registry container itself.
- The registry handles that internal container-to-container hop automatically when the submitted public repo URL matches the configured public repo base URL.

Registry verification config note:

- The registry service reads the repo port from `dpp-sdk-demo/.env` when present and otherwise falls back to `8080`.
- That shared config drives the seeded registry record and the registry's internal verification base URL.
- `POST /registerDPP` still starts from the `repoUrl` inside the request payload.
- If that payload matches the configured public repo base URL, the registry internally rewrites only the verification hop to its configured verification base URL.
- That lets one public repo URL work for both localhost and Dockerized runs.
- If verification still fails, the registry returns a repo-verification error that names the public repo URL and, when different, the internal verification URL.

Runner URL resolution behavior:

- Without explicit URL arguments, the HTTP demo runner checks Docker-style service names first:
  - Registry: `http://mock-eu-registry:${DPP_REGISTRY_PORT}`
  - Repo: `http://mock-dpp-repo:${DPP_REPO_PORT}`
- If those checks fail, it falls back to `http://localhost:${DPP_REGISTRY_PORT}` and `http://localhost:${DPP_REPO_PORT}`.
- If both the Docker-style and localhost health checks fail for a service, the runner throws an exception and quits before the demo flow continues.
- `dpp-sdk-demo/.env` controls those ports when present; if `.env` is missing, the old `8081` and `8080` defaults still apply.

## 4. Run The HTTP Client Demo

Run the standards HTTP flow:

```powershell
# Windows
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar --debug=false
```

```bash
# Linux/MacOS
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar --debug=false
```

For the fuller stakeholder demo, run SDK capability checks first and then reuse the same SDK-built DPPs in the HTTP flow:

```powershell
# Windows
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar all --debug=false
```

```bash
# Linux/MacOS
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar all --debug=false
```

Point out:

- The runner uses public `HttpDppRepoClient` for `CreateDPP`, `ReadDPPById`, `ReadDPPByProductId`, `ReadDPPVersionByProductIdAndDate`, `ReadDPPIdsByProductIds`, `UpdateDPPById`, `DeleteDPPById`, `ReadDataElement`, and `UpdateDataElement`.
- The runner uses public `HttpDppRegistryClient` for `POST /registerDPP`.
- The runner uses a small demo-local helper for `GET /registry/dpps/...` because those internal mock lookup endpoints are not part of the split upstream registry client.
- This helper is mock/demo-only visibility plumbing and is not part of the public `dpp-registry-client` contract.
- The client validates before sending.
- No additional custom helper clients are used beyond that mock/demo-only registry read-back helper.
- The repo service receives JSON, maps it with SDK `Dpp4FunJsonCodec`, validates it with `Dpp4FunValidationService`, and stores it in memory.
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
- For this phase the registry is purely in-memory and does not call a real EU service.
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

Use the import and startup details from `README.md`. The list below is the presenter-facing reference.

- `postman/dpp-registry-api.verified-export-shape.postman_collection.json`
- `postman/dpp-fine-granular-api.import-safe.postman_collection.json`
- `postman/dpp-lifecycle-api.verified-export-shape.postman_collection.json`

Variables:

- `registryBaseUrl = http://localhost:${DPP_REGISTRY_PORT}` or default `http://localhost:8081`
- `repoBaseUrl = http://localhost:${DPP_REPO_PORT}` or default `http://localhost:8080`
- `dppId = 49192c87-20c8-4b6f-88de-48b56ca4c211`
- `productId = 04012345678901`
- `registryId = 8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc`

The repo and registry mocks still start with default in-memory records for ad-hoc manual inspection, but the main happy-path collections do not depend on that shared startup state.

- The lifecycle collection creates a fresh DPP and deletes it during the flow.
- The fine-granular collection creates its own DPP before the element reads/updates and deletes it at the end.
- The registry collection creates its own repo DPP before registration and deletes that repo DPP at the end.
- The registry mock does not expose a metadata delete endpoint, so repeated registry collection runs stay independent by using fresh DPP and product identifiers each time.

The collections cover the standard-style mock APIs only. No `/exists` endpoint is used; repo-backed registry verification uses `HEAD /dpps/{dppId}`.

## Scope

This demo is intentionally small.

Mocked:

- EU registry behavior
- DPP repository behavior
- Persistence, using in-memory stores

Not implemented:

- Real EU registry integration
- Real database
- Authentication or OAuth
- Retry framework
- Production resilience, retries, auditing, or monitoring
- AAS adapter
- Backup identifier/operator/provider placeholders
- Full official draft data-model field coverage
