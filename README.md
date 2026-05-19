# DPP SDK Clients

Internal pre-release Java HTTP client modules for DPP repository and registry APIs.

This project is **draft-prEN-18222-aligned**. It does not claim final EN compliance.

The project intentionally contains only API communication contracts and low-level HTTP client behavior. It does not contain DPP domain models, builders, SDK validators, SDK mappers, persistence, mock service implementations, authentication, retries, async clients, caching, endpoint templating, or pagination frameworks.

## Modules

| Module | Responsibility |
| --- | --- |
| `dpp-repo-payloads` | Repository API wrapper, message, status, request, and response DTOs. |
| `dpp-repo-client` | Repository HTTP client behavior, `DppRepoClient<T>`, `HttpDppRepoClient<T>`, `DppCodec<T>`, `DppValidator<T>`, repo exceptions, and repo HTTP support. |
| `dpp-registry-payloads` | Registry API wrapper, message, status, registration request, and registration response DTOs. |
| `dpp-registry-client` | Registry HTTP client behavior, `DppRegistryClient`, `HttpDppRegistryClient`, registry exceptions, and registry HTTP support. |

Payload modules are API contracts. Client modules are HTTP behavior.

Dependency direction:

- `dpp-repo-client` depends on `dpp-repo-payloads`.
- `dpp-registry-client` depends on `dpp-registry-payloads`.
- Repo modules do not depend on registry modules.
- Registry modules do not depend on repo modules.
- Payload modules do not depend on client modules.

## Local Maven Usage

Install all modules into the local Maven repository:

```powershell
cd dpp-client
.\mvnw.cmd clean install
```

Use only the artifacts needed by the consuming project:

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-repo-client</artifactId>
    <version>0.3.0</version>
</dependency>

<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-registry-client</artifactId>
    <version>0.3.0</version>
</dependency>
```

Payload artifacts can be used directly when a service or test only needs the API contracts:

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-repo-payloads</artifactId>
    <version>0.3.0</version>
</dependency>

<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-registry-payloads</artifactId>
    <version>0.3.0</version>
</dependency>
```

Module-specific documentation:

- [docs/prEN 18222 API alignment](docs/pren-18222-api-alignment.md)
- [dpp-repo-payloads](dpp-repo-payloads/README.md)
- [dpp-repo-client](dpp-repo-client/README.md)
- [dpp-registry-payloads](dpp-registry-payloads/README.md)
- [dpp-registry-client](dpp-registry-client/README.md)

## Repository Client

Packages:

- `dpp.repo.payloads`
- `dpp.repo.client`
- `dpp.repo.client.core`
- `dpp.repo.client.exception`

Consumers provide:

- `DppCodec<T>` for full-DPP JSON serialization/deserialization
- `DppValidator<T>` for full-DPP validation before create requests
- a concrete DPP type owned outside this project

```java
import dpp.repo.client.DppRepoClient;
import dpp.repo.client.HttpDppRepoClient;

DppRepoClient<MyDpp> repoClient = new HttpDppRepoClient<>(
    "http://localhost:8080",
    myCodec,
    myValidator
);
```

Supported repository lifecycle methods:

- `createDpp`
- `readDppById`
- `readDppByProductId`
- `readDppVersionByProductIdAndDate`
- `readDppIdsByProductIds`
- `updateDppById`
- `deleteDppById`

Supported fine-granular lifecycle methods:

- `readDataElement`
- `updateDataElement`

Supported repository paths:

- `GET /dpps/{dppId}`
- `GET /dppsByProductId/{productId}`
- `GET /dppsByProductIdAndDate/{productId}?date={timestamp}`
- `POST /dppsByProductIds`
- `POST /dpps`
- `PATCH /dpps/{dppId}`
- `DELETE /dpps/{dppId}`
- `GET /dpps/{dppId}/elements/{elementPath}`
- `PATCH /dpps/{dppId}/elements/{elementPath}`

Full DPP content stays generic at the client boundary. Reads and full updates deserialize through `DppCodec<T>`. Partial DPP and data-element operations use `JsonNode`.

## Registry Client

Packages:

- `dpp.registry.payloads`
- `dpp.registry.client`
- `dpp.registry.client.exception`

```java
import dpp.registry.client.DppRegistryClient;
import dpp.registry.client.HttpDppRegistryClient;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.registry.payloads.RegisterDppResponse;

DppRegistryClient registryClient = new HttpDppRegistryClient(
    "http://localhost:8081"
);

RegisterDppResponse registered = registryClient.postNewDppToRegistry(
    new RegisterDppRequest(
        productIdentifier,
        dppIdentifier,
        operatorIdentifier,
        repoUrl
    )
);
```

Supported registry method:

- `postNewDppToRegistry`

Supported registry path:

- `POST /registerDPP`

`RegisterDppRequest.productIdentifier` and `operatorIdentifier` are the draft-aligned registration identifiers used by the client contract.

`RegisterDppRequest.dppIdentifier` and `repoUrl` are intentional system/demo integration fields. The current demo/system flow creates and stores a DPP in the repository first, then registers that stored DPP with the registry. The registry-side system can use these fields to verify that the referenced DPP is present in the repo before accepting registration. This is not documented as a pure prEN 18222 requirement. The actual verification behavior must be checked in the demo/backend repository that owns orchestration and mock service logic.

Backup-related draft registry concepts are intentionally out of scope for the current client contract. This repository does not add `backupIdentifier` or other backup placeholders unless a concrete system/API requirement exists.

## API Wrapper

Repository and registry endpoints use wrapper DTOs in their own payload modules:

```json
{
  "statusCode": "Success",
  "payload": {},
  "messages": []
}
```

Wrapper parsing is handled by each client module. Concrete DPP payload parsing remains delegated to consumer-provided `DppCodec<T>`.



## Validation

Common local validation commands:

```powershell
cd dpp-client
.\mvnw.cmd validate
.\mvnw.cmd test
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
```

Targeted module validation examples:

```powershell
cd dpp-client
.\mvnw.cmd -pl dpp-repo-client -am test
.\mvnw.cmd -pl dpp-registry-client -am test
```
