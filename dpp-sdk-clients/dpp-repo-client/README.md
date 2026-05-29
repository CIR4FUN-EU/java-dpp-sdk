# dpp-repo-client

`dpp-repo-client` contains low-level HTTP behavior for the draft-prEN-18222-aligned DPP repository API.

Public surface:

- `dpp.repo.client.DppRepoClient<T>`
- `dpp.repo.client.HttpDppRepoClient<T>`
- `dpp.repo.client.core.DppCodec<T>`
- `dpp.repo.client.core.DppValidator<T>`
- repo client exception types under `dpp.repo.client.exception`

This module depends on `dpp-repo-payloads` and remains model-independent. Consumers must provide their own DPP model, JSON codec, and validation logic.

Supported endpoint mapping:

- `createDpp` -> `POST /dpps`
- `readDppById` -> `GET /dpps/{dppId}`
- `readDppByProductId` -> `GET /dppsByProductId/{productId}`
- `readDppVersionByProductIdAndDate` -> `GET /dppsByProductIdAndDate/{productId}?date={timestamp}`
- `readDppIdsByProductIds` -> `POST /dppsByProductIds`
- `updateDppById` -> `PATCH /dpps/{dppId}`
- `deleteDppById` -> `DELETE /dpps/{dppId}`
- `readDataElement` -> `GET /dpps/{dppId}/elements/{elementPath}`
- `updateDataElement` -> `PATCH /dpps/{dppId}/elements/{elementPath}`

This module must not contain:

- SDK or Cir4Fun domain models
- business validation beyond `DppValidator<T>` integration
- mappers
- persistence or mock service logic
- registry client behavior

Dependency snippet:

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-repo-client</artifactId>
    <version>0.3.0</version>
</dependency>
```

Minimal usage:

```java
import dpp.repo.client.DppRepoClient;
import dpp.repo.client.HttpDppRepoClient;

DppRepoClient<MyDpp> repoClient = new HttpDppRepoClient<>(
    "http://localhost:8080",
    myCodec,
    myValidator
);
```

Error behavior is reported through repo client exception types. Full DPP payload conversion is delegated to `DppCodec<T>`. Fine-granular and partial DPP operations use `JsonNode` payloads rather than SDK-owned domain classes.
