# PostgreSQL And Mock Integration Review

- Date: 2026-06-29
- Reviewer scope: `dpp-postgres`, `dpp-sdk-demo/mock-dpp-repo`, related POM/docs/test wiring

## 1. Diff base used

- Primary review base: current working tree versus `HEAD`
  - reason: the PostgreSQL module and mock integration work are currently present as uncommitted/added files, so `main...HEAD` does not contain the actual implementation under review
- Cross-check only: `main...HEAD`
  - result: it showed older unrelated branch deltas and was not suitable as the authoritative review base for this task

## 2. Files reviewed

- Root/build:
  - `pom.xml`
  - `dpp-postgres/pom.xml`
- PostgreSQL core:
  - `dpp-postgres/dpp-postgres-core/pom.xml`
  - `dpp-postgres/dpp-postgres-core/src/main/java/dppsdk/postgres/core/*.java`
  - `dpp-postgres/dpp-postgres-core/src/main/resources/postgres/core-schema.sql`
  - `dpp-postgres/dpp-postgres-core/src/test/java/dppsdk/postgres/core/*.java`
- Dpp4Fun PostgreSQL:
  - `dpp-postgres/dpp4fun-postgres/pom.xml`
  - `dpp-postgres/dpp4fun-postgres/src/main/java/dppsdk/postgres/dpp4fun/*.java`
  - `dpp-postgres/dpp4fun-postgres/src/main/resources/postgres/dpp4fun-schema.sql`
  - `dpp-postgres/dpp4fun-postgres/src/test/java/dppsdk/postgres/dpp4fun/*.java`
- Mock repo integration:
  - `dpp-sdk-demo/mock-dpp-repo/pom.xml`
  - `dpp-sdk-demo/mock-dpp-repo/src/main/java/demo/repo/DppRepoService.java`
  - `dpp-sdk-demo/mock-dpp-repo/src/main/java/demo/repo/DppRepoBackend.java`
  - `dpp-sdk-demo/mock-dpp-repo/src/main/java/demo/repo/InMemoryDppRepoBackend.java`
  - `dpp-sdk-demo/mock-dpp-repo/src/main/java/demo/repo/PostgresDppRepoBackend.java`
  - `dpp-sdk-demo/mock-dpp-repo/src/main/java/demo/repo/DppIdPage.java`
  - `dpp-sdk-demo/mock-dpp-repo/src/main/java/demo/repo/RepoConfiguration.java`
  - `dpp-sdk-demo/mock-dpp-repo/src/main/resources/application.properties`
  - `dpp-sdk-demo/mock-dpp-repo/src/test/java/demo/repo/DppRepoControllerTest.java`
  - `dpp-sdk-demo/mock-dpp-repo/src/test/java/demo/repo/DppRepoControllerPostgresBackendTest.java`
  - `dpp-sdk-demo/mock-dpp-repo/src/main/java/demo/repo/InMemoryDppStore.java`
  - `dpp-sdk-demo/mock-dpp-repo/src/main/java/demo/repo/DppRepoController.java`
  - `dpp-sdk-demo/mock-dpp-repo/src/main/java/demo/repo/GlobalExceptionHandler.java`
- Datamodel / validation evidence:
  - `dpp-datamodel/MODEL_GUIDE.md`
  - `dpp-datamodel/dpp-core/src/main/java/dppsdk/core/model/DppCore.java`
  - `dpp-datamodel/dpp4fun/src/main/java/dppsdk/dpp4fun/model/Dpp4Fun.java`
  - `dpp-datamodel/dpp-core/src/main/java/dppsdk/core/validation/ValidationService.java`
  - `dpp-datamodel/dpp4fun/src/main/java/dppsdk/dpp4fun/validation/Dpp4FunValidationService.java`
  - `dpp-datamodel/dpp4fun/src/main/java/dppsdk/dpp4fun/transport/Dpp4FunJsonCodec.java`
- Logs/docs:
  - `docs/postgres/DPP4FUN_POSTGRES_IMPLEMENTATION_PLAN.md`
  - `docs/postgres/DPP4FUN_POSTGRES_IMPLEMENTATION_LOG.md`
  - `docs/postgres/MOCK_REPO_POSTGRES_INTEGRATION_LOG.md`
  - `docs/postgres/DPP4FUN_POSTGRES_USAGE.md`
  - `dpp-sdk-demo/README.md`
  - `dpp-sdk-demo/DEMO_GUIDE.md`

## 3. Architecture review

- Verdict: architecture matches the approved direction.
- `dpp-postgres` is a root-level parent with:
  - `dpp-postgres-core`
  - `dpp4fun-postgres`
- `dpp-postgres-core` contains reusable version/core/lifecycle/paging support and does not depend on `dpp4fun`.
- `dpp4fun-postgres` contains grouped Dpp4Fun persistence/query code and depends on `dpp-postgres-core`.
- `mock-dpp-repo` remains the owner of:
  - JSON codec usage
  - semantic validation
  - JSON Merge Patch
  - fine-granular element-path reads/updates
  - HTTP mapping
