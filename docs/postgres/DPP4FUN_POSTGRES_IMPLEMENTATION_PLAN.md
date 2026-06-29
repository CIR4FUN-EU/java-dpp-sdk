# DPP4Fun PostgreSQL Implementation Plan

> Planning-only document based on the current `feat/postgress-implementation` checkout. This plan reflects the code and docs that are actually present now.

## 1. Current module structure

### Monorepo root

- Root aggregator `pom.xml` currently includes:
  - `dpp-datamodel`
  - `dpp-sdk-clients`
  - `dpp-sdk-demo`
- Root aggregator does not currently include `dpp-persistence`.
- Root wrapper files exist: `mvnw`, `mvnw.cmd`, `.mvn/`.
- No repository-local CI pipeline files were found under `.github/`, `.gitlab-ci*`, `Jenkinsfile`, or similar names in this checkout.

### `dpp-datamodel`

- Parent aggregator with:
  - `dpp-core`
  - `dpp4fun`
- `dpp-core` owns reusable model, validation, payload, mapper, and utility code.
- `dpp4fun` depends on `dpp-core` and owns furniture-specific model, validation, payload, mapper, and JSON codec code.

### `dpp-sdk-clients`

- Parent aggregator with:
  - `dpp-repo-payloads`
  - `dpp-repo-client`
  - `dpp-registry-payloads`
  - `dpp-registry-client`
- Repo and registry clients are HTTP-facing and model-independent.

### `dpp-sdk-demo`

- Parent aggregator with:
  - `mock-eu-registry`
  - `mock-dpp-repo`
  - `dpp-integration-demo`
- `mock-dpp-repo` is a Spring Boot module that stores full DPPs in memory and exposes lifecycle, historical, fine-granular, and event endpoints.

### `dpp-persistence`

- The directory exists locally with `AGENTS.md` and `.codex/DPP_PROJECT_RULES.md`.
- In this checkout, no `pom.xml`, `README.md`, `src/main/java`, or `src/test/java` files were found under `dpp-persistence`.
- `git ls-files dpp-persistence` returned no tracked source files in this branch.
- Only `target/` build output trees are visible under the nested `dpp-persistence-*` directories.

### Immediate planning consequence

- The PostgreSQL plan must be based on the current datamodel and mock-repo behavior, not on unseen persistence code from another branch.
- The cleanest current placement is a new top-level sibling project:
  - `dpp-postgres/`

## 2. Current DppCore datamodel summary

Current reusable core model package: `dppsdk.core.model`

### Root types

- `Dpp`
  - abstract base class
  - exposes common convenience getters through `getCoreDpp()`
  - standard API-facing identifiers:
    - DPP ID -> `PassportMetadata.uniqueProductIdentifier`
    - Product ID -> `Nameplate.gtinCode`
- `DppCore`
  - reusable common DPP structure
  - fields:
    - `passportMetadata`
    - `nameplate`
    - `documentation` optional

### Core submodels

- `PassportMetadata`
  - `UUID uniqueProductIdentifier`
  - `List<LocalDate> passportUpdateDates`
  - `String qrCodeOrDigitalTag`
  - `String externalDocumentationLink`
- `Nameplate`
  - `String gtinCode`
  - `String internalArticleNumber`
  - `String batchNumber`
  - `String customsTariffNumber`
  - `String uriOfTheProduct`
  - `Organization manufacturer`
  - `Organization supplier`
- `Documentation`
  - `String digitalInstructionsLink`
  - `String safetyInstructionsLink`
  - `boolean downloadable`
  - `Integer availableForYears`
  - `boolean paperCopyAvailableOnRequest`
- `Organization`
  - `String name`
  - `String gln`
  - product-description fields
  - `String uri`
  - `Contact contact`
  - `OrganizationRole role`
- `Contact`
  - `String organization`
  - `Address address`
  - `Email email`
  - `Telephone telephone`
- `Address`
  - `country`, `zipCode`, `region`, `town`, `street`
- `Email`
  - `emailAddress`, `typeOfEmail`
- `Telephone`
  - `telephoneNumber`, `typeOfTelephone`

### Core model semantics relevant to persistence

