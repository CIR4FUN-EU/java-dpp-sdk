# dpp-registry-payloads

`dpp-registry-payloads` contains registry API communication contracts for the draft-prEN-18222-aligned DPP registry surface.

This module contains DTOs only:

- registry wrapper DTOs such as `DppApiResponse`, `DppApiMessage`, `DppStatusCode`, and `MessageType`
- registry request/response DTOs such as `RegisterDppRequest` and `RegisterDppResponse`

This module must not contain:

- HTTP clients or transport behavior
- repo client code
- SDK or Cir4Fun domain models
- validators or mappers
- persistence or mock service logic

`RegisterDppRequest.productIdentifier` and `operatorIdentifier` are the draft-aligned registry identifiers currently exposed by this contract.

`RegisterDppRequest.dppIdentifier` and `repoUrl` are intentional project/system/demo integration fields used by the repo-backed registration flow. They are not documented as pure draft-prEN-18222 registry fields. The actual repo-backed verification behavior must be checked in the demo/backend repository.

Backup-related draft registry concepts are intentionally out of scope for the current client contract. This module does not expose `backupIdentifier`, backup-operator fields, backup-provider fields, or placeholder members for future backup support. Only currently used fields belong in this payload contract.

Dependency snippet:

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-registry-payloads</artifactId>
    <version>0.3.0</version>
</dependency>
```

Use this artifact when a backend service, contract test, or integration layer only needs the registry request/response contracts and does not need the HTTP client implementation.
