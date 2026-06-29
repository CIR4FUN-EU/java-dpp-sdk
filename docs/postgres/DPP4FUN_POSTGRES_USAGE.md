# DPP4Fun PostgreSQL Usage

## Purpose

`dpp-postgres` adds a PostgreSQL-first relational persistence path for canonical `Dpp4Fun` objects.

It is split into:

- `dpp-postgres-core`
- `dpp4fun-postgres`

The repository stores relational snapshot rows by DPP version. It does not use JSONB as the source of truth for the DPP itself.

## What It Does

`Dpp4FunPostgresRepository` supports:

- create
- append version
- current read by `dppId`
- current read by `productId`
- historical read by `productId` and timestamp
- active existence checks
- soft delete
- lifecycle event read/write
- batch active DPP ID lookup by product IDs
- simple projection search

## What It Does Not Do

This module intentionally does not add:

- a generic database abstraction
- a service layer
- mock-repo integration
- patch application
- JSON codec behavior
- semantic validation
- hashes
- blockchain or anchoring logic
- Track & Trace lifecycle

## Validation Responsibility

The repository assumes incoming `Dpp4Fun` instances are already validated.

Semantic validation stays in existing modules:

- `dppsdk.core.validation.ValidationService`
- `dppsdk.dpp4fun.validation.Dpp4FunValidationService`

The PostgreSQL layer only enforces storage integrity through schema constraints such as:

- required root IDs
- foreign keys
- status check constraints
- `version_no > 0`
- unique `dpp_id`
- unique active `product_id`

## JSON Responsibility

Canonical JSON handling remains outside this module in the existing codec modules.

JSONB is used only for lifecycle-event payload data.

The DPP itself is stored in relational tables.

## Repository Construction

```java
import dppsdk.postgres.dpp4fun.Dpp4FunPostgresRepository;
import org.postgresql.ds.PGSimpleDataSource;

PGSimpleDataSource dataSource = new PGSimpleDataSource();
dataSource.setURL("jdbc:postgresql://localhost:5432/dpp");
dataSource.setUser("postgres");
dataSource.setPassword("postgres");

Dpp4FunPostgresRepository repository = new Dpp4FunPostgresRepository(dataSource);
```

The constructor initializes the core and Dpp4Fun schema if the tables do not exist yet.

## Create

```java
import dppsdk.postgres.core.PostgresDppOperationContext;

repository.create(
        validatedDpp,
        new PostgresDppOperationContext("create-op-1", Instant.now())
);
```

Behavior:

- inserts `dpp_passports`
- inserts version `1` as `ACTIVE`
- inserts `DppCore` rows
- inserts Dpp4Fun rows
- records `DPP_CREATED`

## Read

```java
Optional<Dpp4Fun> byDppId = repository.findCurrentByDppId(dppId);
Optional<Dpp4Fun> byProductId = repository.findCurrentByProductId(productId);
boolean exists = repository.existsActiveByDppId(dppId);
```

Historical read:

```java
Optional<Dpp4Fun> historical = repository.findByProductIdAt(productId, instant);
```

## Update By Appending a Version

```java
repository.appendVersion(
        updatedValidatedDpp,
        1L,
        new PostgresDppOperationContext("update-op-2", Instant.now())
);
```

Behavior:

- checks the expected active version number
- marks the old active version `SUPERSEDED`
- inserts a new `ACTIVE` version
- inserts a fresh relational snapshot for that version
- records `DPP_UPDATED`

## Soft Delete

```java
repository.softDelete(dppId, 2L, Instant.now());
```

Behavior:

- checks the expected active version number
- marks the active version `DELETED`
- removes the DPP from active lookup
- preserves history
- preserves lifecycle events
- records `DPP_DELETED`

## Lifecycle Events

The repository stores append-only audit events with:

- `eventId`
- `dppId`
- `eventType`
- `occurredAt`
- `data`

Read:

```java
List<DppLifecycleEventRecord> events = repository.findEventsByDppId(dppId);
```

Write an explicit event:

```java
repository.recordLifecycleEvent(
        dppId,
        DppLifecycleEventType.DATA_ELEMENT_UPDATED,
        Instant.now(),
        Map.of("elementPath", "characteristics.productName")
);
```

## Batch Lookup

```java
DppPage<String> page = repository.findActiveDppIdsByProductIds(
        productIds,
        new DppPageRequest(null, 50)
);
```

The cursor is string-based and currently uses integer offsets, matching the mock repository paging style.

## Search

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
                25,
                0
        )
);
```

Supported filters:

- `sector`
- `category`
- `brand`
- `productType`
- `materialName`
- `componentName`
- `partName`
- `limit`
- `offset`

Search returns projections, not reconstructed full DPP aggregates.

## Reuse Pattern For Future DPP Types

If another DPP type is added later, reuse should follow the same split:

- keep generic passport/version/core/event support in `dpp-postgres-core`
- add a type-specific mapper and repository module beside `dpp4fun-postgres`

Do not move persistence logic into the datamodel classes.