- Models are immutable.
- Builders enforce local structural rules only.
- `toBuilder()` is the supported immutable edit path.
- Collection-valued fields are copied on input/output.
- Persistence-specific behavior does not belong in these classes.

## 3. Current Dpp4Fun datamodel summary

Current furniture-specific model package: `dppsdk.dpp4fun.model`

### Aggregate root

- `Dpp4Fun extends Dpp`
  - fields:
    - `DppCore coreDpp`
    - `ProductClassification classification`
    - `Characteristics characteristics`
    - `BillOfMaterials billOfMaterials` optional
  - convenience getters delegate into the nested submodels rather than duplicating state

### Furniture-specific submodels

- `ProductClassification`
  - `sector`
  - `group`
  - `category`
  - `subCategory`
  - `tags`
- `Characteristics`
  - `productName`
  - `description`
  - `brand`
  - `productType`
  - `Dimensions dimensions`
  - `Double weight`
  - `String color`
  - `List<String> features`
- `Dimensions`
  - `width`
  - `height`
  - `depth`
  - `unit`
- `BillOfMaterials`
  - `List<Material> materials`
  - `List<Component> components`
  - `List<Part> parts`
- `Material`
  - `name`
  - `mandatory`
  - `portion`
  - `reference`
- `Component`
  - `name`
  - `reference`
- `Part`
  - `name`
  - `mandatory`
  - `reference`

### Dpp4Fun semantics relevant to persistence

- The aggregate already carries all data needed to write a fully relational snapshot.
- JSON compatibility is handled by the existing payload/mapper/codec path, not by the domain model.
- Persistence should store and reconstruct `Dpp4Fun` without changing its semantics.

## 4. Current validation responsibility summary

### Current split

- Builder layer:
  - local structural checks only
- Validator layer:
  - semantic and cross-object rules
- JSON codec:
  - serialization/deserialization plus optional validation

### Current validator entry points

- `dppsdk.core.validation.ValidationService`
  - core-safe validation facade
- `dppsdk.dpp4fun.validation.Dpp4FunValidationService`
  - registers `Dpp4Fun` validators on top of the core service

### Rules that must stay in validators, not in PostgreSQL persistence

- future `passportUpdateDates` rejection
- semantic blank-string rejection for optional text fields
- `classification.category` vs `characteristics.productType` consistency
- `externalDocumentationLink` without `Documentation` object
- duplicate normalized tag/feature/BOM entry detection
- email `@` sanity check
- `mandatory == true` material requiring `portion > 0`
- contact-channel presence rules
- manufacturer/supplier role-slot rules

### Database constraints that are appropriate

- `NOT NULL` for required stored columns
- foreign keys between passport/version/core/detail tables
- unique DPP identifier
- unique active product identifier
- `version_no > 0`
- status check constraint
- one current version row per passport
- append-only event rows

### Persistence assumption

- The PostgreSQL repository assumes the incoming `Dpp4Fun` has already been validated by existing validators.
- PostgreSQL storage should enforce storage integrity, not re-implement semantic validation.

## 5. Current mock repository operation map

Current module: `dpp-sdk-demo/mock-dpp-repo`

