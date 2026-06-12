# dpp-registry-client

`dpp-registry-client` contains low-level HTTP behavior for the draft-prEN-18222-aligned DPP registry API.

Public surface:

- `dpp.registry.client.DppRegistryClient`
- `dpp.registry.client.HttpDppRegistryClient`
- registry client exception types under `dpp.registry.client.exception`

This module depends on `dpp-registry-payloads` and exposes the current registration operation only:

- `postNewDppToRegistry` -> `POST /registerDPP`

This module must not contain:

- SDK or Cir4Fun domain models
- repository client orchestration
- persistence or mock registry service behavior
- production EC registry implementation concerns

Dependency snippet:

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-registry-client</artifactId>
    <version>0.3.0</version>
</dependency>
```

Minimal usage:

```java
import dpp.registry.client.DppRegistryClient;
import dpp.registry.client.HttpDppRegistryClient;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.registry.payloads.RegisterDppResponse;

DppRegistryClient registryClient = new HttpDppRegistryClient(
    "http://localhost:8081"
);

RegisterDppResponse registered = registryClient.postNewDppToRegistry(
    new RegisterDppRequest(productIdentifier, dppIdentifier, operatorIdentifier, repoUrl)
);
```

The low-level client is intentionally separate from repo-backed registration orchestration. If the overall system verifies repository presence before accepting registry registration, that behavior belongs in the demo/backend repository rather than in this low-level client module.

The current low-level client intentionally follows the active four-field `RegisterDppRequest` contract. Backup-related draft concepts are not implemented here, and this module must not add unused placeholder fields or future-facing registry methods until there is a concrete system/API requirement.
