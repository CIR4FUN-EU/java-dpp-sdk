# DPP Backend Demo

Partner-facing Java backend demo showing how `dpp-sdk` and the standard-style `dpp-client` work together.

Use this file as the quickstart for building and running the demo. Use `DEMO_GUIDE.md` as the live-demo script and talking-points guide.

## Purpose

This repo exists to show the reuse boundary between the SDK, the HTTP client library, and a small partner-facing backend demo:

- `dpp-sdk` owns DPP models, builders, validation, payload mapping, and JSON transport.
- `dpp-client` owns generic HTTP clients for the standard-style DPP APIs and client-side error separation.
- This repo adds only Cir4Fun demo fixtures, adapters, mock services, runners, in-memory stores, tests, and Postman collections.

Runtime truth note:

- The canonical runtime view for this demo is the `demo` repo plus the locally installed `dpp-sdk` and `dpp-client` snapshot artifacts used at build/run time.
- The linked `DPP_SDK` source checkout may lag that installed snapshot. In particular, the installed SDK currently exposes `DppCore`, which this demo uses.

## What It Shows

- Build Cir4Fun DPPs with SDK builders and demo fixtures.
- Show a full DPP assembled step by step by building `DppCore` common fields first, then wrapping it in `Cir4FunFurnitureDpp`.
- Validate DPPs with SDK `ValidationService`.
- Show SDK required-field protection when an incomplete DPP is built.
- Demonstrate immutable edits and deletes with `toBuilder()`.
- Convert DPPs to and from JSON with SDK `DppJsonCodec`.
- Keep the external DTO/JSON payload shape flat while the domain model uses `DppCore`.
- Adapt the SDK type to `dpp-client` with thin `DppCodec<T>` and `DppValidator<T>` adapters.
- Show that the client integration surface stays small: base URL plus codec and validator adapters.
- Use `HttpDppRepoClient` and `HttpDppRegistryClient` against the mock Life Cycle API, simplified Fine Granular API, and Registry API.
- Store a DPP in the repo service, verify it by lightweight HEAD, read it by ID or product ID, patch the full DPP, read and update a single element, query IDs by product IDs, register it with the registry, and soft-delete it.
- Demonstrate invalid DPP, malformed JSON, conflict, not-found, HTTP, and network error paths.

## Modules

- `dpp-demo-common`: demo DPP factory plus SDK-to-client adapters.
- `mock-eu-registry-service`: mock registry HTTP service on `http://localhost:8081`.
- `mock-dpp-repo-service`: mock repo HTTP service on `http://localhost:8080`.
- `dpp-producer-service`: command-line SDK and HTTP demo runner.

## Prerequisites

Install the reusable libraries locally first from your own `dpp-sdk` and `dpp-client` checkouts.

```powershell
cd <path-to-dpp-sdk>
.\mvnw.cmd clean install

cd <path-to-dpp-client>
.\mvnw.cmd clean install
```

## Build

Windows:

```powershell
# Run from the repository root
.\mvnw.cmd clean package
```

Linux/macOS:

```bash
./mvnw clean package
```