| Endpoint | Controller method | Service method | Current store behavior | Current tests | Proposed PostgreSQL repository method | Repository or outside repository |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /dpps` | `DppRepoController.create` | `DppRepoService.create` | `store.create(dppId, productId, canonicalJson, now)` writes active record and appends first version; `store.appendEvent(..., "DPP_CREATED", {"productId": ...})` | `DppRepoControllerTest.createDppCreatesAndCanBeReadByIdAndProductId`, `StandardsRepoClientFlowEndToEndTest.clientCanExerciseStandardsRepoFlowWithRegistryStub` | `create(validatedDpp, context)` | Repository |
| `GET /dpps/{dppId}` | `DppRepoController.readById` | `DppRepoService.readById` | `findActiveByDppId`; returns current stored JSON; deleted records hidden | `DppRepoControllerTest.createDppCreatesAndCanBeReadByIdAndProductId`, `readByIdAndProductIdHandleMissingAndDeletedRecords`, end-to-end test | `findCurrentByDppId(dppId)` | Repository |
| `HEAD /dpps/{dppId}` | `DppRepoController.verifyById` | `DppRepoService.hasActiveDpp` | `hasActiveDpp` delegates to active record lookup only | `DppRepoControllerTest.headByDppIdVerifiesActiveDppExistence`, `MockRepoSeedDataTest` | `existsActiveByDppId(dppId)` | Repository |
| `GET /dppsByProductId/{productId}` | `DppRepoController.readByProductId` | `DppRepoService.readByProductId` | `findActiveByProductId` through active product->dpp lookup | `DppRepoControllerTest.createDppCreatesAndCanBeReadByIdAndProductId`, `readByIdAndProductIdHandleMissingAndDeletedRecords`, `MockRepoSeedDataTest`, end-to-end test | `findCurrentByProductId(productId)` | Repository |
| `GET /dppsByProductIdAndDate/{productId}?date=...` | `DppRepoController.readByProductIdAndDate` | `DppRepoService.readVersionByProductIdAndDate` | `findVersionByProductIdAndDate(productId, at)` finds latest version whose `validFrom <= at` and rejects if product was deleted between version start and requested time | `DppRepoControllerTest.readVersionByProductIdAndDateReturnsHistoricalSnapshots`, `versionLookupUsesDeterministicProductDeletionHistory`, end-to-end test | `findByProductIdAt(productId, timestamp)` | Repository |
| `POST /dppsByProductIds` | `DppRepoController.readIdsByProductIds` | `DppRepoService.readIdsByProductIds` | validates request, parses string cursor as integer offset, returns active DPP IDs only, computes next cursor | `DppRepoControllerTest.readDppIdsByProductIdsSupportsLimitCursorAndRejectsBadInput`, end-to-end test | `findActiveDppIdsByProductIds(productIds, pageRequest)` | Repository |
| `PATCH /dpps/{dppId}` | `DppRepoController.update` | `DppRepoService.updateById` | merges JSON via `DppMergePatchService`, validates full merged `Dpp4Fun`, enforces immutable DPP/product IDs, `store.update(...)`, appends new version, records `DPP_UPDATED` | `DppRepoControllerTest.updateByIdUsesMergePatchAndRemainsAtomicOnValidationFailure`, concurrency test, end-to-end test | `appendVersion(validatedDpp, expectedVersion, context)` | Patch application and validation stay outside; version append is repository |
| `DELETE /dpps/{dppId}` | `DppRepoController.delete` | `DppRepoService.deleteById` | `store.softDelete(...)` removes active lookup, keeps record and historical versions, records `DPP_DELETED` | `DppRepoControllerTest.deleteByIdSoftDeletesAndRecordsLifecycleEvent`, `readByIdAndProductIdHandleMissingAndDeletedRecords`, end-to-end test | `softDelete(dppId, expectedVersion, context)` | Repository |
| `GET /dpps/{dppId}/elements/{elementPath}` | `DppRepoController.readDataElement` | `DppRepoService.readDataElement` | loads current full JSON, resolves curated element path via `DppElementPathService`; no dedicated store method | `DppRepoControllerTest.readAndUpdateDataElementWorkAndRemainAtomic`, `MockRepoSeedDataTest`, `DppElementPathServiceTest`, end-to-end test | `findCurrentByDppId(dppId)` then service-level element read | Outside repository |
| `PATCH /dpps/{dppId}/elements/{elementPath}` | `DppRepoController.updateDataElement` | `DppRepoService.updateDataElement` | loads current JSON, applies curated element update via `DppElementPathService`, validates full updated `Dpp4Fun`, enforces immutable IDs, `store.update(...)`, appends new version, records `DATA_ELEMENT_UPDATED` with `{"elementPath": ...}` | `DppRepoControllerTest.readAndUpdateDataElementWorkAndRemainAtomic`, `DppElementPathServiceTest`, end-to-end test | `appendVersion(validatedDpp, expectedVersion, context)` plus event metadata in `context` | Element-path update and validation stay outside; version append is repository |
| `GET /dpps/{dppId}/events` | `DppRepoController.readEvents` | `DppRepoService.readEvents` | checks DPP exists via `findAnyByDppId`, then returns append-only event list by `dppId`; events remain readable after delete | `DppRepoControllerTest.readEventsReturnsLifecycleEventObjectsAndMissingDppFails`, delete test, end-to-end test | `findEventsByDppId(dppId)` | Repository |

## 6. Current lifecycle event behavior

### Current event shape

Current event record in the mock repo:

- `eventId`
- `dppId`
- `eventType`
- `occurredAt`
- `data` as JSON tree

Current event data payloads:

- `DPP_CREATED` -> `{"productId": "<gtin/productId>"}`
- `DPP_UPDATED` -> `{"productId": "<gtin/productId>"}`
- `DATA_ELEMENT_UPDATED` -> `{"elementPath": "<path>"}`
- `DPP_DELETED` -> `{"productId": "<gtin/productId>"}`

### Current behavior

- append-only
- keyed by `dppId`
- readable after soft delete
- not used as source-of-truth DPP storage
- no field diffs
- no full DPP snapshots
- not full Track & Trace

### PostgreSQL requirement implied by current behavior

- Preserve the same event model.
- Store events in a relational event table with a small payload column.
- `data` may be stored as PostgreSQL `jsonb` because it is small audit metadata, not the authoritative DPP document source.

## 7. PostgreSQL target architecture

Target direction for this plan:

- PostgreSQL-first, not multi-database
- root-level standalone parent module
- reusable core PostgreSQL support in one module
- Dpp4Fun-specific relational persistence in one module
- no new persistence behavior inside `dpp-core` or `dpp4fun`
- no JSONB full-document source of truth
- no service layer in the PostgreSQL modules
- one SDK-facing repository for `Dpp4Fun`

### Architectural shape

- `dpp-postgres-core`
  - reusable PostgreSQL infrastructure for DPP identity, versions, DppCore mapping, lifecycle events, paging primitives, operation context, and shared row/type mapping
- `dpp4fun-postgres`
  - Dpp4Fun-specific schema mapping, query support, and public repository

### Storage shape

- Relational snapshot storage by version
- Current reads resolve the current version row for a passport
- Historical reads resolve the latest version row whose validity started before the requested timestamp and whose passport was not deleted before that timestamp
- Lifecycle events stored separately from versions

## 8. Proposed module structure

```text
dpp-postgres/
|-- pom.xml
|-- dpp-postgres-core/
|   `-- pom.xml
`-- dpp4fun-postgres/
    `-- pom.xml
```

