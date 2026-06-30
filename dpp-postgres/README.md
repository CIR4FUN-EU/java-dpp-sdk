# DPP PostgreSQL

Optional PostgreSQL-first persistence support for canonical `Dpp4Fun` objects.

This module does not own the mock EU registry PostgreSQL backend. The registry's optional PostgreSQL mode is a demo-local persistence seam inside `dpp-sdk-demo/mock-eu-registry`.

This top-level module is useful when an application wants durable relational persistence. It is not required if you only need:

- models/builders
- semantic validation
- JSON serialization/deserialization

Those remain available in `dpp-datamodel`.

To use this module at runtime, you need a reachable PostgreSQL database. It does not start PostgreSQL for you.

## Module Structure

- `dpp-postgres-core`
  - reusable PostgreSQL support for DPP identity, versioning, `DppCore`, lifecycle events, and paging
- `dpp4fun-postgres`
  - `Dpp4Fun`-specific relational mapping, repository operations, history lookup, lifecycle events, batch lookup, and search

## What It Does

- stores `Dpp4Fun` relationally in PostgreSQL
- reads PostgreSQL rows back into canonical `Dpp4Fun` objects
- supports versioned snapshots
- supports historical lookup by product id and timestamp
- supports soft delete while preserving history
- stores simple lifecycle/audit events
- supports batch product-id lookup and simple search projections

## What It Does Not Do

- no semantic validation
- no JSON Merge Patch
- no fine-granular element-path logic
- no hashes
- no blockchain
- no full Track & Trace lifecycle
- no JSONB full-document source of truth

## How The Data Is Formatted

This module stores `Dpp4Fun` in a normalized relational PostgreSQL shape, not as one big JSON document.

At a high level:

- `dpp_passports`
  - one logical DPP identity (`dpp_id`, `product_id`, `passport_type`)
- `dpp_versions`
  - one row per stored version of that DPP
- core tables such as `dpp_passport_metadata`, `dpp_nameplates`, `dpp_organizations`, `dpp_contacts`
  - reusable `DppCore` content
- `dpp4fun_*` tables
  - `Dpp4Fun`-specific classification, characteristics, dimensions, features, and bill-of-materials content
- `dpp_lifecycle_events`
  - lightweight audit/lifecycle event history

So the stored shape is relational:

`One passport -> many versions -> one version joins to many core rows and many dpp4fun rows`

If you want the exact table layout, see:

- `dpp-postgres-core/src/main/resources/postgres/core-schema.sql`
- `dpp4fun-postgres/src/main/resources/postgres/dpp4fun-schema.sql`

## Schema Map

This section links the main PostgreSQL tables to the Java classes that write or read them.

Core/versioning tables:

- `dpp_passports`
  - used by `PostgresDppVersionRepositorySupport`
- `dpp_versions`
  - used by `PostgresDppVersionRepositorySupport`
- `dpp_lifecycle_events`
  - used by `PostgresLifecycleEventRepository`

Reusable `DppCore` tables:

- `dpp_passport_metadata`
  - used by `PostgresDppCoreMapper`
- `dpp_passport_update_dates`
  - used by `PostgresDppCoreMapper`
- `dpp_nameplates`
  - used by `PostgresDppCoreMapper`
- `dpp_organizations`
  - used by `PostgresDppCoreMapper`
- `dpp_contacts`
  - used by `PostgresDppCoreMapper`
- `dpp_addresses`
  - used by `PostgresDppCoreMapper`
- `dpp_emails`
  - used by `PostgresDppCoreMapper`
- `dpp_telephones`
  - used by `PostgresDppCoreMapper`
- `dpp_documentation`
  - used by `PostgresDppCoreMapper`

`Dpp4Fun`-specific tables:

- `dpp4fun_classifications`
  - used by `Dpp4FunPostgresMapper`
- `dpp4fun_classification_tags`
  - used by `Dpp4FunPostgresMapper`
- `dpp4fun_characteristics`
  - used by `Dpp4FunPostgresMapper`
- `dpp4fun_dimensions`
  - used by `Dpp4FunPostgresMapper`
- `dpp4fun_features`
  - used by `Dpp4FunPostgresMapper`
- `dpp4fun_bill_of_materials`
  - used by `Dpp4FunPostgresMapper`
- `dpp4fun_materials`
  - used by `Dpp4FunPostgresMapper`
- `dpp4fun_components`
  - used by `Dpp4FunPostgresMapper`
- `dpp4fun_parts`
  - used by `Dpp4FunPostgresMapper`

Query-oriented code:

- `Dpp4FunQueryRepository`
  - reads from `dpp_passports`, `dpp_versions`, `dpp4fun_classifications`, `dpp4fun_characteristics`, `dpp4fun_materials`, `dpp4fun_components`, and `dpp4fun_parts`
- `Dpp4FunPostgresRepository`
  - is the main orchestration/facade; it delegates actual table reads/writes to `PostgresDppVersionRepositorySupport`, `PostgresDppCoreMapper`, `Dpp4FunPostgresMapper`, `PostgresLifecycleEventRepository`, and `Dpp4FunQueryRepository`

## Build A `Dpp4Fun`

