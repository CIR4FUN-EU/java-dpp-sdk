# EN 18222:2026 API Alignment

This note reflects the current repository state against the EN 18222:2026 text provided in the local standards attachment.

It does not claim final EN compliance, certification, or legal conformity. It documents what the code currently does and where that differs from EN 18222:2026.

## Basis And Scope

- Standard basis: `EN 18222:2026`, using the local attachment provided to this repository task.
- Repo state basis: current `dpp-sdk-clients` HTTP clients and the current mock repository and mock registry controllers in `dpp-sdk-demo`.
- Validation date for this note: 2026-07-08.

## Important Standard Text Note

EN 18222:2026 is internally inconsistent on the historical-read method name:

- Clause 4.1 lists `ReadDPPVersionByProductIdAndDate`.
- Clause 4.4 and Table 16 define `ReadDPPVersionByIdAndDate` with REST path `v1/dppsByIdAndDate/{dppId}?date={timestamp}`.

This note treats Clause 4.4 and Table 16 as the concrete REST mapping to compare against, because they define both the parameter semantics and the REST path.

## Current Overall Status

The current implementation is not fully aligned with EN 18222:2026.

What is present today:

- the repository lifecycle API method set is mostly present;
- the fine-granular read/update methods are present;
- the registry registration method is present;
- path segments are percent-encoded in the low-level clients.

What is not aligned today:

- the REST paths omit the required `v1/` version prefix;
- the historical repository read method uses product ID instead of DPP ID;
- the fine-granular element path is a curated subset, not RFC 9535 JSONPath;
- the registry DTO contract differs materially from `DppRegistryEntry`;
- the registry success payload uses `registryIdentifier` instead of the standard output name `registrationId`;
- the optional `representation=compressed|full` query flag is not implemented;
- the wrapper status code enums do not include `ServerNotImplemented`.

## One-To-One Method Comparison

### Repository Lifecycle API

| EN 18222:2026 method | EN REST mapping | Current repo/client state | Status |
| --- | --- | --- | --- |
| `ReadDPPById` | `GET v1/dpps/{dppId}` | Implemented as `GET /dpps/{dppId}` in the mock repo and used by the repo client | Partial |
| `ReadDPPByProductId` | `GET v1/dppsByProductId/{productId}` | Implemented as `GET /dppsByProductId/{productId}` in the mock repo and used by the repo client | Partial |
| `ReadDPPVersionByIdAndDate` | `GET v1/dppsByIdAndDate/{dppId}?date={timestamp}` | Current code exposes `GET /dppsByProductIdAndDate/{productId}?date=...` and the repo client method is `readDppVersionByProductIdAndDate` | Not aligned |
| `ReadDPPIdsByProductIds` | `POST v1/dppsByProductIds` | Implemented as `POST /dppsByProductIds` in the mock repo and used by the repo client | Partial |
| `CreateDPP` | `POST v1/dpps` | Implemented as `POST /dpps` in the mock repo and used by the repo client | Partial |
| `UpdateDPPById` | `PATCH v1/dpps/{dppId}` | Implemented as `PATCH /dpps/{dppId}` in the mock repo and used by the repo client | Partial |
| `DeleteDPPById` | `DELETE v1/dpps/{dppId}` | Implemented as `DELETE /dpps/{dppId}` in the mock repo and used by the repo client | Partial |

### Fine-Granular API

| EN 18222:2026 method | EN REST mapping | Current repo/client state | Status |
| --- | --- | --- | --- |
| `ReadDataElement` | `GET v1/dpps/{dppId}/elements/{elementIdPath}` | Implemented as `GET /dpps/{dppId}/elements/{elementPath}` | Partial |
| `UpdateDataElement` | `PATCH v1/dpps/{dppId}/elements/{elementIdPath}` | Implemented as `PATCH /dpps/{dppId}/elements/{elementPath}` with a `{ "payload": ... }` wrapper body | Partial |

### Registry API

| EN 18222:2026 method | EN REST mapping | Current repo/client state | Status |
| --- | --- | --- | --- |
| `RegisterProductDPP` | `POST v1/registerDPP` | Implemented as `POST /registerDPP` in the mock registry and used by the registry client | Partial |

## Current Misalignments In Detail

### 1. Missing `v1/` Prefix

EN 18222:2026 Clause 8 requires versioned REST paths such as:

- `v1/dpps/{dppId}`
- `v1/registerDPP`
- `v1/dpps/{dppId}/elements/{elementIdPath}`

The current controllers and clients use unversioned paths instead:

- mock repo: `/dpps`, `/dppsByProductId/...`, `/dppsByProductIdAndDate/...`, `/dpps/{dppId}/elements/...`
- mock registry: `/registerDPP`
- clients use the same unversioned paths

### 2. Historical Read Uses The Wrong Identifier

EN 18222:2026 Clause 4.4 and Table 16 define the historical read by DPP identifier and date.

Current code instead uses product identifier and date:

- client interface: `readDppVersionByProductIdAndDate`
- HTTP client path: `/dppsByProductIdAndDate/{productId}?date=...`
- mock repo controller path: `/dppsByProductIdAndDate/{productId}`

That is a functional contract mismatch, not just a naming issue.

### 3. Fine-Granular Element Paths Are Not Full JSONPath

EN 18222:2026 Clause 8.1 requires `elementIdPath` to follow RFC 9535 JSONPath.

The current mock repo explicitly documents and implements only a curated subset of supported element paths, not arbitrary JSONPath expressions. The current low-level client simply percent-encodes the provided path and does not add JSONPath semantics on top.

### 4. Registry Request DTO Does Not Match `DppRegistryEntry`

EN 18222:2026 Table 11 defines `DppRegistryEntry` with these fields:

- `uniqueProductIdentifier`
- `digitalProductPassportId`
- `uniqueEconomicOperatorIdentifier`
- `uniqueEconomicOperatorIdentifier` of the backup operator
- `dppApiEnd point`

The current request contract instead contains:

- `productIdentifier`
- `dppIdentifier`
- `operatorIdentifier`
- `repoUrl`

Current code therefore differs in both field names and field set. In particular:

- the current contract has `repoUrl`, which is a demo/system integration field for repo-backed verification;
- the current contract does not expose the backup-operator field described in Table 11;
- the current contract does not expose `dppApiEnd point` under that standard name.

### 5. Registry Response Field Name Differs

EN 18222:2026 Table 8 defines the registration output parameter as `registrationId`.

The current registry client and mock registry use `registryIdentifier` instead.

### 6. Optional `representation` Query Support Is Missing

EN 18222:2026 Clause 8.1 defines an optional `representation` query flag for GET, POST, and PATCH methods with values `compressed` and `full`.

The current clients and mock services do not implement that query parameter.

### 7. Status Code Enum Is Incomplete

EN 18222:2026 Table 15 includes `ServerNotImplemented` mapped to HTTP `501`.

The current repository and registry `DppStatusCode` enums do not include `ServerNotImplemented`.

## Current Extra Behavior Outside EN 18222:2026

The current mock/demo system also exposes behavior that is useful for demos but is outside the EN 18222:2026 method set covered here:

- repository `HEAD /dpps/{dppId}` for active-DPP verification by the mock registry;
- repository `GET /dpps/{dppId}/events`;
- registry `GET /registry/dpps/{registryId}`;
- registry `GET /registry/dpps/by-dpp-id/{dppId}`;
- repo-backed registry verification using `repoUrl` before metadata is stored.

These endpoints and behaviors may be valid project-specific demo capabilities, but they are not part of the EN 18222:2026 API surface defined in Clauses 4 to 8.

## File Evidence

Primary current-code references:

- `dpp-sdk-clients/dpp-repo-client/src/main/java/dpp/repo/client/DppRepoClient.java`
- `dpp-sdk-clients/dpp-repo-client/src/main/java/dpp/repo/client/HttpDppRepoClient.java`
- `dpp-sdk-clients/dpp-registry-client/src/main/java/dpp/registry/client/HttpDppRegistryClient.java`
- `dpp-sdk-clients/dpp-registry-payloads/src/main/java/dpp/registry/payloads/RegisterDppRequest.java`
- `dpp-sdk-clients/dpp-registry-payloads/src/main/java/dpp/registry/payloads/RegisterDppResponse.java`
- `dpp-sdk-clients/dpp-repo-payloads/src/main/java/dpp/repo/payloads/DppStatusCode.java`
- `dpp-sdk-clients/dpp-registry-payloads/src/main/java/dpp/registry/payloads/DppStatusCode.java`
- `dpp-sdk-demo/mock-dpp-repo/src/main/java/demo/repo/DppRepoController.java`
- `dpp-sdk-demo/mock-eu-registry/src/main/java/demo/registry/DppRegistryController.java`

## Conclusion

The current implementation is best described as:

- method-family overlap with EN 18222:2026;
- partial REST-shape alignment for most endpoints;
- material contract divergence for historical reads, versioned paths, fine-granular path semantics, and registry payloads.

Any future claim of EN 18222:2026 alignment should be limited to the specific areas actually changed and revalidated after the code and tests are updated.