### Root `dpp-postgres/pom.xml`

Responsibilities:

- independent parent aggregator for PostgreSQL modules
- Java 17 build config
- dependency management for:
  - PostgreSQL JDBC driver
  - JUnit 5
  - Testcontainers PostgreSQL
- optional root-level entry point for `-f dpp-postgres/pom.xml clean test`

### Reactor decision

Phase recommendation:

- Create `dpp-postgres` as a standalone top-level project first.
- Do not add it to the existing root `pom.xml` in the first persistence phase.

Reason:

- The current root reactor excludes `dpp-persistence` already.
- A standalone aggregator matches the user's requested direction and minimizes disruption to current builds.
- Root reactor integration can be a later explicit decision once the PostgreSQL modules are stable.

## 9. Proposed reusable core PostgreSQL components

Proposed package area:

- `dpp-postgres-core/src/main/java/dppsdk/postgres/core/...`

### Public reusable components

- `PostgresOperationContext`
  - `Instant occurredAt`
  - `String eventType`
  - `JsonNode eventData`
  - `Integer expectedVersionNo` or equivalent optimistic-concurrency value
- `CursorPageRequest`
  - `String cursor`
  - `int limit`
- `CursorPage<T>`
  - `List<T> items`
  - `String nextCursor`
- `LifecycleEventRow` or domain-facing `StoredLifecycleEvent`
  - mirrors current mock event behavior
- `PostgresTypeMapper<T>`
  - reusable conversion interface for PostgreSQL row/parameter handling where grouped mappers need shared conversions
- `PostgresRepositoryException`
  - base unchecked persistence exception
- `DuplicateDppIdException`
- `DuplicateActiveProductIdException`
- `OptimisticConcurrencyException`
- `StoredDppNotFoundException`

### Internal reusable mappers