```java
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.PassportMetadata;
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.ProductClassification;

import java.time.LocalDate;
import java.util.UUID;

PassportMetadata metadata = new PassportMetadata.Builder()
        .uniqueProductIdentifier(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        .addPassportUpdateDate(LocalDate.of(2026, 6, 29))
        .qrCodeOrDigitalTag("https://example.com/dpp/11111111-1111-1111-1111-111111111111")
        .build();

Nameplate nameplate = new Nameplate.Builder()
        .gtinCode("04012345678901")
        .build();

DppCore coreDpp = new DppCore.Builder()
        .passportMetadata(metadata)
        .nameplate(nameplate)
        .build();

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
        .coreDpp(coreDpp)
        .classification(classification)
        .characteristics(characteristics)
        .build();
```

## Validate A `Dpp4Fun`

```java
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;

Dpp4FunValidationService validator = new Dpp4FunValidationService();
validator.validate(dpp);
```

Validation stays outside the PostgreSQL repository.

## Serialize And Deserialize

`Dpp4FunPostgresRepository` persists canonical `Dpp4Fun` objects, so the usual SDK JSON codec still fits naturally around it.

```java
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;

Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();

String json = codec.toJson(dpp);
Dpp4Fun parsed = codec.fromJson(json);
```

If you want parse-and-validate in one step:

```java
Dpp4Fun parsedAndValidated = codec.fromJsonAndValidate(json);
```

## Create A Repository

```java
import dppsdk.postgres.dpp4fun.Dpp4FunPostgresRepository;
import org.postgresql.ds.PGSimpleDataSource;

PGSimpleDataSource dataSource = new PGSimpleDataSource();
dataSource.setURL("jdbc:postgresql://localhost:5432/dpp");
dataSource.setUser("postgres");
dataSource.setPassword("postgres");

Dpp4FunPostgresRepository repository = new Dpp4FunPostgresRepository(dataSource);
```

## Persist And Read

```java
import dppsdk.postgres.core.PostgresDppOperationContext;

repository.create(dpp, new PostgresDppOperationContext("create-demo", java.time.Instant.now()));

java.util.Optional<Dpp4Fun> currentByDppId = repository.findCurrentByDppId(dpp.getDppId());
java.util.Optional<Dpp4Fun> currentByProductId = repository.findCurrentByProductId(dpp.getProductId());
boolean exists = repository.existsActiveByDppId(dpp.getDppId());
```

## Append A New Version

```java
Dpp4Fun updated = dpp.toBuilder()
        .characteristics(dpp.getCharacteristics().toBuilder()
                .productName("Cir4Fun Platform Bed - Updated")
                .build())
        .build();

repository.appendVersion(
        updated,
        1L,
        new PostgresDppOperationContext("update-demo", java.time.Instant.now())
);
```

Historical lookup:

```java
java.util.Optional<Dpp4Fun> historical =
        repository.findByProductIdAt(dpp.getProductId(), java.time.Instant.parse("2026-06-29T12:00:00Z"));
```

## Soft Delete

```java
repository.softDelete(dpp.getDppId(), 2L, java.time.Instant.now());
```

This removes the DPP from active lookups but preserves version history and lifecycle events.

## Read Lifecycle Events

```java
import dppsdk.postgres.core.DppLifecycleEventRecord;

java.util.List<DppLifecycleEventRecord> events = repository.findEventsByDppId(dpp.getDppId());
```

The event model is simple mock-compatible audit history, not full Track & Trace.

## Query Support

This module currently supports simple repository-style queries, not a general complex-query engine.

Supported query patterns:

- current lookup by `dppId`
- current lookup by `productId`
- historical lookup by `productId` and timestamp
- version history by `dppId`
- lifecycle events by `dppId`
- batch lookup of active `dppId`s by product-id list
- simple filtered `Dpp4Fun` search projections

The current `Dpp4Fun` search supports exact-match filters for:

- `sector`
- `category`
- `brand`
- `productType`
- `materialName`
- `componentName`
- `partName`

with `limit` and `offset` paging.

Example:

```java
List<Dpp4FunSearchResult> results = repository.search(
        new Dpp4FunSearchCriteria(
                "Furniture",
                "Beds",
                null,
                null,
                null,
                null,
                null,
                10,
                0
        )
);
```

What it does not currently provide:

- arbitrary boolean query composition
- range queries
- fuzzy/full-text search
- dynamic sorting
- analytics/aggregation query APIs
- JSON-path querying

## Build And Test

From the repository root.

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -pl dpp-postgres/dpp4fun-postgres,dpp-sdk-demo/mock-dpp-repo,dpp-sdk-demo/mock-eu-registry -am test
```

Linux/macOS:

```bash
./mvnw test
./mvnw -pl dpp-postgres/dpp4fun-postgres,dpp-sdk-demo/mock-dpp-repo,dpp-sdk-demo/mock-eu-registry -am test
```

## Docker / Testcontainers Note

The PostgreSQL integration tests use Testcontainers. In environments without Docker, those tests are skipped through `@Testcontainers(disabledWithoutDocker = true)`.

That means:

- normal compilation still runs
- non-containerized tests still run
- one Docker-enabled verification pass is still recommended before merge or release
