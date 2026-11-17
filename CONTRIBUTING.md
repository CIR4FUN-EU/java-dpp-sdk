# Contributing to the Java DPP SDK

Thanks for helping improve the SDK. This guide covers development contributions to the Java repository. For SDK usage, start with the [README](README.md).

## Development setup

The project targets JDK 17 and includes the Maven Wrapper, so a separate Maven installation is
not required. From the repository root:

```bash
git clone https://github.com/CIR4FUN-EU/java-dpp-sdk.git
cd java-dpp-sdk
./mvnw verify
```

On Windows PowerShell, run `.\mvnw.cmd verify` instead. Add `clean` only when you intend to remove existing `target/` build output. The full build and test commands are documented in the [README build section](README.md#build-from-source).

The full reactor requires JDK 17. PostgreSQL integration tests use Testcontainers and require a
working Docker-compatible runtime; Compose-based demos require Docker Compose v2. If Docker is
unavailable, record affected integration checks as blocked rather than treating skipped tests as
passed.

The reactor contains four independently parented projects: `dpp-datamodel` (`dpp-core`,
`dpp4fun`), `dpp-postgres` (core, projections, events, and `dpp4fun-postgres`),
`dpp-sdk-clients` (repository and registry payloads and clients), and `dpp-sdk-demo` (mock
services and integration demo). The root POM aggregates these projects but is not their Maven
parent. See each module README for module-specific setup.

The repository currently has no Maven artifact publishing workflow or documented Maven release
procedure; `.github/workflows/publish-images.yml` publishes container images only. Do not infer a
Maven publication process from that workflow.

## Making changes

- Keep changes in the module that owns the behavior: `dpp-datamodel` for DPP models and validation, `dpp-postgres` for PostgreSQL persistence, `dpp-sdk-clients` for generic HTTP clients, and `dpp-sdk-demo` for demos and mock services.
- Add or update focused tests for changed behavior. Preserve existing tests and public API and JSON
  compatibility; for intentional breaking changes, include migration guidance and coordinate the
  release impact with maintainers.
- When changing models, mappers, codecs, or semantic profiles, update the owning model/profile
  documentation and relevant canonical JSON fixtures or tests. Do not infer new semantic rules
  from examples alone.
- Update `CHANGELOG.md` for user-visible behavior, public API, compatibility, or setup changes.
- For focused validation, use the owning module's Maven Wrapper command and include required upstream modules with `-am`. For changes contained in one module, run its focused tests with required upstream modules; use the full root `clean verify` when a change spans modules.
- Keep examples free of credentials and other secrets. Describe limitations accurately; demo services are not production deployments.

## Pull requests

Before opening a pull request:

1. Run the relevant tests and record the exact command and result.
2. Review the diff for unrelated files, generated artifacts, and secrets.
3. Explain the behavior change and any compatibility impact in the pull request description.
4. Update user-facing documentation when behavior or setup changes.

Maintainers review pull requests for scope, tests, API/JSON compatibility, documentation, and
changelog coverage. Respond to requested changes in the pull request; do not merge your own
contribution unless repository permissions and the project's review rules explicitly allow it.

## License and contributions

The repository is licensed under the [Apache License, Version 2.0](LICENSE). Contributions
intentionally submitted for inclusion are governed by Section 5 of that license unless a separate
written agreement applies. Submit only work that you are authorized to contribute, and preserve
required third-party license and attribution notices. For security concerns, follow
[SECURITY.md](SECURITY.md); do not report vulnerabilities in a public issue.
