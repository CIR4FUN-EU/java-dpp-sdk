# Changelog

All notable changes are documented using [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Version selection follows the project's pre-1.0 [Semantic Versioning](https://semver.org/spec/v2.0.0.html) policy.

## [0.6.0]

### Breaking API and model changes

- Replaced the provisional core-aggregate arrangement (`Dpp`/`DppCore` and older mapping-centric APIs) with a single immutable, canonical `DigitalProductPassport` plus `DppHeader` and ordered EN-style `DataElement` hierarchy. Update consumers to the new construction and codec APIs.
- Rebuilt DPP4Fun as a typed *facade* over the same canonical passport; typed edits no longer maintain a separately mapped authoritative furniture graph. Migrate removed provisional classes and conveniences to named D4.1 aspect/Characteristics builders and `toBuilder()` editing.
- Changed the generic repository HTTP client to accept JSON (`JsonNode`) and return JSON payloads rather than requiring typed `DppCodec<T>` / `DppValidator<T>` adapters. Callers explicitly validate and serialize beforehand, and decode FULL JSON after reads with the SDK codec.
- Defined explicit FULL or COMPRESSED representation choices for create, reads, updates and fine-granular operations. Omitted representation uses compressed operational JSON; applications wanting the typed canonical model should read FULL.
- Reorganized PostgreSQL persistence around canonical JSONB snapshots and version history, with optional projection and lifecycle/audit-event modules. Review the new module dependencies and schema/bootstrap responsibilities before upgrading any persisted demo dataset.

### Removed public APIs

- Removed the provisional `Dpp`, `DppCore`, and `PassportMetadata` core hierarchy and its old core payload, mapper, and validator APIs. Use `DigitalProductPassport`, `DppHeader`, the EN `DataElement` types, `DppJsonCodec`, and the separate validators.
- Removed provisional Dpp4Fun types and convenience APIs including `Documentation`, `ProductClassification`, generic `Characteristics`, `Dimensions`, `Organization`, and product-name convenience accessors. Use the named D4.1 aspect and variant builders under `dpp4fun`.
- Removed generic `DppCodec<T>` / `DppValidator<T>` repository-client adapters and `DppValidationClientException`. Prepare JSON with the datamodel, call the JSON-only client, and handle transport/API failures through the current client exception types.

### Fixed

- Corrected all six Dpp4Fun `Surface` bindings and their current typed behavior to descriptive `xsd:string`; numeric input is not coerced. The contradictory D4.1 numeric-area prose remains documented.
- Required `messageType` and `text` on API message entries, rejected null message-list entries, and validated repository/registry client base URLs as absolute HTTP(S) URLs.
- Corrected demo/API examples and Postman request paths against the current JSON representation and client contracts.

### Added — EN-oriented core modeling and transport

- `DigitalProductPassport`, header granularity (`model`/`batch`/`item`), and the five concrete EN 18223 DataElement types: single-valued, collections, homogeneous multi-valued, related resources, and multilingual values.
- Structural/semantic `DppValidator`, local `DataFormatValidator`, and separately invoked `IdentifierValidator` with explicit selected identifier methods. Checks return structured rule/field diagnostics without implying global/legal conformity.
- `DppJsonCodec` expanded FULL JSON read/write, compressed operational JSON serialization, and best-effort compressed expansion; an optional source-backed resolver can recover catalog metadata but never invent unknown dictionary references.
- Model immutability and defensive collection handling, ordered/repeatable elements, JSON-native primitive semantics, strict handling of unsupported expanded JSON properties, and explicit local datatype/format validation.

### Added — furniture DPP4Fun profile

- Typed, immutable catalog-backed DPP4Fun bindings for all **19 general D4.1 aspects**, **six Characteristics variants** (Chair, Crib, Fabric, Locker, ParticleBoard and Sofa), and reusable composite families.
- Frozen source-versioned semantic profile, element-ID/path mappings, supported datatypes and unit slots, variant applicability, repeated-item policies, and source-backed metadata resolver.
- Typed DPP4Fun construction, profile validation, immutable field-level edits, unknown-core-compatible element preservation and DPP4Fun JSON convenience over the canonical core transport.
- Unit-aware dimensions and measurement slots with explicit semantic unit definitions; unsupported unit mappings and unverified dictionary IRIs are not fabricated.

### Added — PostgreSQL backend and demo coverage

- Generic `dpp-postgres-core` canonical DPP revision storage, optimistic version handling, current/history/point-in-time reads and soft deletes; complete expanded DPP documents remain stored in JSONB.
- Optional `dpp-postgres-projections` current-state query documents and explicit index generation; optional `dpp-postgres-events` lightweight audit-event recording; `dpp4fun-postgres` composes them for furniture-oriented search.
- Selectable demo repository `core` or `dpp4fun` validation profile, memory/PostgreSQL backends, optional events, and independent metadata-only mock registry storage.
- Updated demo builder scenarios and repository/registry API examples for the new canonical model, JSON client boundary, expanded/compressed representations and fine-granular element paths.

### Changed and corrected

- Changed `Characteristics.Surface` in all six DPP4Fun variants from numeric to descriptive `xsd:string`, according to pinned public Cir4Fun AAS templates and populated examples. The conflicting D4.1 numeric-area prose is recorded as an unresolved source issue.
- Kept Safety `ProfessionalUseOnly` Boolean and `Standard.ComplianceStatus` String as supported by AAS templates and examples.
- Clarified the metadata-only mock registry contract: registration does **not** check repository availability or copy full DPP data. Older `HEAD`-verification documentation is obsolete.
- Hardened repository/registry request and response DTO handling: required structured API message fields, rejection of null message-list entries, validation of absolute HTTP(S) client base URLs, optional result payloads and bodyless 204 deletion.
- Kept HTTP route scope explicit: selected EN 18222 lifecycle/registry operations and singular RFC 9535 fine-granular path subset, not all endpoints or full JSONPath.
- Reformatted HTTP demo output to show labeled, pretty-printed FULL and COMPRESSED payloads and clearly identified collection- and element-level JSONPath results.
- Renamed the demo repository module `mock-dpp-repo` to `demo-dpp-repo` (artifact, directory, `DemoRepoApplication`, Swagger title `DPP Demo Repository API`). It is a working reference implementation with SDK validation, PostgreSQL-backed revisions, history and soft delete; only the registry is a mock. The published image names `dpp-repo-api` and `dpp-registry-api` are unchanged.
- Renamed the demo Compose environment variables `MOCK_REPO_*` to `DPP_REPO_*` and `MOCK_REGISTRY_*` to `DPP_REGISTRY_*`, removing the duplicate `DPP_*` aliases that previously shadowed them.
- Split documentation responsibilities between concise root navigation, datamodel README plus detailed MODEL_GUIDE, client API contracts, PostgreSQL architecture and operational demo guides.
- Aligned the Maven reactor and demo Compose configurations on version `0.6.0`. The configured GHCR image references are `ghcr.io/cir4fun-eu/dpp-repo-api:0.6.0` and `ghcr.io/cir4fun-eu/dpp-registry-api:0.6.0`.

### Documentation

- Consolidated the root README as a short entry point, kept detailed model/validation rules in `MODEL_GUIDE.md`, client contracts in the client README, PostgreSQL behavior in its module README, and operational commands in the demo README.
- Updated the architecture diagram to show the canonical passport, typed Dpp4Fun facade, generic JSON clients, PostgreSQL modules, and independent metadata-only registry.
- Made the versioned 0.6.0 GHCR image configuration the demo operations guide's primary quick start, documented startup configuration and cleanup, and kept local builds as the developer path.
- Updated the presenter guide to focus on the demo flow and link setup and service checks to the operations guide.

### Migration guidance from 0.5.1

1. Update Maven coordinates and rebuild consumer code against the `0.6.0` public APIs; replace old `Dpp`/`DppCore`, mapper and typed-client adapter usages with canonical `DigitalProductPassport` and JSON client boundary.
2. Build and validate DPP4Fun values with the named typed aspect builders, then use `Dpp4FunJsonCodec` / `DppJsonCodec` to create FULL/COMPRESSED JSON. On FULL HTTP reads, explicitly decode with `DppJsonCodec`; clients return neutral JSON.
3. Review stored DPP representations and projection bootstrap when upgrading a live PostgreSQL dataset. A populated legacy database may require explicit migration or recreation; no Flyway/Liquibase migration framework is shipped.
4. If you previously supplied numeric `Surface` data, migrate it intentionally to an appropriate, source-justified representation. Numeric measurements are **not** silently coerced into descriptive surface-type strings.
5. Use the versioned `0.6.0` GHCR image tags in the prebuilt-image Compose workflow; do not rely on the mutable `latest` tag.
6. Rename `MOCK_REPO_*` to `DPP_REPO_*` and `MOCK_REGISTRY_*` to `DPP_REGISTRY_*` in any existing `.env`, and delete the now-duplicate `DPP_*` lines. A stale `.env` does not fail the build; Compose falls back to its defaults, so overridden ports silently stop applying. Scripts referencing `mock-dpp-repo` paths or the `mock-dpp-repo` artifact need the `demo-dpp-repo` name.

### Known standards and operational limits

- EN 18223 multi-valued cardinality prose vs Table 4 and expanded `elements` vs `value` examples are internally inconsistent; the implementation's chosen interpretations are documented in MODEL_GUIDE.
- The D4.1 numeric-area `Surface` prose conflicts with published `xs:string` surface-type AAS templates/examples; model-owner clarification is outstanding.
- The SDK locally implements selected identifier syntax/profile checks under EN 18219. It does not issue globally unique IDs, prove registered issuer ownership, or support every legal identifier method.
- Compressed JSON may omit DataElement subtype, dictionary and datatype information; generic expansion is best effort without an authoritative resolver.
- Selected EN 18222 operations are a subset; full RFC 9535 paths, access control, backup/recovery, cross-repository interoperability and legal production certification are not supplied.
- Model→batch→item inheritance and parent/child hierarchy management are intentionally deferred beyond this release; this does not alter `DppHeader.granularity` support.

## [0.5.1] - 2026-08-06

### Fixed

- Aligned the approved cross-language data contracts, including Unicode
  whitespace handling, finite numeric validation, JSON mapping boundaries, and
  defensive null-member validation evidence.
- Added direct regression evidence for null registry requests and null
  bill-of-material members.

### Changed

- Bumped the monorepo, published module artifacts, runnable demo JARs, and
  maintained demo-image references from `0.5.0` to `0.5.1`.

## [0.5.0] - 2026-07-14

### Changed
- Bumped the monorepo and all published module artifacts from `0.4.0` to `0.5.0` for the breaking API and standards-alignment changes in this release.
- Moved all demo-only repository and registry business endpoints to `/internal/...` in a hard cutover; `/v1` and conventional health and OpenAPI routes are unchanged.
- Changed the mock repository and registry root URLs to redirect directly to `/swagger-ui/index.html`.

### Documentation
- Updated root and module documentation for the Maven reactor, mock-service routes, runtime configuration, and supported implementation scope.
- Corrected standards overclaims, registry payload wording, internal Docker build commands, stale version references, and guidance for the non-buildable `dpp-persistence` path.

## [0.4.0] - 2026-07-02

### Added
- Root-level `dpp-postgres` module for optional PostgreSQL persistence.
- `dpp-postgres-core` reusable PostgreSQL support for DPP identity, versioning, `DppCore`, paging, and lifecycle events.
- `dpp4fun-postgres` relational `Dpp4Fun` persistence with version/history, soft delete, lifecycle events, batch lookup, and search support.
- Optional PostgreSQL backend wiring for `dpp-sdk-demo/mock-dpp-repo`.
- Optional PostgreSQL backend wiring for `dpp-sdk-demo/mock-eu-registry`.
- `dpp.repo.backend=memory|postgres` and `dpp.registry.backend=memory|postgres` mock backend settings, with memory remaining the default for direct local runs.
- PostgreSQL-backed mock endpoint tests and PostgreSQL usage documentation.

### Changed
- Bumped the monorepo and main module artifact version from `0.3.0` to `0.4.0`.
- Preserved mock endpoint paths and response shapes while allowing `mock-dpp-repo` to run on either in-memory or PostgreSQL storage.
- Preserved mock endpoint paths and response shapes while allowing `mock-eu-registry` to run on either in-memory or PostgreSQL storage.
- Kept validation, JSON handling, merge patching, fine-granular element-path logic, and registry metadata verification outside the PostgreSQL repository layers.
- Added lightweight comments and Javadocs to the PostgreSQL persistence and mock backend integration classes.

### Documentation
- Reworked the root README into a current standalone monorepo entry point with aligned build, demo, Docker, Swagger UI, Postman, and module-routing guidance.
- Consolidated the `dpp-datamodel` docs, removed redundant datamodel docs, and kept the model/validation guidance in the current maintained files.
- Consolidated the `dpp-sdk-clients` docs into the main module README and removed redundant per-submodule README duplication.
- Updated `dpp-sdk-demo` docs to distinguish optional `.env.example`-based overrides from built-in defaults.
- Updated the root architecture diagram to include `dpp-postgres`, optional PostgreSQL-backed mock storage paths, and the current legend/arrow behavior.

### Validation
- Root-level Maven test and package validation was rerun after the `0.4.0` version bump so the updated coordinates and jar names remain consistent with the documented commands.

## [0.3.0] - 2026-06-12

### Added
- Initial public release of the DPP SDK, a Java toolkit for building and validating Digital Product Passports.
- `dpp-datamodel`, reusable DPP domain model and payload DTOs.
- `dpp-sdk-clients`, client library for DPP services.
- `dpp-sdk-demo`, runnable Spring Boot example showing end-to-end usage including mocked services for dpp-registry and dpp-repository based on selected API shapes from the draft standardisation documents published by the JTC24 of CEN/CENELEC.

### Documentation
- Root README describing the monorepo split across the three modules.
- Architecture diagram under `docs/`.

[0.4.0]: https://github.com/CIR4FUN-EU/dpp-sdk/releases/tag/v0.4.0
[0.5.0]: https://github.com/CIR4FUN-EU/dpp-sdk/releases/tag/v0.5.0
[0.5.1]: https://github.com/CIR4FUN-EU/dpp-sdk/releases/tag/v0.5.1
[0.3.0]: https://github.com/CIR4FUN-EU/dpp-sdk/releases/tag/v0.3.0
