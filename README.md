# DPP Backend Demo

Partner-facing Java backend demo showing how `dpp-sdk` and the simplified fixed-endpoint `dpp-client` work together.

Use this file as the quickstart for building and running the demo. Use `DEMO_GUIDE.md` as the live-demo script and talking-points guide.

## Purpose

This repo exists to show the reuse boundary between the SDK, the HTTP client library, and a small partner-facing backend demo:

- `dpp-sdk` owns DPP models, builders, validation, payload mapping, and JSON transport.
- `dpp-client` owns generic fixed-endpoint HTTP clients and client-side error separation.
- This repo adds only Cir4Fun demo fixtures, adapters, mock services, runners, and Postman collections.

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
- Use `HttpDppRepoClient` and `HttpDppRegistryClient` against mock HTTP services.
- Store a DPP in the repo service, fetch/update/list/delete it, and register a stored DPP with the registry.
- Demonstrate invalid DPP, malformed JSON, not-found, HTTP, and network error paths.

## Modules

- `dpp-demo-common`: demo DPP factory plus SDK-to-client adapters.
- `mock-eu-registry-service`: mock registry HTTP service on `http://localhost:8081`.
- `mock-dpp-repo-service`: mock repo HTTP service on `http://localhost:8082`.
- `dpp-producer-service`: command-line SDK and HTTP demo runner.

## Prerequisites

Install the reusable libraries locally first. Update the paths if your `dpp-sdk` or `dpp-client` checkouts live elsewhere.

```powershell
cd C:\Users\yah70309\Desktop\DPP_SDK
.\mvnw.cmd clean install

cd C:\Users\yah70309\Desktop\clients\dpp-client
.\mvnw.cmd clean install
```

## Build

Windows:

```powershell
cd C:\Users\yah70309\Desktop\demo
mvnw.cmd clean package
```

Linux/macOS:

```bash
./mvnw clean package
```

## Run Services

Start the services in this order before the default HTTP demo flow:

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
2. Run the default mode or `http` when the registry and repo services are already running.
3. Run `all` when you want the SDK walkthrough first and then the HTTP flow.

With registry and repo already running:

```powershell
java -jar dpp-producer-service\target\dpp-producer-service-1.0.0-SNAPSHOT.jar --debug=false
```

The default run is the HTTP end-to-end flow. For a fuller stakeholder demo, run SDK capability checks first and then reuse those same SDK-built DPPs in the HTTP flow:

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
java -jar dpp-producer-service\target\dpp-producer-service-1.0.0-SNAPSHOT.jar http http://localhost:8081 http://localhost:8082 --debug=false
```

The producer uses only public `HttpDppRepoClient` and `HttpDppRegistryClient` APIs. The clients use fixed endpoints.

Successful runs print step-by-step console output for the selected flow and finish without stack traces.

## Postman

Import:

- `postman/dpp_registry_service.postman_collection.json`
- `postman/dpp_repo_service.postman_collection.json`

Base URLs:

- Registry: `http://localhost:8081`
- Repo: `http://localhost:8082`

The collections include valid DPP flows, invalid DPP flows, malformed JSON, not-found cases, and repo-to-registry registration. The repo-to-registry registration is demonstrated in Postman through `Register stored DPP through repo`.

## Mocked And Not Production-Ready

Mocked:

- EU registry behavior
- DPP repo behavior
- Persistence, using in-memory maps

Not implemented:

- Real EU registry integration
- Real database
- Production resilience, retries, auditing, or monitoring
