# DPP Data Model

Java reference SDK for a Digital Product Passport (DPP) data model split into two Maven modules:

- `dpp-core`
- `dpp4fun`

The root project is the parent/aggregator artifact `com.example.dppsdk:dpp-datamodel:0.3.0`.

## Modules

### `dpp-core`

Owns reusable DPP core concepts:

- immutable core domain models in `dppsdk.core.model`
- core validators in `dppsdk.core.validation`
- core payload DTOs in `dppsdk.core.payload`
- core mappers in `dppsdk.core.mapper`
- shared utilities such as `dppsdk.core.util.DppIdentifiers`

### `dpp4fun`

Depends on `dpp-core` and owns the furniture-specific extension:

- furniture aggregate and submodels in `dppsdk.dpp4fun.model`
- Dpp4Fun validators in `dppsdk.dpp4fun.validation`
- Dpp4Fun payload DTOs in `dppsdk.dpp4fun.payload`
- Dpp4Fun mappers in `dppsdk.dpp4fun.mapper`
- JSON transport in `dppsdk.dpp4fun.transport.Dpp4FunJsonCodec`

`Dpp4Fun` owns a `DppCore` plus furniture-specific classification, characteristics, and bill-of-materials data.

## Build And Test

```powershell
# Windows
.\mvnw.cmd validate
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

```powershell
# Linux/MacOS
./mvnw validate
./mvnw test
./mvnw clean verify
```

## Main Entry Points

- `dppsdk.core.model.DppCore`
- `dppsdk.core.validation.ValidationService`
- `dppsdk.core.util.DppIdentifiers`
- `dppsdk.dpp4fun.model.Dpp4Fun`
- `dppsdk.dpp4fun.mapper.Dpp4FunMapper`
- `dppsdk.dpp4fun.transport.Dpp4FunJsonCodec`

`Dpp4FunJsonCodec` preserves the current JSON compatibility behavior:

- canonical Java payload structure remains nested under `coreDpp`
- outbound JSON still flattens `passportMetadata`, `nameplate`, and `documentation`
- inbound JSON accepts both nested and flat shapes

## Payloads And Mappers

Payloads and mappers are intentionally kept for now.

- They preserve the current JSON behavior and mapping boundary.
- They avoid mixing the module/naming refactor with a transport redesign.
- They still provide the right place for UUID/date/enum string conversion and compatibility handling.

Possible later options are:

1. keep the current split as-is
2. partially simplify boilerplate leaf DTOs and mappers after behavior-preserving tests
3. run a direct-domain codec spike and keep it only if it is simpler overall
4. add versioned official-model payloads if the external partner model diverges
5. add a separate AAS adapter later if AAS export is required

## Scope And Non-Goals

This SDK does not currently implement:

- persistence or database integration
- registry submission or generic HTTP client behavior
- authentication, authorization, retries, caching, or async orchestration
- full regulatory completeness or the full draft data model
- AAS export or adapter support
- backup-related fields

## Documentation

- `DPP_SDK_OVERVIEW.md`: short repository scope and non-goals
- `SDK_USAGE.md`: construction, validation, mapping, and JSON usage
- `MODEL_GUIDE.md`: current model structure
- `VALIDATION_GUIDE.md`: validation architecture
- `VALIDATION_RULES.md`: explicit validation rules implemented in v1
- `LOCAL_CONSUMPTION.md`: consuming `dpp-core` and `dpp4fun` from another local Maven project