- PostgreSQL repository responsibility stays limited to:
  - durable storage
  - version history
  - lookup
  - soft delete
  - lifecycle event persistence
  - batch lookup and search

## 4. Dependency review

- `dpp-postgres-core`
  - depends on `dpp-core`, PostgreSQL JDBC, Jackson, JUnit/Testcontainers
  - no `dpp4fun`, demo, client, registry, dataspace, or Spring-web dependency found
- `dpp4fun-postgres`
  - depends on `dpp-core`, `dpp4fun`, `dpp-postgres-core`, PostgreSQL JDBC, Jackson, test dependencies
  - no mock/demo/registry/dataspace/blockchain dependency found
- `mock-dpp-repo`
  - depends on `dpp4fun-postgres`, which is acceptable for this integration phase
- Dependency cycle check:
  - no reverse dependency from `dpp4fun-postgres` back into `mock-dpp-repo`
  - no cycle found

## 5. Schema review

- Core schema contains the expected relational tables:
  - `dpp_passports`
  - `dpp_versions`
  - `dpp_passport_metadata`
  - `dpp_passport_update_dates`
  - `dpp_nameplates`
  - `dpp_documentation`
  - `dpp_organizations`
  - `dpp_contacts`
  - `dpp_addresses`
  - `dpp_emails`
  - `dpp_telephones`
  - `dpp_lifecycle_events`
- Dpp4Fun schema contains the expected relational tables:
  - `dpp4fun_classifications`
  - `dpp4fun_classification_tags`
  - `dpp4fun_characteristics`
  - `dpp4fun_dimensions`
  - `dpp4fun_features`
  - `dpp4fun_bill_of_materials`
  - `dpp4fun_materials`
  - `dpp4fun_components`
  - `dpp4fun_parts`
- Child rows are version-scoped through `dpp_versions.id`.
- Unique active product lookup is enforced with `uq_dpp_passports_active_product_id` on `deleted_at is null`.
- One active version per passport is enforced with `uq_dpp_versions_active_passport`.
- No hash columns found.
- Full DPP JSONB source-of-truth storage was not introduced.
- JSONB is used only for lifecycle event payload data, which matches the approved scope.

## 6. Repository API review

- `Dpp4FunPostgresRepository` supports:
  - `create`
  - `appendVersion`
  - `findCurrentByDppId`
  - `existsActiveByDppId`
  - `findCurrentByProductId`
  - `findByProductIdAt`
  - `findActiveDppIdsByProductIds`
  - `findHistoryByDppId`
  - `softDelete`
  - `findEventsByDppId`
  - `recordLifecycleEvent`
  - `search`
- API shape stays domain-oriented:
  - accepts `Dpp4Fun`
  - returns `Dpp4Fun`, summaries, projections, or lifecycle-event records
  - no HTTP payload/controller types leak into the repository
- JSON Merge Patch and element-path logic are not present in the repository.
- Repository transactions are used for create/update/delete.
- Stale expected version failures exist at repository level and are now mapped safely in the mock PostgreSQL backend.

## 7. Mock integration review

- Endpoint paths are unchanged:
  - `POST /dpps`
  - `GET /dpps/{dppId}`
  - `HEAD /dpps/{dppId}`
  - `GET /dppsByProductId/{productId}`
  - `GET /dppsByProductIdAndDate/{productId}?date=...`
  - `POST /dppsByProductIds`
  - `PATCH /dpps/{dppId}`
  - `DELETE /dpps/{dppId}`
  - `GET /dpps/{dppId}/elements/{elementPath}`
  - `PATCH /dpps/{dppId}/elements/{elementPath}`
  - `GET /dpps/{dppId}/events`
- Backend selection is opt-in:
  - default `memory`
  - optional `postgres`
- `DppRepoService` still validates whole `Dpp4Fun` objects before persistence and still applies merge patch / element-path logic itself.
- Registry HEAD verification behavior is preserved because `HEAD /dpps/{dppId}` still checks active existence only.

## 8. Validation responsibility review

- Validation still happens before persistence in the mock flow through:
  - `Dpp4FunJsonCodec`
  - `Dpp4FunValidationService`
- PostgreSQL repository code does not invoke `Dpp4FunValidationService` or reimplement semantic validation rules.
- SQL constraints remain storage-integrity oriented.
- No evidence of core-only validation replacing full `Dpp4Fun` validation in the mock flow.

## 9. Lifecycle review

- Lifecycle behavior matches current mock semantics:
  - `DPP_CREATED` on create with `productId`
  - `DPP_UPDATED` on full update with `productId`
  - `DATA_ELEMENT_UPDATED` on fine-granular update with `elementPath`
  - `DPP_DELETED` on soft delete with `productId`
- Events are append-only, queried by `dppId`, and remain readable after soft delete.
- No event-sourcing or Track & Trace expansion found.

## 10. Overengineering review

- No class-per-model repository split found.
- No public mapper explosion for tiny submodels found.
- No generic arbitrary query DSL found.
- No service layer that only wraps repository methods found.
- No multi-database abstraction added.
- No hashes, blockchain, registry integration, dataspace integration, or Track & Trace expansion found.
- Verdict: implementation is not overengineered for the approved scope.

