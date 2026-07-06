# DPP PostgreSQL

## Purpose

`dpp-postgres` provides PostgreSQL-specific persistence support for canonical `Dpp4Fun` objects. It is optional: consumers that only need models, validation, mapping, JSON, or HTTP clients do not need this module.

Parent coordinates from `dpp-postgres/pom.xml`:

- `groupId`: `com.example.dppsdk`
- `artifactId`: `dpp-postgres`
- `version`: `0.4.0`
- packaging: `pom`

This module does not own the mock registry's PostgreSQL seam. That registry-specific persistence lives inside `dpp-sdk-demo/mock-eu-registry`.

## Module Map

| Module | Coordinates | What it provides |
| --- | --- | --- |
| `dpp-postgres-core` | `com.example.dppsdk:dpp-postgres-core:0.4.0` | Reusable PostgreSQL support for versioning, core mapping, lifecycle events, paging |
| `dpp4fun-postgres` | `com.example.dppsdk:dpp4fun-postgres:0.4.0` | `Dpp4Fun` repository, relational mapping, history lookup, search support |

## Prerequisites

- Java 17
- local Maven wrapper use from this repo
- upstream datamodel artifacts installed when building this subproject in isolation

If you build from the monorepo root, the reactor order already provides what this module needs.

If you build `dpp-postgres` on its own, install the upstream datamodel artifacts first:

```powershell
..\mvnw.cmd -f ..\dpp-datamodel\pom.xml clean install
```

```bash
# Ensure the root wrapper is executable:
chmod +x ../mvnw
../mvnw -f ../dpp-datamodel/pom.xml clean install
```

## Build And Install

Run from `dpp-postgres`.

These commands work directly when invoked through the root reactor, or from this subproject after the prerequisite install above.

Build all PostgreSQL modules:

```powershell
..\mvnw.cmd test
..\mvnw.cmd clean install
```

```bash
# Ensure the root wrapper is executable:
chmod +x ../mvnw
../mvnw test
../mvnw clean install
```

Build only `dpp-postgres-core`:

```powershell
..\mvnw.cmd -pl "dpp-postgres-core" -am test
```

```bash
# Ensure the root wrapper is executable:
chmod +x ../mvnw
../mvnw -pl "dpp-postgres-core" -am test
```

Build only `dpp4fun-postgres`:

```powershell
..\mvnw.cmd -pl "dpp4fun-postgres" -am test
```

```bash
# Ensure the root wrapper is executable:
chmod +x ../mvnw
../mvnw -pl "dpp4fun-postgres" -am test
```

## Maven Consumption

Core PostgreSQL support:

```xml
<dependency>
    <groupId>com.example.dppsdk</groupId>
    <artifactId>dpp-postgres-core</artifactId>
    <version>0.4.0</version>
</dependency>
```

`Dpp4Fun` PostgreSQL repository:

```xml
<dependency>
    <groupId>com.example.dppsdk</groupId>
    <artifactId>dpp4fun-postgres</artifactId>
    <version>0.4.0</version>
</dependency>
```

## Main Entry Points

- `dppsdk.postgres.core.PostgresDppOperationContext`
- `dppsdk.postgres.core.DppLifecycleEventRecord`
- `dppsdk.postgres.core.DppPageRequest`
- `dppsdk.postgres.dpp4fun.Dpp4FunPostgresRepository`
- `dppsdk.postgres.dpp4fun.Dpp4FunSearchCriteria`
- `dppsdk.postgres.dpp4fun.Dpp4FunSearchResult`
- `dppsdk.postgres.dpp4fun.Dpp4FunVersionSummary`

## How Data Is Stored

The PostgreSQL storage model is append-only and version-oriented.

- `dpp_passports` stores the stable DPP identity (`dpp_id`, `product_id`, passport type, timestamps, soft-delete marker)
- `dpp_versions` stores immutable version rows per passport with `ACTIVE`, `SUPERSEDED`, and `DELETED` status values
- core reusable fields are normalized into dedicated tables such as:
  - `dpp_passport_metadata`
  - `dpp_passport_update_dates`
  - `dpp_nameplates`
  - `dpp_documentation`
  - `dpp_organizations`, `dpp_contacts`, `dpp_addresses`, `dpp_emails`, `dpp_telephones`