- `PostgresDppCoreMapper`
  - grouped mapper for:
    - `DppCore`
    - `PassportMetadata`
    - `Nameplate`
    - `Documentation`
    - `Organization`
    - `Contact`
    - `Address`
    - `Email`
    - `Telephone`
  - writes relational version-scoped rows
  - reconstructs a `DppCore` from version-scoped rows

### Internal reusable repositories/helpers

- `PostgresPassportRepositorySupport`
  - current-version resolution
  - version-number allocation
  - active-product uniqueness checks
  - historical lookup base query
- `PostgresLifecycleEventStore`
  - append event row
  - find events by `dppId`
- `SchemaSql`
  - SQL resource loader or constant holder for schema/bootstrap scripts

### Explicit non-goals for core PostgreSQL module

- no `DppStore` abstraction for this phase
- no generic cross-database SPI
- no `Dpp4Fun` dependency
- no service layer
- no fine-granular patch logic
- no semantic validation logic

## 10. Proposed Dpp4Fun-specific PostgreSQL components

Proposed package area:

- `dpp4fun-postgres/src/main/java/dppsdk/postgres/dpp4fun/...`

### Public SDK-facing repository

- `Dpp4FunPostgresRepository`

Responsibilities:

- create validated `Dpp4Fun`
- read current by DPP ID
- read current by product ID
- existence check by DPP ID
- read historical by product ID and timestamp
- append validated new version
- soft delete
- batch lookup of active DPP IDs by product IDs with cursor paging
- read lifecycle events by DPP ID
- search projection queries

### Internal grouped mapper

- `Dpp4FunPostgresMapper`
  - grouped relational mapper for:
    - `ProductClassification`
    - `Characteristics`
    - `Dimensions`
    - `BillOfMaterials`
    - `Material`
    - `Component`
    - `Part`

### Query side

- `Dpp4FunQueryRepository`
  - query-oriented SQL for:
    - batch product ID lookup
    - historical lookup
    - search projections
- `Dpp4FunSearchCriteria`
  - optional filters such as:
    - category
    - brand
    - material
    - component
    - part
- `Dpp4FunSearchResult`
  - lightweight projection for search/read-list use cases

### Support classes

- `Dpp4FunRowAssembler`
  - gathers joined result sets into the domain aggregate
- `Dpp4FunVersionWriteSet`
  - grouped write model for one relational version insert batch

## 11. Proposed schema tables

The schema should be version-centric and relational.

### Core tables

- `dpp_passport`
  - surrogate key
  - `dpp_id` unique
  - `product_id`
  - `status`
  - `created_at`
  - `updated_at`
  - `deleted_at`
  - partial unique index: one active row per `product_id`
- `dpp_passport_version`
  - surrogate key
  - `passport_fk`
  - `version_no`
  - `is_current`
  - `valid_from`
  - `created_at`
  - unique `(passport_fk, version_no)`
  - partial unique index on `(passport_fk)` where `is_current = true`
- `dpp_passport_update_date`
  - `version_fk`
  - sequence/order column
  - update date value
- `dpp_core_version`
  - `version_fk`
  - QR/digital tag
  - external documentation link
  - GTIN and other nameplate columns
  - manufacturer and supplier organization references
- `dpp_documentation_version`
  - `version_fk`
  - digital instructions link
  - safety instructions link
  - downloadable
  - available-for-years
  - paper-copy flag
- `dpp_organization_version`
  - organization row per version usage
  - role slot marker (`MANUFACTURER` or `SUPPLIER`)
  - name/gln/product descriptor fields/uri
  - optional contact reference
- `dpp_contact_version`
  - `organization_fk`
  - organization/contact label
  - optional address/email/telephone references
- `dpp_address_version`
  - contact-scoped address values
- `dpp_email_version`
  - contact-scoped email values
- `dpp_telephone_version`
  - contact-scoped telephone values
- `dpp_lifecycle_event`
  - surrogate key
  - `event_id` unique
  - `dpp_id`
  - `event_type`
  - `occurred_at`
  - `event_data_jsonb`
  - append-only

### Dpp4Fun tables

- `dpp4fun_classification_version`
  - `version_fk`
  - `sector`
  - `group_name`
  - `category`
  - `sub_category`
- `dpp4fun_classification_tag`
  - `version_fk`
  - sequence/order column
  - tag