## 11. Test integrity review

- Modified old test files:
  - none
- Added test files:
  - `dpp-postgres/dpp-postgres-core/src/test/java/dppsdk/postgres/core/PostgresCoreIntegrationTest.java`
  - `dpp-postgres/dpp-postgres-core/src/test/java/dppsdk/postgres/core/PostgresCoreSurfaceTest.java`
  - `dpp-postgres/dpp4fun-postgres/src/test/java/dppsdk/postgres/dpp4fun/Dpp4FunPostgresRepositoryIntegrationTest.java`
  - `dpp-postgres/dpp4fun-postgres/src/test/java/dppsdk/postgres/dpp4fun/Dpp4FunPostgresSurfaceTest.java`
  - `dpp-sdk-demo/mock-dpp-repo/src/test/java/demo/repo/DppRepoControllerPostgresBackendTest.java`
- No deleted tests found.
- No weakened assertions, `@Disabled`, `skipTests`, Surefire/Failsafe exclusions, or CI-avoidance build changes found.

## 12. Test coverage review

- PostgreSQL module coverage present for:
  - create/read roundtrip
  - current lookup by `dppId`
  - active existence
  - current lookup by `productId`
  - append version
  - stale version rejection
  - historical lookup by timestamp
  - soft delete
  - lifecycle events including `DATA_ELEMENT_UPDATED`
  - batch product ID lookup with paging
  - search by category/brand/productType/material/component/part
  - optional documentation roundtrip
  - optional BOM roundtrip
  - organization/contact/address/email/telephone roundtrip
- Mock PostgreSQL-mode coverage present for:
  - create/get/head/get-by-product
  - full update + history lookup
  - batch lookup paging
  - delete + events
  - fine-granular read/update
  - `DATA_ELEMENT_UPDATED`
- Remaining coverage gap:
  - Docker-gated PostgreSQL integration tests were skipped in this environment, so PostgreSQL runtime behavior still needs one Docker-enabled validation run before merge

## 13. Findings by severity

### Blocker

- None.

### Major

- None.

### Minor

- Fixed: `dpp-sdk-demo/mock-dpp-repo/src/main/java/demo/repo/PostgresDppRepoBackend.java`
  - before fix, stale optimistic-concurrency failures from `Dpp4FunPostgresRepository` would fall through as generic 500s in PostgreSQL-backed mock mode, and duplicate-product conflict detection depended on brittle exception-message parsing
  - fix applied:
    - map `"Stale expected version"` failures to `ClientResourceConflict`
    - read the PostgreSQL constraint name from `PSQLException` when available and keep the old message fallback only as backup

### Suggestion

- Run the PostgreSQL integration tests on a Docker-enabled machine before merge so the new repository and PostgreSQL-backed mock path have one real containerized verification pass, not only compile/surface coverage plus skipped Testcontainers suites.

## 14. Fixes applied

- Updated `PostgresDppRepoBackend` to:
  - translate stale version conflicts into `RepoApiException(ClientResourceConflict, "DPP_VERSION_CONFLICT", ...)`
  - use `PSQLException.getServerErrorMessage().getConstraint()` for duplicate active-product conflict mapping when available

## 15. Validation commands/results

- Inspected:
  - `git status --short`
  - `git diff --stat`
  - `git diff --name-status`
  - `git diff --name-only`
  - `git merge-base main HEAD`
  - `git diff main...HEAD --stat`
  - `git diff main...HEAD --name-status`
- Validation command attempted in sandbox:
  - `.\mvnw.cmd -pl dpp-postgres,dpp-sdk-demo -am test`
  - result: insufficient evidence because it only built parent POMs
- Validation command attempted in sandbox:
  - `.\mvnw.cmd -pl dpp-postgres/dpp4fun-postgres,dpp-sdk-demo/mock-dpp-repo -am test`
  - result: blocked by sandbox/network restriction while Maven wrapper tried to connect
- Final validation command run successfully with approval:
  - `.\mvnw.cmd -pl dpp-postgres/dpp4fun-postgres,dpp-sdk-demo/mock-dpp-repo -am test`
  - result: `BUILD SUCCESS`
  - notes:
    - existing datamodel/client/demo tests in the affected reactor path passed
    - Testcontainers integration tests were skipped where Docker was unavailable:
      - `PostgresCoreIntegrationTest` skipped
      - `Dpp4FunPostgresRepositoryIntegrationTest` skipped
      - `DppRepoControllerPostgresBackendTest` skipped

## 16. Remaining risks

- PostgreSQL behavior is validated at compile/surface level in this environment, but Docker-dependent integration tests still need one real run.
- PostgreSQL-mode mock endpoint coverage is materially useful, but still narrower than the full memory-mode suite.

## 17. Merge readiness verdict

- Ready after minor cleanup

Reason:
- no blocker or major architectural issue found
- no old-test or CI weakening found
- one minor backend error-mapping issue was fixed during review
- remaining risk is the need for a Docker-enabled integration run, not a design or implementation blocker
