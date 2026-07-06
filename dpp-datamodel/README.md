# DPP Data Model

## Purpose

`dpp-datamodel` is the Java SDK area for building, validating, mapping, and serializing DPP domain objects. It does not own HTTP clients, mock services, or database persistence.

Parent coordinates from `dpp-datamodel/pom.xml`:

- `groupId`: `dpp.datamodel`
- `artifactId`: `dpp-datamodel`
- `version`: `0.4.0`
- packaging: `pom`

## Module Map

| Module | Coordinates | What it provides |
| --- | --- | --- |
| `dpp-core` | `dpp.datamodel:dpp-core:0.4.0` | Reusable core models, validators, payload DTOs, mappers, identifier helpers |
| `dpp4fun` | `dpp.datamodel:dpp4fun:0.4.0` | Furniture-specific models, validators, payload DTOs, mappers, JSON codec |

## Build And Install

Run from `dpp-datamodel`.

Build all datamodel modules:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean install
```

```bash
./mvnw test
./mvnw clean install
```

Build only `dpp-core`:

```powershell
.\mvnw.cmd -pl "dpp-core" -am test
```

```bash
./mvnw -pl "dpp-core" -am test
```

Build only `dpp4fun`:

```powershell
.\mvnw.cmd -pl "dpp4fun" -am test
```

```bash
./mvnw -pl "dpp4fun" -am test
```

## Maven Consumption

Consume `dpp-core` when you only need the reusable core layer:

```xml
<dependency>
    <groupId>dpp.datamodel</groupId>
    <artifactId>dpp-core</artifactId>
    <version>0.4.0</version>
</dependency>
```

Consume `dpp4fun` when you need the furniture-specific aggregate on top of `dpp-core`:

```xml
<dependency>
    <groupId>dpp.datamodel</groupId>
    <artifactId>dpp4fun</artifactId>
    <version>0.4.0</version>
</dependency>
```

## Main Entry Points

- `dppsdk.core.model.Dpp`
- `dppsdk.core.model.DppCore`
- `dppsdk.core.validation.ValidationService`
- `dppsdk.core.validation.DppCoreValidator`
- `dppsdk.core.util.DppIdentifiers`
- `dppsdk.core.mapper.DppCoreMapper`
- `dppsdk.dpp4fun.model.Dpp4Fun`
- `dppsdk.dpp4fun.validation.Dpp4FunValidationService`
- `dppsdk.dpp4fun.mapper.Dpp4FunMapper`
- `dppsdk.dpp4fun.transport.Dpp4FunJsonCodec`

## Package Responsibilities

- `dppsdk.core.model`: reusable immutable core domain objects and builders
- `dppsdk.core.validation`: core semantic validation and the core-safe `ValidationService`
- `dppsdk.core.mapper`: domain-to-payload conversion for the core layer
- `dppsdk.core.payload`: transport-oriented DTOs for reusable core concepts
- `dppsdk.dpp4fun.model`: furniture-specific domain objects headed by `Dpp4Fun`
- `dppsdk.dpp4fun.validation`: furniture-specific validators and `Dpp4FunValidationService`
- `dppsdk.dpp4fun.mapper`: furniture-specific mapping on top of the core layer
- `dppsdk.dpp4fun.payload`: furniture-specific transport DTOs
- `dppsdk.dpp4fun.transport`: JSON transport entry points such as `Dpp4FunJsonCodec`

## Usage

### Build Reusable Core DPP Structure

```java
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;

import java.time.LocalDate;
import java.util.UUID;

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

DppCore core = new DppCore.Builder()
        .passportMetadata(metadata)
        .nameplate(nameplate)
        .build();
```

### Build A Full `Dpp4Fun`

```java
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.ProductClassification;

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

Dpp4Fun dpp = new Dpp4Fun.Builder()
        .coreDpp(core)
        .classification(classification)
        .characteristics(characteristics)
        .build();
```

### Validate

Core validation:

```java
import dppsdk.core.validation.ValidationService;

ValidationService coreValidation = new ValidationService();
coreValidation.validate(core);
```

`Dpp4Fun` validation:

```java
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;

Dpp4FunValidationService validation = new Dpp4FunValidationService();
validation.validate(dpp);
```

### Map Domain Objects To Payload DTOs

```java
import dppsdk.dpp4fun.mapper.Dpp4FunMapper;
import dppsdk.dpp4fun.payload.Dpp4FunPayload;

Dpp4FunMapper mapper = new Dpp4FunMapper();
Dpp4FunPayload payload = mapper.toPayload(dpp);
Dpp4Fun roundTripped = mapper.toDomain(payload);
```

### Serialize And Deserialize JSON

`Dpp4FunJsonCodec` flattens `passportMetadata`, `nameplate`, and `documentation` for transport, while still accepting both nested and flat inbound shapes.

```java
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;

Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
String json = codec.toJson(dpp);
Dpp4Fun parsed = codec.fromJson(json);
Dpp4Fun parsedAndValidated = codec.fromJsonAndValidate(json);
```

### Immutable Edit / Copy Pattern

The model layer supports `toBuilder()` on the domain objects.

```java
Dpp4Fun updated = dpp.toBuilder()
        .characteristics(dpp.getCharacteristics().toBuilder()
                .productName("Cir4Fun Platform Bed - Updated")
                .build())
        .build();
```

### Extract DPP And Product Identifiers

```java
import dppsdk.core.util.DppIdentifiers;

String dppId = DppIdentifiers.dppId(dpp);
String productId = DppIdentifiers.productId(dpp);
```

## Recommended Usage Pattern

1. Build the domain object with the model builders.
2. Validate it with `ValidationService` or `Dpp4FunValidationService`, depending on scope.
3. Use `Dpp4FunMapper` when you need payload DTO conversion.
4. Use `Dpp4FunJsonCodec` when you need JSON transport conversion.

## What This Module Does Not Provide

- no HTTP endpoints or HTTP client behavior
- no registry submission logic
- no database or persistence implementation
- no mock-service or demo runtime orchestration
- no production security, compliance, certification, or regulatory-completeness claim

## Related Docs

- [`MODEL_GUIDE.md`](MODEL_GUIDE.md): consolidated model structure and validation reference
