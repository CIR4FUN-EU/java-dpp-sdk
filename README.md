# DPP SDK

Java reference SDK for a Digital Product Passport (DPP) data model in the Cir4Fun furniture domain.

This repository currently contains:
- immutable domain models in `dppsdk.model`
- validators in `dppsdk.validation`
- domain/payload mappers in `dppsdk.mapper`
- JSON transport through `dppsdk.transport.DppJsonCodec`
- automated tests and fixtures under `src/test/java`

This is a v1 reference SDK. It does not implement persistence, security, registry submission, external API checks, or full regulatory completeness.

## Project Structure

```text
src/main/java/dppsdk/model       immutable domain model and builders
src/main/java/dppsdk/validation  typed validators and ValidationService
src/main/java/dppsdk/mapper      domain <-> payload mappers
src/main/java/dppsdk/payload     JSON-friendly payload classes
src/main/java/dppsdk/transport   DppJsonCodec JSON entry point
src/test/java/dppsdk/...         automated tests and test fixtures
```

## Build And Test

```bash
.\mvnw.cmd test
.\mvnw.cmd package
```

## Main Entry Points

- `dppsdk.model.Cir4FunFurnitureDpp`
- `dppsdk.model.DppCore`
- `dppsdk.validation.ValidationService`
- `dppsdk.mapper.Cir4FunFurnitureDppMapper`
- `dppsdk.transport.DppJsonCodec`

`Cir4FunFurnitureDpp` owns a `DppCore` plus Cir4Fun-specific classification,
characteristics, and bill-of-materials data. Common fields can be read through
convenience getters such as `getUniqueProductIdentifier()`, `getGtinCode()`,
and `getProductName()` without navigating every submodel manually.

`dppsdk.mapper.Cir4FunFurnitureDppMapper` handles domain/payload conversion
when you need the payload object directly. `DppJsonCodec` sits one layer above
that and is the easiest path when you want JSON in and JSON out.

The payload layer now uses the same canonical nesting in Java, including
`coreDpp`, while `DppJsonCodec` still emits and accepts the legacy flat JSON
shape for compatibility.

## Documentation

- `SDK_USAGE.md`: how to construct, validate, map, and serialize a DPP
- `MODEL_GUIDE.md`: current model structure
- `VALIDATION_GUIDE.md`: validation architecture
- `VALIDATION_RULES.md`: explicit validation rules implemented in v1
- `LOCAL_CONSUMPTION.md`: using this SDK from another local Maven project
- `DPP_SDK_OVERVIEW.md`: short scope and non-goals summary
