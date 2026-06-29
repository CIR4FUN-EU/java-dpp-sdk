# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Root-level `dpp-postgres` module for optional PostgreSQL persistence.
- `dpp-postgres-core` reusable PostgreSQL support for DPP identity, versioning, `DppCore`, paging, and lifecycle events.
- `dpp4fun-postgres` relational `Dpp4Fun` persistence with version/history, soft delete, lifecycle events, batch lookup, and search support.
- Optional PostgreSQL backend wiring for `dpp-sdk-demo/mock-dpp-repo`.
- `dpp.repo.backend=memory|postgres` mock backend setting, with memory remaining the default.
- PostgreSQL-backed mock endpoint tests and PostgreSQL usage documentation.

### Changed
- Preserved mock endpoint paths and response shapes while allowing `mock-dpp-repo` to run on either in-memory or PostgreSQL storage.
- Kept validation, JSON handling, merge patching, and fine-granular element-path logic outside the PostgreSQL repository layer.
- Added lightweight comments/Javadocs to the PostgreSQL persistence and mock backend integration classes.

### Validation
- Targeted Maven validation passed for the PostgreSQL and mock integration modules.
- Docker/Testcontainers-backed PostgreSQL tests were skipped in the current environment where Docker was unavailable, so a Docker-enabled verification pass is still pending.


## [0.3.0] - 2026-06-12

### Added
- Initial public release of the DPP SDK — a Java toolkit for building and
  validating Digital Product Passports.
- `dpp-datamodel` — reusable DPP domain model and payload DTOs.
- `dpp-sdk-clients` — client library for DPP services.
- `dpp-sdk-demo` — runnable Spring Boot example showing end-to-end usage including mocked services for dpp-registry and dpp-repository conforming to the draft standardisation documents published by the JTC24 of CEN/CENELEC 

### Documentation
- Root README describing the monorepo split across the three modules.
    - `dpp-sdk-demo` — runnable Spring Boot example showing end-to-end usage.

  ### Documentation
    - Root README describing the monorepo split across the three modules.
    - Architecture diagram under `docs/`.

  [0.3.0]: https://github.com/CIR4FUN-EU/dpp-sdk/releases/tag/v0.3.0
