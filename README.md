# DPP SDK

This repository is a v1 demo and reference Java SDK for a Digital Product Passport
(DPP) flow in the Cir4Fun furniture domain.

It is intentionally small and library-friendly:
- immutable domain models live in `dppsdk.model`
- semantic validation lives in `dppsdk.validation`
- domain/payload mapping lives in `dppsdk.mapper`
- JSON transport lives in `dppsdk.transport`
- demo runner code lives under `src/test/java/dppsdk/demo`

## What The SDK Supports

The current SDK lets you:
- build DPP domain objects with builders
- validate individual models or a full DPP aggregate
- map between domain objects and transport payloads
- serialize and deserialize JSON through the transport codec

This is still a v1/demo/reference SDK, so it is not a production release pipeline or
an exhaustive regulatory implementation.

## Project Structure

```text
src/main/java/dppsdk/model       immutable domain model and builders
src/main/java/dppsdk/validation  typed validators and ValidationService
src/main/java/dppsdk/mapper      domain <-> payload mappers
src/main/java/dppsdk/payload     JSON-friendly transport payload classes
src/main/java/dppsdk/transport   DppJsonCodec JSON entry point
src/test/java/dppsdk/demo        example/demo code for local reference
src/test/java/dppsdk/...         automated tests
```

## Maven Coordinates

```xml
<dependency>
    <groupId>com.example.dppsdk</groupId>
    <artifactId>dpp-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Clone The Repo

```bash
git clone <repo-url>
cd DPP_SDK
```

## Build And Install Locally

```bash
.\mvnw.cmd clean install
```

That command:
- compiles the SDK
- runs the test suite
- packages the JAR
- installs the artifact into your local Maven repository
- installs a test-support JAR with the `tests` classifier

You do not need Maven installed globally if you use the wrapper. On the same
machine, another Maven project can then depend on this SDK version without
publishing it anywhere.

## Run Tests

```bash
.\mvnw.cmd test
```

## Demo Scope

The SDK demo classes live under `src/test/java/dppsdk/demo`.

They are reference/demo code only:
- they are not packaged into the published SDK jar
- they are meant to show usage patterns, not provide the main consumer API

Reusable test support such as `dppsdk.support.TestDataFactory` is published
separately in the `dpp-sdk` test JAR and can be consumed with the `tests`
classifier.

## Use It From Another Maven Project

Add the dependency above to the consumer project `pom.xml`, then import the SDK
classes you need.

If a consumer test needs `dppsdk.support.TestDataFactory`, add this additional
test-scope dependency:

```xml
<dependency>
    <groupId>com.example.dppsdk</groupId>
    <artifactId>dpp-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <classifier>tests</classifier>
    <scope>test</scope>
</dependency>
```

The most common entry points are:
- `dppsdk.model.*` builders for manual object construction
- `dppsdk.validation.ValidationService` for validation
- `dppsdk.mapper.Cir4FunFurnitureDppMapper` for explicit mapping
- `dppsdk.transport.DppJsonCodec` for JSON send/receive
- `dppsdk.support.TestDataFactory` from the `tests` classifier for reusable valid test data

## Quick Usage

### Build A DPP

```java
Cir4FunFurnitureDpp dpp = new Cir4FunFurnitureDpp.Builder()
        .passportMetadata(new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.randomUUID())
                .addPassportUpdateDate(LocalDate.now())
                .build())
        .classification(new ProductClassification.Builder()
                .sector("Furniture")
                .category("Beds")
                .build())
        .characteristics(new Characteristics.Builder()
                .productName("Demo Bed")
                .productType("Bed")
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

### Validate A DPP

```java
ValidationService validationService = new ValidationService();
validationService.validate(dpp);
```

### Serialize And Deserialize JSON

```java
DppJsonCodec codec = new DppJsonCodec();
String json = codec.toJson(dpp);
Cir4FunFurnitureDpp parsed = codec.fromJsonAndValidate(json);
```

### Map Explicitly

```java
Cir4FunFurnitureDppMapper mapper = new Cir4FunFurnitureDppMapper();
Cir4FunFurnitureDppPayload payload = mapper.toPayload(dpp);
Cir4FunFurnitureDpp domain = mapper.toDomain(payload);
```

## Notes

- Builders enforce basic structural integrity.
- Validators enforce semantic business rules.
- Payload classes are transport-only and are not the main consumer API.
- The demo runner exists as reference code in test scope, not as the library center.
