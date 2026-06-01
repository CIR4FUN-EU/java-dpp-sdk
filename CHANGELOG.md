# Changelog

All notable changes to this monorepo will be documented in this file.

## [Unreleased]

### Added

- Root `LICENSE` file using Apache-2.0 text.
- Initial monorepo changelog for the current internal snapshot.

### Documentation

- Root README documents the monorepo split between `dpp-datamodel`, `dpp-sdk-clients`, and `dpp-sdk-demo`.
- `dpp-sdk-demo/README.md` clarifies that it describes the `dpp-sdk-demo` subproject rather than a standalone demo repository.

### Snapshot Notes

- `dpp-datamodel` currently publishes `com.example.dppsdk` artifacts at version `0.3.0`.
- `dpp-sdk-clients` currently publishes `dpp.client` artifacts at version `0.3.0`.
- `dpp-sdk-demo` currently builds `demo` artifacts at version `1.0.0-SNAPSHOT`.
- The repository remains internal and pre-release.
- `mock-dpp-repo` and `mock-eu-registry` are mock/demo services, not production services.