Common verification commands on Windows:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
.\mvnw.cmd clean package
```

## Run Services

Start the services in this order before the default standards demo flow:

Start the registry:

```powershell
java -jar mock-eu-registry-service\target\mock-eu-registry-service-1.0.0-SNAPSHOT.jar --debug=false
```

Start the repo:

```powershell
java -jar mock-dpp-repo-service\target\mock-dpp-repo-service-1.0.0-SNAPSHOT.jar --debug=false
```

## Run Demo

Expected flow:

1. Run `sdk` when you want the SDK-only capability walkthrough.
2. Run the default mode, `standards`, or `http` when the registry and repo services are already running.
3. Run `all` when you want the SDK walkthrough first and then the HTTP flow.

With registry and repo already running:

```powershell
java -jar dpp-producer-service\target\dpp-producer-service-1.0.0-SNAPSHOT.jar --debug=false
```

The default run is the standards end-to-end flow. For a fuller stakeholder demo, run SDK capability checks first and then reuse those same SDK-built DPPs in the HTTP flow:

```powershell
java -jar dpp-producer-service\target\dpp-producer-service-1.0.0-SNAPSHOT.jar all --debug=false
```

SDK-only mode does not require the backend services:

```powershell
java -jar dpp-producer-service\target\dpp-producer-service-1.0.0-SNAPSHOT.jar sdk --debug=false
```

The SDK-only mode shows `DppCore` construction, validation, mapper/JSON round trips, and immutable edit/delete examples for characteristics, documentation, and bill of materials entries.

Explicit HTTP-only mode:

```powershell
java -jar dpp-producer-service\target\dpp-producer-service-1.0.0-SNAPSHOT.jar http --debug=false
```

Optional base URL overrides:

```powershell
java -jar dpp-producer-service\target\dpp-producer-service-1.0.0-SNAPSHOT.jar http http://localhost:8091 http://localhost:8090 --debug=false
```

The producer uses public `HttpDppRepoClient` APIs for the Life Cycle and Fine Granular flows and public `HttpDppRegistryClient` APIs for `/registerDPP` plus the internal read-back helpers. The demo creates the DPP in the repo first, then calls the registry; the repo service does not automatically call the registry.

Successful runs print step-by-step console output for the selected flow and finish without stack traces.

## Postman

Import:

- `postman/dpp_lifecycle_api.postman_collection.json`
- `postman/dpp_registry_api.postman_collection.json`
- `postman/dpp_fine_granular_api.postman_collection.json`

Base URLs:

- Registry: `http://localhost:8081`
- Repo: `http://localhost:8080`

The collections cover the new standard-style mock APIs only:

- Repo Life Cycle API: `POST /dpps`, `GET /dpps/{dppId}`, `HEAD /dpps/{dppId}`, `GET /dppsByProductId/{productId}`, `GET /dppsByProductIdAndDate/{productId}`, `POST /dppsByProductIds`, `PATCH /dpps/{dppId}`, `DELETE /dpps/{dppId}`, `GET /dpps/{dppId}/events`
- Repo Fine Granular API: `GET/PATCH /dpps/{dppId}/elements/{elementPath}`
- Registry API:
- `POST /registerDPP` as the main registration operation
- `GET /registry/dpps/{registryId}` as an internal/mock lookup helper
- `GET /registry/dpps/by-dpp-id/{dppId}` as an internal/mock lookup helper
- `GET /health`

`HEAD /dpps/{dppId}` is an internal lightweight verification endpoint. It returns 200 when an active DPP exists, 404 when the DPP is missing or soft-deleted, and no DPP payload. The mock registry uses it to verify repo references before storing registry metadata. This is not the old `/exists` endpoint.

`GET /dpps/{dppId}/events` is an internal/mock lifecycle-events endpoint for demo and test visibility. It is not a full Track & Trace API.

`POST /registerDPP` verifies the referenced DPP with `{repoUrl}/dpps/{dppIdentifier}` using HEAD before storing metadata. The registry does not fetch full DPP JSON and does not run SDK validation; it trusts the repo as the validated DPP store. A missing DPP fails with `ClientErrorResourceNotFound`, while an unreachable or failing repo returns `ServerErrorBadGateway`.

Old `/repo/dpps/...` paths and the old registry registration shape were replaced in this phase.

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

## Current Mock Boundary

- The repo service now implements the standard-style Life Cycle API and simplified Fine Granular API.
- The registry service now implements `POST /registerDPP` plus two internal lookup endpoints for test/debug convenience.
- Registry registration verifies the repo reference before storing metadata; the client/demo still orchestrates create-then-register.
- Responses use the shared `DppApiResponse` wrapper with correlation IDs in error messages.
- Storage is intentionally in-memory only for this phase.
- These mocks are VM-ready at the API and test level, but they are not production-secure.

## Future Work

- `demo.common.api` remains the server-side mock contract for now, while `dpp-client` keeps its own client DTOs.
- The two DTO sets are intentionally kept JSON-contract compatible and are covered by alignment tests in this repo.
- If the shared API surface grows further, extract the common DPP API DTOs into a neutral `dpp-api-contract` module instead of letting the two copies drift independently.
- No `/exists` endpoint, backup provider/operator support, database, auth, or retry framework is part of the current mock implementation.
