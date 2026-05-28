# DPP SDK Demo

Partner-facing Java backend demo showing how `com.example.dppsdk:dpp4fun:0.3.0` and the split `dpp-sdk-clients` modules work together.

This phase is draft-prEN-18222-aligned. It is not a claim of final EN 18222 compliance or production readiness.

Use this file as the quickstart for building and running the demo. Use `DEMO_GUIDE.md` as the live-demo script and talking-points guide.

## Purpose

This repo exists to show the reuse boundary between the SDK, the HTTP client library, and a small partner-facing backend demo:

- `dpp4fun` owns the `Dpp4Fun` product aggregate, furniture-specific builders/model classes, mapper support, validation, and `Dpp4FunJsonCodec`.
- `dpp-core` owns reusable core DPP models, validators, payload mapping, and shared utilities used transitively by `dpp4fun`.
- `dpp-sdk-clients` owns shared repo/registry payload contracts plus the low-level HTTP clients for the standard-style DPP APIs.
- This repo adds only Dpp4Fun demo fixtures, thin SDK adapters, mock services, runners, in-memory stores, internal mock lookup DTOs, tests, and Postman collections.

Runtime truth note:

- The canonical runtime view for this demo is the `demo` repo plus the locally installed `dpp4fun`, `dpp-core`, and `dpp-sdk-clients` artifacts used at build/run time.
- Treat Maven-resolved classes as the source of truth.

## What It Shows

- Build Dpp4Fun DPPs with SDK builders and demo fixtures.
- Show a full DPP assembled step by step by building `DppCore` common fields first, then wrapping it in `Dpp4Fun`.
- Validate full product DPPs with SDK `Dpp4FunValidationService`.
- Show SDK required-field protection when an incomplete DPP is built.
- Demonstrate immutable edits and deletes with `toBuilder()`.
- Convert DPPs to and from JSON with SDK `Dpp4FunJsonCodec`.
- Keep the external DTO/JSON payload shape flat while the domain model uses `DppCore`.
- Adapt the SDK type to `dpp-repo-client` with thin `DppCodec<T>` and `DppValidator<T>` adapters.
- Show that the client integration surface stays small: base URL plus codec and validator adapters.
- Use `HttpDppRepoClient` and `HttpDppRegistryClient` against the mock Life Cycle API, simplified Fine Granular API, and Registry API.
- Store a DPP in the repo service, verify it by lightweight HEAD, read it by ID or product ID, patch the full DPP, read and update a single element, query IDs by product IDs, register it with the registry, and soft-delete it.
- Demonstrate invalid DPP, malformed JSON, conflict, not-found, HTTP, and network error paths.

## Modules

- `mock-eu-registry`: mock registry HTTP service on `http://localhost:8081`.
- `mock-dpp-repo`: mock repo HTTP service on `http://localhost:8080`.
- `dpp-integration-demo`: command-line SDK and HTTP demo runner, including the demo DPP factory, thin SDK-to-client adapters, and the mock-only registry read-back helper used for internal visibility.

## Prerequisites

Install the reusable libraries locally first from your own split SDK/data-model checkout and `dpp-sdk-clients` checkout.

Windows:

```powershell
cd <path-to-dpp-datamodel>
.\mvnw.cmd clean install

cd <path-to-dpp-sdk-clients>
.\mvnw.cmd clean install
```

Linux/MacOS:

```bash
cd <path-to-dpp-datamodel>
./mvnw clean install

cd <path-to-dpp-sdk-clients>
./mvnw clean install
```

## Build

Windows:

```powershell
# Run from the repository root
.\mvnw.cmd clean package
```

Linux/MacOS:

```bash
./mvnw clean package
```

On Linux/macOS, `mvnw` is committed as executable, so a fresh clone should run `./mvnw ...` without a manual `chmod +x`.

Common verification commands on:

```powershell
# Windows
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
.\mvnw.cmd clean package
```

```bash
# Linux/MacOS
./mvnw clean test
./mvnw clean verify
./mvnw clean package
```

Wrapper note:

- The Maven wrapper jar is committed in this repo.
- On a machine that does not already have the Maven 3.9.11 distribution cached, the first wrapper run may download it from Maven Central.

## Run Services

Start the services in this order before the default standards demo flow:

Start the registry:

```bash
java -jar mock-eu-registry\target\mock-eu-registry-1.0.0-SNAPSHOT-exec.jar --debug=false # Windows
```
```bash
java -jar mock-eu-registry/target/mock-eu-registry-1.0.0-SNAPSHOT-exec.jar --debug=false # Linux/MacOS
```

Start the repo:

```bash
java -jar mock-dpp-repo\target\mock-dpp-repo-1.0.0-SNAPSHOT-exec.jar --debug=false # Windows
```

```bash
java -jar mock-dpp-repo/target/mock-dpp-repo-1.0.0-SNAPSHOT-exec.jar --debug=false # Linux/MacOS
```

## Run Demo

Expected flow:

1. Run `sdk` when you want the SDK-only capability walkthrough.
2. Run the default mode, `standards`, or `http` when the registry and repo services are already running.
3. Run `all` when you want the SDK walkthrough first and then the HTTP flow.
4. Run `sdk-http` when you want the same combined flow as `all`.

With registry and repo already running:

```bash
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar --debug=false # Windows
```
```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar --debug=false # Linux/MacOS
```

The default run is the standards end-to-end flow. For a fuller stakeholder demo, run SDK capability checks first and then reuse those same SDK-built DPPs in the HTTP flow:

```bash
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar all --debug=false # Windows
```
```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar all --debug=false # Linux/MacOS
```

