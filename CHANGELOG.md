# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
- `dpp-sdk-demo`, runnable Spring Boot example showing end-to-end usage including mocked services for dpp-registry and dpp-repository conforming to the draft standardisation documents published by the JTC24 of CEN/CENELEC.

### Documentation
- Root README describing the monorepo split across the three modules.
- Architecture diagram under `docs/`.

[0.4.0]: https://github.com/CIR4FUN-EU/dpp-sdk/releases/tag/v0.4.0
[0.3.0]: https://github.com/CIR4FUN-EU/dpp-sdk/releases/tag/v0.3.0
