# DPP Client

Internal pre-release Java HTTP clients for standardized Digital Product Passport (DPP) standard-style mock repository and registry APIs.

This is not a stable public API. The library is currently aligned to the standard-style mock endpoints and may change between internal releases without compatibility shims.

This library stays intentionally small. It provides:

- generic client interfaces
- Java `HttpClient` implementations
- API wrapper DTOs and simple response DTOs
- separated client exceptions
- injected `DppCodec<T>` for full DPP JSON mapping
- injected `DppValidator<T>` for client-side validation before create operations

It does not provide DPP domain models, builders, payload mappers, databases, authentication, backend wiring, retries, async clients, or endpoint customization.

## Local Dependency

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-client</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

Consumers provide:

- a `DppCodec<T>` to convert a full DPP object to and from JSON
- a `DppValidator<T>` to validate a full DPP before create operations
- a base URL for the target mock service

```java
DppRepoClient<MyDpp> repoClient = new HttpDppRepoClient<>(
    "http://localhost:8080",
    myCodec,
    myValidator
);

DppRegistryClient registryClient = new HttpDppRegistryClient(
    "http://localhost:8081"
);
```

The client remains generic. SDK usage stays behind `DppCodec<T>` and `DppValidator<T>`. The SDK should not parse HTTP wrappers.

## Standard-Style Endpoints

The client now targets only the standard-style mock API paths.
Legacy CRUD/list/exists compatibility methods and legacy response DTOs have been removed.

Repository / Life Cycle API:

- `POST /dpps`
- `GET /dpps/{dppId}`
- `GET /dppsByProductId/{productId}`
- `GET /dppsByProductIdAndDate/{productId}?date={timestamp}`
- `POST /dppsByProductIds`
- `PATCH /dpps/{dppId}`
- `DELETE /dpps/{dppId}`

Repository / Fine Granular API:

- `GET /dpps/{dppId}/elements/{elementPath}`
- `PATCH /dpps/{dppId}/elements/{elementPath}`

Registry:

- `POST /registerDPP`

Mock/internal convenience lookups:

- `GET /registry/dpps/{registryId}`
- `GET /registry/dpps/by-dpp-id/{dppId}`

Old production endpoint paths are no longer used:

- `/repo/dpps...`
- `/registry/dpps/register`

All path parameters are URL-encoded. `elementPath` is encoded as a single path segment so dotted and bracketed paths are transmitted correctly.

## Public Interfaces

Repository client:

```java
public interface DppRepoClient<T> {
    CreateDppResponse createDpp(T dpp);
    T readDppById(String dppId);
    T readDppByProductId(String productId);
    T readDppVersionByProductIdAndDate(String productId, Instant date);
    ReadDppIdsResponse readDppIdsByProductIds(List<String> productIds, Integer limit, String cursor);
    T updateDppById(String dppId, JsonNode partialDpp);
    DeleteDppResponse deleteDppById(String dppId);
    JsonNode readDataElement(String dppId, String elementPath);
    JsonNode updateDataElement(String dppId, String elementPath, JsonNode payload);
}
```

Registry client:

```java
public interface DppRegistryClient {
    RegisterDppResponse postNewDppToRegistry(RegisterDppRequest request);
    Optional<RegistryRecordResponse> readRegistryRecordByRegistryId(String registryId);
    Optional<RegistryRecordResponse> readRegistryRecordByDppId(String dppId);
}
```

Codec and validator:

```java
public interface DppCodec<T> {
    String toJson(T dpp);
    T fromJson(String json);
}

public interface DppValidator<T> {
    void validate(T dpp);
}
```

## API Wrapper

The mock APIs return a standard wrapper:

```json
{
  "statusCode": "Success",
  "payload": {},
  "messages": []
}
```

`HttpSupport` parses this wrapper with Jackson inside the client library. Full DPP payload objects are still handed off to `DppCodec<T>`.

## Internal HTTP Behavior

- The default HTTP clients are intended for internal mock-service integration.
- Connect timeout: `5 seconds`
- Request timeout: `15 seconds`
- Automatic retries are not currently performed.
- Authentication, authorization, and OAuth/token handling are not currently implemented.
- Timeout, connection, and network failures are surfaced as `DppNetworkClientException`.

## Behavior

- `createDpp` validates with `DppValidator<T>` before sending, then serializes the full DPP with `DppCodec<T>`.
- `readDppById`, `readDppByProductId`, `readDppVersionByProductIdAndDate`, and `updateDppById` read a full DPP payload and deserialize it through `DppCodec<T>`.
- `updateDppById` sends a partial JSON merge-style patch as `JsonNode`. It does not validate before sending because it is not a full DPP.
- `readDataElement` and `updateDataElement` use `JsonNode` because fine-granular values can be strings, numbers, objects, arrays, or booleans.
- `updateDataElement` does not validate before sending because it is a partial element update.
- At this stage, wrapper payload handling does not distinguish a missing fine-granular `payload` field from an explicit JSON `null` payload; both are treated as invalid wrapper payloads, so `payload: null` is not yet supported for fine-granular reads or updates.
- Requests send `Accept: application/json` and only send `Content-Type: application/json` when a body is present.

## Example

```java
DppRepoClient<MyDpp> repoClient = new HttpDppRepoClient<>(
    "http://localhost:8080",
    myCodec,
    myValidator
);

CreateDppResponse created = repoClient.createDpp(dpp);
MyDpp loaded = repoClient.readDppById(created.getDppId());

JsonNode name = repoClient.readDataElement(
    created.getDppId(),
    "characteristics.productName"
);

ObjectMapper mapper = new ObjectMapper();
JsonNode patch = mapper.readTree("{\"characteristics\":{\"productName\":\"Updated Name\"}}");
MyDpp updated = repoClient.updateDppById(created.getDppId(), patch);

RegisterDppRequest request = new RegisterDppRequest(
    productIdentifier,
    dppIdentifier,
    operatorIdentifier,
    repoUrl
);

RegisterDppResponse registered = registryClient.postNewDppToRegistry(request);
```

## Errors

Client failures use runtime exceptions:

- `DppValidationClientException` for validator failures before a request is sent
- `DppMappingClientException` for codec or client-side JSON mapping failures
- `DppHttpClientException` for non-2xx HTTP responses
- `DppNetworkClientException` for network, timeout, I/O, or interruption failures
- `DppApiClientException` for 2xx HTTP responses whose wrapper `statusCode` is an API error

## Retry Note

Automatic retries are intentionally not implemented in this stage. Write operations such as create, patch, delete, register, and element update are not retried automatically because they are not guaranteed to be idempotent. If a request times out after the server processed it, retrying automatically could create duplicate or conflicting behavior. A later stage may add limited retry support for safe read-only operations once idempotency and operational requirements are defined.

## Next-Stage Requirements / Not Implemented Yet

- backup provider / backup operator registration
- configurable timeout settings
- retry configuration
- authentication and authorization
- OAuth/token handling
- access-control rules
- persistent database-backed mock services
- real EC registry integration
- real archiving service integration
- production monitoring/observability

Backup provider / backup operator support is intentionally not part of the current client API. The active registry request contains only `productIdentifier`, `dppIdentifier`, `operatorIdentifier`, and `repoUrl`. Backup-related identifiers should be added later only after the expected registry contract and failover/archive behavior are defined.

## Scope

This library intentionally does not support:

- authentication or custom headers
- configurable endpoint templates
- mock clients
- retries, pagination, caching, or async behavior
- Cir4Fun model dependencies in the main client library