- `dpp4fun_characteristics_version`
  - `version_fk`
  - `product_name`
  - `description`
  - `brand`
  - `product_type`
  - `width`
  - `height`
  - `depth`
  - `dimension_unit`
  - `weight`
  - `color`
- `dpp4fun_feature`
  - `version_fk`
  - sequence/order column
  - feature
- `dpp4fun_bom_material`
  - `version_fk`
  - sequence/order column
  - `name`
  - `mandatory`
  - `portion`
  - `reference`
- `dpp4fun_bom_component`
  - `version_fk`
  - sequence/order column
  - `name`
  - `reference`
- `dpp4fun_bom_part`
  - `version_fk`
  - sequence/order column
  - `name`
  - `mandatory`
  - `reference`

### Why version-scoped rows

- Current mock behavior keeps append-only version history.
- Historical reads are snapshot-style reads.
- Current validation and JSON handling remain outside persistence.
- Version-scoped relational rows allow full aggregate reconstruction without using full-document JSON storage as source of truth.

## 12. Proposed repository API

Suggested public API for `Dpp4FunPostgresRepository`:

```java
public interface Dpp4FunPostgresRepository {
    String create(Dpp4Fun validatedDpp, PostgresOperationContext context);
    Optional<Dpp4Fun> findCurrentByDppId(String dppId);
    boolean existsActiveByDppId(String dppId);
    Optional<Dpp4Fun> findCurrentByProductId(String productId);
    Optional<Dpp4Fun> findByProductIdAt(String productId, Instant at);
    CursorPage<String> findActiveDppIdsByProductIds(List<String> productIds, CursorPageRequest pageRequest);
    int appendVersion(Dpp4Fun validatedDpp, PostgresOperationContext context);
    void softDelete(String dppId, PostgresOperationContext context);
    List<StoredLifecycleEvent> findEventsByDppId(String dppId);
    CursorPage<Dpp4FunSearchResult> search(Dpp4FunSearchCriteria criteria, CursorPageRequest pageRequest);
}
```

### API notes

- `create`
  - returns the created DPP ID
- `appendVersion`
  - returns the new version number
  - uses optimistic concurrency through `context.expectedVersionNo`
- `softDelete`
  - uses optimistic concurrency through `context.expectedVersionNo`
- `findByProductIdAt`
  - matches current mock repository historical semantics
- `findEventsByDppId`
  - returns audit events even after soft delete
- `search`
  - query/projection API for category/brand/material/component/part use cases

### What stays outside the repository

- merge patch application
- curated element-path resolution
- semantic validation
- transport JSON flattening/normalization
- HTTP request/response handling

## 13. Explicit exclusions

This plan explicitly rejects:

- one repository per model class
- one public mapper per tiny datamodel class
- a generic `DppStore` abstraction for this phase
- a PostgreSQL service layer that only wraps repository methods
- hashes
- blockchain anchoring
- registry integration
- dataspace integration
- full Track & Trace lifecycle
- event sourcing
- lifecycle events as DPP snapshots
- lifecycle event field diffs
- JSONB full-document storage as the primary source of truth
- generic arbitrary query framework
- duplicating semantic validators in SQL
- persistence methods inside `Dpp`, `DppCore`, or `Dpp4Fun`
- changes to current datamodel semantics

## 14. Implementation phases

### Phase 1: PostgreSQL module scaffold

- Create top-level `dpp-postgres/pom.xml`
- Create child modules:
  - `dpp-postgres-core`
  - `dpp4fun-postgres`
- Add Java 17, JUnit 5, PostgreSQL driver, Testcontainers PostgreSQL
- Keep the parent standalone from the root reactor initially

### Phase 2: Core PostgreSQL foundation

- Add paging primitives and operation context
- Add persistence exceptions
- Add schema DDL for:
  - passport identity
  - versions
  - lifecycle events
  - DppCore relational tables
- Implement `PostgresDppCoreMapper`

### Phase 3: Dpp4Fun relational mapping

- Add Dpp4Fun schema DDL
- Implement `Dpp4FunPostgresMapper`
- Implement full aggregate write/read reconstruction for current versions

### Phase 4: Historical/version semantics

