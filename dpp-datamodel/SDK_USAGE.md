# SDK Usage Guide

This guide covers the main entry points in the split `dpp-core` / `dpp4fun` SDK.

## Main Entry Points

### Core

- `dppsdk.core.model.Dpp`
- `dppsdk.core.model.DppCore`
- `dppsdk.core.validation.ValidationService`
- `dppsdk.core.mapper.Mapper`
- `dppsdk.core.util.DppIdentifiers`

### Dpp4Fun

- `dppsdk.dpp4fun.model.Dpp4Fun`
- `dppsdk.dpp4fun.validation.Dpp4FunValidationService`
- `dppsdk.dpp4fun.mapper.Dpp4FunMapper`
- `dppsdk.dpp4fun.payload.Dpp4FunPayload`
- `dppsdk.dpp4fun.transport.Dpp4FunJsonCodec`

## Package Responsibilities

### `dppsdk.core.model`

Immutable reusable DPP domain objects and builders.

### `dppsdk.core.validation`

Core semantic validation rules and the core-safe `ValidationService`.

### `dppsdk.core.mapper`

Field-by-field conversion between core domain objects and core payload objects.

### `dppsdk.core.payload`

Transport-friendly DTOs for reusable DPP core concepts.

### `dppsdk.dpp4fun.model`

Furniture-specific domain objects headed by `Dpp4Fun`.

### `dppsdk.dpp4fun.validation`

Furniture-specific validators and `Dpp4FunValidationService`.

### `dppsdk.dpp4fun.mapper`

Furniture-specific mapping on top of the core layer.

### `dppsdk.dpp4fun.payload`

Furniture-specific transport DTOs.

### `dppsdk.dpp4fun.transport`

High-level JSON transport entry points. `Dpp4FunJsonCodec` is the simplest way to serialize or parse a DPP document.

## Example Flow

### 1. Build Shared Core Data

```java
DppCore core = new DppCore.Builder()
        .passportMetadata(new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.randomUUID())
                .addPassportUpdateDate(LocalDate.now())
                .build())
        .nameplate(new Nameplate.Builder()
                .gtinCode("GTIN-DEMO-001")
                .manufacturer(new Organization.Builder()
                        .name("Demo Manufacturer GmbH")
                        .role(OrganizationRole.MANUFACTURER)
                        .build())
                .build())
        .build();
```

### 2. Build The Furniture Aggregate

```java
Dpp4Fun dpp = new Dpp4Fun.Builder()
        .coreDpp(core)
        .classification(new ProductClassification.Builder()
                .sector("Furniture")
                .category("Beds")
                .build())
        .characteristics(new Characteristics.Builder()
                .productName("Demo Bed")
                .productType("Bed")
                .build())
        .build();
```

For common reads, use aggregate convenience getters:

```java
UUID dppId = dpp.getUniqueProductIdentifier();
String gtin = dpp.getGtinCode();
String productName = dpp.getProductName();
String category = dpp.getCategory();
```

### 3. Validation

Core-only validation:

```java
ValidationService coreValidation = new ValidationService();
coreValidation.validate(core);
```

Furniture aggregate validation:

```java
Dpp4FunValidationService validationService = new Dpp4FunValidationService();
validationService.validate(dpp);
```

### 4. Editing Existing Objects

Models remain immutable. Use `toBuilder()` to create modified copies:

```java
Dpp4Fun renamed = dpp.toBuilder()
        .characteristics(dpp.getCharacteristics().toBuilder()
                .productName("Updated Bed")
                .build())
        .build();
```

### 5. Explicit Mapping

```java
Dpp4FunMapper mapper = new Dpp4FunMapper();
Dpp4FunPayload payload = mapper.toPayload(dpp);
DppCorePayload corePayload = payload.getCoreDpp();
Dpp4Fun roundTripped = mapper.toDomain(payload);
```

### 6. JSON Inbound / Outbound

```java
Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();

String json = codec.toJson(dpp);
Dpp4Fun parsed = codec.fromJson(json);
Dpp4Fun parsedAndValidated = codec.fromJsonAndValidate(json);
```

`Dpp4FunJsonCodec` keeps the current transport behavior:

- payloads remain nested under `coreDpp` in Java
- outbound JSON is flattened for compatibility
- inbound JSON accepts flat or nested shapes

## Identifiers Used By Standard APIs

- DPP ID maps to `PassportMetadata.uniqueProductIdentifier`.
- Product ID maps to `Nameplate.gtinCode`.
- Use `dpp.getDppId()`, `dpp.getProductId()`, `DppIdentifiers.dppId(dpp)`, or `DppIdentifiers.productId(dpp)` when callers need stable identifiers.

## Recommended Consumer Pattern

1. Build the domain object with the model builders.
2. Validate with `ValidationService` or `Dpp4FunValidationService`, depending on scope.
3. Use `Dpp4FunMapper` or `Dpp4FunJsonCodec` only when transport conversion is needed.
