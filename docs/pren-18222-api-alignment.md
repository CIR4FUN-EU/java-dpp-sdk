# draft-prEN-18222 API Alignment

This repository is **draft-prEN-18222-aligned**. It does not claim final EN compliance, certification, or legal conformity.

The current implementation follows the draft-aligned API boundaries that were used for the four-module refactor:

- payload modules contain reusable API communication contracts only
- client modules contain low-level HTTP behavior only
- no SDK or Cir4Fun domain model belongs in this repository
- no business validation, mappers, persistence, mock services, or orchestration belong in this repository

## Repository API Alignment

Implemented repository client methods:

- `createDpp`
- `readDppById`
- `readDppByProductId`
- `readDppVersionByProductIdAndDate`
- `readDppIdsByProductIds`
- `updateDppById`
- `deleteDppById`
- `readDataElement`
- `updateDataElement`

Implemented repository paths:

- `GET /dpps/{dppId}`
- `GET /dppsByProductId/{productId}`
- `GET /dppsByProductIdAndDate/{productId}?date={timestamp}`
- `POST /dppsByProductIds`
- `POST /dpps`
- `PATCH /dpps/{dppId}`
- `DELETE /dpps/{dppId}`
- `GET /dpps/{dppId}/elements/{elementPath}`
- `PATCH /dpps/{dppId}/elements/{elementPath}`

The repository client stays model-independent. Full DPP payload conversion remains delegated to consumer-provided `DppCodec<T>` and `DppValidator<T>`.

## Registry API Alignment

Implemented registry client method:

- `postNewDppToRegistry`

Implemented registry path:

- `POST /registerDPP`

`RegisterDppRequest.productIdentifier` and `operatorIdentifier` are the draft-aligned registry identifiers currently exposed by this client contract.

`RegisterDppRequest.dppIdentifier` and `repoUrl` are **project/system/demo integration fields**, not pure draft-prEN-18222 registry fields. They exist because the current system flow can create and store a DPP in the repository first and then register that stored DPP with the registry.

The draft may describe backup-related registration information, but this client repository does **not** currently implement `backupIdentifier`, backup-operator fields, backup-provider fields, backup endpoints, or placeholder DTO members for future backup support.

Backup-related support is intentionally out of scope at this stage because there is no current client/system contract in this repository that uses it. Do not add empty placeholders or future-facing backup fields just to mirror draft wording. The active request contract remains limited to the fields that are implemented and covered by tests today:

- `productIdentifier`
- `dppIdentifier`
- `operatorIdentifier`
- `repoUrl`

## Removed Compatibility Surface

This refactor is intentionally not backward-compatible.

Removed from the low-level client contract:

- the old single `dpp.client:dpp-client` artifact
- the old `dpp.client.*` package surface
- registry lookup APIs
- `RegistryRecordResponse`
- legacy repo-prefixed path references
- legacy registry registration path references
- old presence-check endpoint usage

## Demo/System Verification Note

The low-level registry client in this repository does not perform repo-backed orchestration or repository verification. That behavior belongs in the demo/backend repository that owns mock services, storage, and end-to-end registration verification.

When checking whether repo-backed registry verification is preserved, inspect the demo/backend repository rather than this low-level client/payload repository.