- `Dpp4Fun`-specific fields are stored in dedicated tables such as:
  - `dpp4fun_classifications`
  - `dpp4fun_classification_tags`
  - `dpp4fun_characteristics`
  - `dpp4fun_dimensions`
  - `dpp4fun_features`
  - `dpp4fun_bill_of_materials`
  - `dpp4fun_materials`
  - `dpp4fun_components`
  - `dpp4fun_parts`

This means the PostgreSQL layer keeps one stable passport identity plus an append-only history of versions, rather than overwriting the same row in place.

## Lifecycle Events

The reusable PostgreSQL core also stores audit-oriented lifecycle events in `dpp_lifecycle_events`.

Each lifecycle event row stores:

- a unique `event_id`
- the related `dpp_id`
- the `event_type`
- `occurred_at`
- a small `jsonb` `data` payload

Current supported event types are:

- `DPP_CREATED`
- `DPP_UPDATED`
- `DATA_ELEMENT_UPDATED`
- `DPP_DELETED`

In Java, these events are exposed through:

- `dppsdk.postgres.core.DppLifecycleEventType`
- `dppsdk.postgres.core.DppLifecycleEventRecord`

The lifecycle-event table is intentionally lightweight and audit-oriented. It is not the canonical source of full DPP content; the full DPP state comes from the normalized passport/version tables.

## Usage

### Create The Repository

```java
import dppsdk.postgres.dpp4fun.Dpp4FunPostgresRepository;
import org.postgresql.ds.PGSimpleDataSource;

PGSimpleDataSource dataSource = new PGSimpleDataSource();
dataSource.setURL("jdbc:postgresql://localhost:5432/dpp");
dataSource.setUser("postgres");
dataSource.setPassword("postgres");

Dpp4FunPostgresRepository repository = new Dpp4FunPostgresRepository(dataSource);
```

### Persist A Validated `Dpp4Fun`

The repository expects already validated domain objects.

```java
import dppsdk.postgres.core.PostgresDppOperationContext;

repository.create(
        dpp,
        new PostgresDppOperationContext("create-demo", java.time.Instant.now())
);
```

### Read Current And Historical Versions

```java
java.util.Optional<Dpp4Fun> byDppId = repository.findCurrentByDppId(dpp.getDppId());
java.util.Optional<Dpp4Fun> byProductId = repository.findCurrentByProductId(dpp.getProductId());
java.util.Optional<Dpp4Fun> historical = repository.findByProductIdAt(
        dpp.getProductId(),
        java.time.Instant.parse("2026-06-29T12:00:00Z")
);
boolean exists = repository.existsActiveByDppId(dpp.getDppId());
```

### Append A New Version

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

### Soft Delete And Events

```java
repository.softDelete(dpp.getDppId(), 2L, java.time.Instant.now());

java.util.List<dppsdk.postgres.core.DppLifecycleEventRecord> events =
        repository.findEventsByDppId(dpp.getDppId());
```

### Search

```java
import dppsdk.postgres.dpp4fun.Dpp4FunSearchCriteria;
import dppsdk.postgres.dpp4fun.Dpp4FunSearchResult;

java.util.List<Dpp4FunSearchResult> results = repository.search(
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

Current exact-match filters are:

- `sector`
- `category`
- `brand`
- `productType`
- `materialName`
- `componentName`
- `partName`

## What This Module Does Not Provide

- no semantic validation layer
- no JSON codec ownership
- no generic HTTP client behavior
- no mock registry PostgreSQL module
- no JSON Merge Patch or fine-granular element-path API
- no production hardening, compliance, or operational guarantees

## Schema References

- [`dpp-postgres-core/src/main/resources/postgres/core-schema.sql`](dpp-postgres-core/src/main/resources/postgres/core-schema.sql)
- [`dpp4fun-postgres/src/main/resources/postgres/dpp4fun-schema.sql`](dpp4fun-postgres/src/main/resources/postgres/dpp4fun-schema.sql)