- Implement create
- Implement append version with optimistic concurrency
- Implement current read by DPP ID
- Implement current read by product ID
- Implement historical read by product ID + timestamp
- Implement soft delete preserving history
- Implement event append/read

### Phase 5: Query support

- Implement batch product ID lookup with string cursor paging compatible with the mock API
- Implement `Dpp4FunQueryRepository`
- Implement search by:
  - category
  - brand
  - material
  - component
  - part

### Phase 6: Mock integration

- Keep `DppRepoService` as the current place for:
  - codec usage
  - full-DPP validation
  - merge patch logic
  - curated element-path logic
- Add a PostgreSQL-backed adapter path in `mock-dpp-repo` that delegates storage calls to `Dpp4FunPostgresRepository`
- Preserve current endpoint contracts and event semantics

### Phase 7: Docs and build integration

- Add PostgreSQL module README(s)
- Add usage/build docs
- Decide whether to add `dpp-postgres` to the root reactor after tests stabilize

## 15. Test plan

Use Testcontainers PostgreSQL in the new PostgreSQL modules.

Required tests:

- create and read back full `Dpp4Fun`
- read by `dppId`
- exists active by `dppId`
- read by `productId`
- historical read by `productId` and timestamp
- append version
- stale expected version rejection
- soft delete
- events created and preserved after delete
- batch product ID lookup with cursor paging
- search projection by category/brand/material/component/part
- optional `Documentation` roundtrip
- optional `BillOfMaterials` roundtrip
- organization/contact/address/email/telephone roundtrip

Recommended test layout:

- `dpp-postgres-core`
  - schema bootstrap test
  - lifecycle-event persistence test
  - DppCore roundtrip tests
  - concurrency/version-number tests
- `dpp4fun-postgres`
  - full aggregate repository integration tests
  - search query tests
  - historical lookup tests

Do not modify old datamodel or mock-repo tests in the planning phase.

## 16. Mock integration plan

### Current mock responsibilities to preserve

- `DppRepoService` currently owns:
  - JSON parse/serialize via `Dpp4FunJsonCodec`
  - semantic validation via `Dpp4FunValidationService`
  - full-aggregate patch behavior via `DppMergePatchService`
  - fine-granular element behavior via `DppElementPathService`
  - mock-compatible lifecycle event meaning

### Planned integration shape

- Keep the current HTTP controller API unchanged.
- Keep patch and element-path logic in `mock-dpp-repo`.
- Replace direct `InMemoryDppStore` coupling with a storage adapter path that delegates to `Dpp4FunPostgresRepository`.
- Preserve current operation semantics:
  - create -> repository `create`
  - full update -> service validates updated `Dpp4Fun`, repository `appendVersion`
  - fine-granular update -> service validates updated `Dpp4Fun`, repository `appendVersion`
  - delete -> repository `softDelete`
  - events -> repository `findEventsByDppId`

### Why this split matches current code

- The repository should own storage and history.
- The mock service should continue owning API behavior, patch behavior, and validation orchestration.
- This mirrors the current boundary already present between `DppRepoService` and `InMemoryDppStore`.

## 17. Risks and open questions

- Root reactor integration is currently unresolved because the root `pom.xml` excludes `dpp-persistence` already. Decide explicitly whether `dpp-postgres` stays standalone long-term or later joins the root reactor.
- No repo-local CI files are visible in this checkout, so CI integration steps may depend on an external pipeline definition not present here.
- The current checkout does not expose tracked `dpp-persistence` source files. Do not assume compatibility with unseen branch-only persistence abstractions.
- Historical lookup after delete/recreate for the same product must preserve the current mock behavior of returning no snapshot during the deleted gap.
- Search result shape is not defined today. Confirm whether `Dpp4FunSearchResult` should be:
  - lightweight projection only, or
  - projection plus selected version metadata.
- Decide whether lifecycle event payload should be stored as:
  - `jsonb`, or
  - serialized text.
  The plan assumes `jsonb` is acceptable only for event metadata, not for the full DPP source of truth.
- Decide whether PostgreSQL schema bootstrap should use:
  - plain SQL scripts executed in tests and application setup, or
  - a migration tool later.
  For the first phase, plain SQL is the lower-complexity choice.