SDK-only mode does not require the backend services:

```bash
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar sdk --debug=false # Windows
```
```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar sdk --debug=false # Linux/MacOS
```

The SDK-only mode shows `DppCore` construction, `Dpp4FunValidationService`, mapper/JSON round trips, and immutable edit/delete examples for characteristics, documentation, and bill of materials entries.

Explicit HTTP-only mode:

```bash
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar http --debug=false # Windows
```
```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar http --debug=false # Linux/MacOS
```

Optional base URL overrides:

```bash
java -jar dpp-integration-demo\target\dpp-integration-demo-1.0.0-SNAPSHOT.jar http http://localhost:8091 http://localhost:8090 --debug=false # Windows
```
```bash
java -jar dpp-integration-demo/target/dpp-integration-demo-1.0.0-SNAPSHOT.jar http http://localhost:8091 http://localhost:8090 --debug=false # Linux/MacOS
```

The producer uses public `HttpDppRepoClient` APIs for the Life Cycle and Fine Granular flows and public `HttpDppRegistryClient` APIs for `/registerDPP`. The upstream split registry client does not expose the mock-only lookup endpoints, so the producer uses a small demo-local HTTP helper only for `GET /registry/dpps/...` read-back against this mock service. The demo creates the DPP in the repo first, then calls the registry; the repo service does not automatically call the registry.

Successful runs print step-by-step console output for the selected flow and finish without stack traces.

## Postman

Import:

- `postman/dpp-lifecycle-api.verified-export-shape.postman_collection.json`
- `postman/dpp-registry-api.verified-export-shape.postman_collection.json`
- `postman/dpp-fine-granular-api.import-safe.postman_collection.json`

Base URLs:

- Registry: `http://localhost:8081`
- Repo: `http://localhost:8080`

Default in-memory demo data on clean startup:

- Repo preloads DPP `49192c87-20c8-4b6f-88de-48b56ca4c211` for product `04012345678901`.
- Registry preloads metadata record `8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc` for a separate demo DPP and repo URL `http://localhost:8080`.

Collection independence:

- The lifecycle collection already creates a fresh DPP and deletes it at the end.
- The fine-granular collection now creates its own DPP at the start and deletes it at the end.
- The registry collection now creates its own repo DPP before registration and deletes that repo DPP at the end.
- The mock registry API has no delete endpoint for registry metadata, so registry-record isolation is achieved with fresh identifiers per run rather than metadata cleanup.
- The startup seed remains useful for ad-hoc manual requests, but the happy-path collection runs do not depend on it.

The collections cover the new standard-style mock APIs only:

- Repo Life Cycle API: `POST /dpps`, `GET /dpps/{dppId}`, `HEAD /dpps/{dppId}`, `GET /dppsByProductId/{productId}`, `GET /dppsByProductIdAndDate/{productId}`, `POST /dppsByProductIds`, `PATCH /dpps/{dppId}`, `DELETE /dpps/{dppId}`, `GET /dpps/{dppId}/events`
- Repo Fine Granular API: `GET/PATCH /dpps/{dppId}/elements/{elementPath}`
- Registry API: `POST /registerDPP` as the main registration operation, `GET /registry/dpps/{registryId}` as an internal/mock lookup helper, and `GET /registry/dpps/by-dpp-id/{dppId}` as an internal/mock lookup helper
- Service health: repo `GET /health` and registry `GET /health`

`HEAD /dpps/{dppId}` is an internal lightweight verification endpoint. It returns 200 when an active DPP exists, 404 when the DPP is missing or soft-deleted, and no DPP payload. The mock registry uses it to verify repo references before storing registry metadata.

`GET /dpps/{dppId}/events` is an internal/mock lifecycle-events endpoint for demo and test visibility. It is not a full Track & Trace API.

The repo `GET /health` endpoint is also demo-local and unwrapped. It exists for quick smoke checks and Postman flow setup, not as part of the reusable repo client contract.

`POST /registerDPP` verifies the referenced DPP with `{repoUrl}/dpps/{dppIdentifier}` using HEAD before storing metadata. The registry does not fetch full DPP JSON and does not run SDK validation; it trusts the repo as the validated DPP store. A missing DPP fails with `ClientErrorResourceNotFound`, while an unreachable or failing repo returns `ServerErrorBadGateway`.

## Mocked And Not Production-Ready

Mocked:

- EU registry behavior
- DPP repo behavior
- Persistence, using in-memory stores only

Not implemented:

- Real EU registry integration
- Real database
- Authentication or OAuth
- Event streaming
- Production resilience, retries, auditing, or monitoring
- AAS adapter
- Backup identifier/operator/provider placeholders
- Full official draft data-model field coverage

## Current Mock Boundary

- The repo service now implements the standard-style Life Cycle API and simplified Fine Granular API.
- The registry service now implements `POST /registerDPP` plus two internal lookup endpoints for test/debug convenience.
- Registry registration verifies the repo reference before storing metadata; the client/demo still orchestrates create-then-register.
- Responses use the upstream split `DppApiResponse` wrappers with correlation IDs in error messages.
- Storage is intentionally in-memory only for this phase.
- These mocks are internal use ready at the API and test level, but they are not production-secure.

## Future Work

- Upstream split payload modules now own the shared repo and registry request/response contracts used by the mock services.
- This repo keeps only demo-local DTOs that are not provided upstream, such as health payloads and internal mock registry lookup payloads.
- If the internal mock lookup surface grows further, extract those mock-only DTOs into a neutral demo contract module instead of rebuilding client behavior locally.
