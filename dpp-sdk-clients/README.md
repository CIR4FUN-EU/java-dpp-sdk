# DPP SDK Clients

## Purpose

`dpp-sdk-clients` provides generic Java HTTP clients and payload contracts for repository and registry APIs. The client modules stay model-independent: callers provide their own DPP type plus codec and validation hooks where needed.

The clients implement selected EN 18222:2026-facing contracts but are not fully aligned. They do not claim final EN compliance, certification, or legal conformity.

Parent coordinates from `dpp-sdk-clients/pom.xml`:

- `groupId`: `dpp.client`
- `artifactId`: `dpp-sdk-clients`
- `version`: `0.4.0`
- packaging: `pom`

## Module Map

| Module | Coordinates | What it provides |
| --- | --- | --- |
| `dpp-repo-payloads` | `dpp.client:dpp-repo-payloads:0.4.0` | Repository wrapper DTOs and request/response contracts |
| `dpp-repo-client` | `dpp.client:dpp-repo-client:0.4.0` | `DppRepoClient<T>`, `HttpDppRepoClient<T>`, `DppCodec<T>`, `DppValidator<T>`, repo exceptions |
| `dpp-registry-payloads` | `dpp.client:dpp-registry-payloads:0.4.0` | Registry wrapper DTOs and registration request/response contracts |
| `dpp-registry-client` | `dpp.client:dpp-registry-client:0.4.0` | `DppRegistryClient`, `HttpDppRegistryClient`, registry exceptions |

## Build And Install

Run from `dpp-sdk-clients`.

Build all client modules:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean install
```

```bash
./mvnw test
./mvnw clean install
```

Build only the repository client:

```powershell
.\mvnw.cmd -pl "dpp-repo-client" -am test
```

```bash
./mvnw -pl "dpp-repo-client" -am test
```

Build only the registry client:

```powershell
.\mvnw.cmd -pl "dpp-registry-client" -am test
```

```bash
./mvnw -pl "dpp-registry-client" -am test
```

Build only the payload modules:

```powershell
.\mvnw.cmd -pl "dpp-repo-payloads" -am test
.\mvnw.cmd -pl "dpp-registry-payloads" -am test
```

```bash
./mvnw -pl "dpp-repo-payloads" -am test
./mvnw -pl "dpp-registry-payloads" -am test
```

## Maven Consumption

Repository client:

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-repo-client</artifactId>
    <version>0.4.0</version>
</dependency>
```

Registry client:

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-registry-client</artifactId>
    <version>0.4.0</version>
</dependency>
```

Payload-only usage:

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-repo-payloads</artifactId>
    <version>0.4.0</version>
</dependency>

<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-registry-payloads</artifactId>
    <version>0.4.0</version>
</dependency>
```

## Public Entry Points

Repository client surface:

- `dpp.repo.client.DppRepoClient<T>`
- `dpp.repo.client.HttpDppRepoClient<T>`
- `dpp.repo.client.core.DppCodec<T>`
- `dpp.repo.client.core.DppValidator<T>`
- `dpp.repo.client.exception.DppClientException`
- `dpp.repo.client.exception.DppHttpClientException`
- `dpp.repo.client.exception.DppApiClientException`
- `dpp.repo.client.exception.DppNetworkClientException`
- `dpp.repo.client.exception.DppMappingClientException`
- `dpp.repo.client.exception.DppValidationClientException`

Registry client surface:

- `dpp.registry.client.DppRegistryClient`
- `dpp.registry.client.HttpDppRegistryClient`
- `dpp.registry.client.exception.DppClientException`
- `dpp.registry.client.exception.DppHttpClientException`
- `dpp.registry.client.exception.DppApiClientException`
- `dpp.registry.client.exception.DppNetworkClientException`
- `dpp.registry.client.exception.DppMappingClientException`

Payload contract surface:

- `dpp.repo.payloads.DppApiResponse`
- `dpp.repo.payloads.DppApiMessage`
- `dpp.repo.payloads.DppStatusCode`
- `dpp.repo.payloads.CreateDppResponse`
- `dpp.repo.payloads.DeleteDppResponse`
- `dpp.repo.payloads.ReadDppIdsRequest`
- `dpp.repo.payloads.ReadDppIdsResponse`
- `dpp.registry.payloads.DppApiResponse`
- `dpp.registry.payloads.DppApiMessage`
- `dpp.registry.payloads.DppStatusCode`
- `dpp.registry.payloads.RegisterDppRequest`
- `dpp.registry.payloads.RegisterDppResponse`

## Module Responsibilities

### Payload Modules

`dpp-repo-payloads` and `dpp-registry-payloads` contain DTOs only.

Repository payload examples:

- wrapper DTOs: `DppApiResponse`, `DppApiMessage`, `DppStatusCode`, `MessageType`
- request/response DTOs: `CreateDppResponse`, `DeleteDppResponse`, `ReadDppIdsRequest`, `ReadDppIdsResponse`

Registry payload examples:

- wrapper DTOs: `DppApiResponse`, `DppApiMessage`, `DppStatusCode`, `MessageType`
- request/response DTOs: `RegisterDppRequest`, `RegisterDppResponse`

Payload modules do not contain:

- HTTP clients or transport behavior
- SDK model classes
- validators or mappers
- persistence or mock-service logic

Use the payload artifacts directly when a contract test, backend adapter, or integration layer only needs request/response DTOs and not the client implementations.

### Client Modules

`dpp-repo-client` and `dpp-registry-client` contain low-level HTTP behavior only.

They depend on their matching payload modules and stay model-independent.

Client modules do not contain:

- SDK model classes
- business/domain validation beyond `DppValidator<T>` integration
- mappers
- persistence or mock-service logic
- orchestration between repo and registry flows

## Usage

### Provide `DppCodec<T>` And `DppValidator<T>`

The repository client expects full-DPP JSON serialization and pre-create validation to be supplied by the caller.

```java
import dpp.repo.client.core.DppCodec;
import dpp.repo.client.core.DppValidator;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;

Dpp4FunJsonCodec sdkCodec = new Dpp4FunJsonCodec();
Dpp4FunValidationService sdkValidator = new Dpp4FunValidationService();

DppCodec<Dpp4Fun> codec = new DppCodec<>() {
    @Override
    public String toJson(Dpp4Fun dpp) {
        return sdkCodec.toJson(dpp);
    }

    @Override
    public Dpp4Fun fromJson(String json) {
        return sdkCodec.fromJson(json);
    }
};

DppValidator<Dpp4Fun> validator = sdkValidator::validate;
```

### Create A Repository Client

```java
import dpp.repo.client.DppRepoClient;
import dpp.repo.client.HttpDppRepoClient;

DppRepoClient<Dpp4Fun> repoClient = new HttpDppRepoClient<>(
        "http://localhost:8080",
        codec,
        validator
);
```

### Create A Registry Client

```java
import dpp.registry.client.DppRegistryClient;
import dpp.registry.client.HttpDppRegistryClient;

DppRegistryClient registryClient = new HttpDppRegistryClient("http://localhost:8081");
```

### Repository Lifecycle And Fine-Grained Operations

Supported `DppRepoClient<T>` methods from `dpp-repo-client/src/main/java/dpp/repo/client/DppRepoClient.java`:

- `createDpp(T dpp)`
- `readDppById(String dppId)`
- `readCompressedDppById(String dppId)`
- `readDppByProductId(String productId)`
- `readDppVersionByIdAndDate(String dppId, Instant date)`
- `readDppIdsByProductIds(List<String> productIds, Integer limit, String cursor)`
- `updateDppById(String dppId, JsonNode partialDpp)`
- `deleteDppById(String dppId)`
- `readDataElement(String dppId, String elementIdPath)`
- `updateDataElement(String dppId, String elementIdPath, JsonNode dataElement)`

The deprecated `readDppVersionByProductIdAndDate` method remains available only as non-standard compatibility; new callers should use the DPP-ID method.

Example:

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DeleteDppResponse;

CreateDppResponse created = repoClient.createDpp(dpp);
Dpp4Fun stored = repoClient.readDppById(created.getDppId());

ObjectNode patch = new ObjectMapper()
        .createObjectNode()
        .putObject("characteristics")
        .put("productName", "Updated via client");

Dpp4Fun updated = repoClient.updateDppById(stored.getDppId(), patch);
DeleteDppResponse deleted = repoClient.deleteDppById(updated.getDppId());
```

Fine-granular element access:

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

JsonNode productName = repoClient.readDataElement(dpp.getDppId(), "$.characteristics.productName");
JsonNode changed = repoClient.updateDataElement(
        dpp.getDppId(),
        "$.characteristics.productName",
        JsonNodeFactory.instance.textNode("Updated element value")
);
```

### Register DPP Metadata

The registry client currently exposes registration only.

```java
import dpp.registry.payloads.RegisterDppRequest;
import dpp.registry.payloads.RegisterDppResponse;

RegisterDppResponse registered = registryClient.postNewDppToRegistry(
        new RegisterDppRequest(
                dpp.getProductId(),
                dpp.getDppId(),
                "operator-123",
                "http://localhost:8080"
        )
);
```

`RegisterDppRequest` contains the four supported standard-facing fields:

- `uniqueProductIdentifier`
- `digitalProductPassportId`
- `uniqueEconomicOperatorIdentifier`
- `dppApiEndpoint`

The response uses `registrationId`. Backup-operator behavior remains out of scope.

## Payload Notes

`ReadDppIdsRequest` uses:

- `productIdentifiers`
- optional `limit`
- optional `cursor`

`ReadDppIdsResponse` returns:

- `dppIdentifiers`
- optional `nextCursor`

Fine-granular PATCH sends the data element directly as JSON.

Fine-granular `elementIdPath` uses a bounded RFC 9535-compatible singular subset: `$`, dot members, quoted bracket members, and non-negative array indexes. Wildcards, descendants, unions, slices, filters, functions, and negative indexes are not supported by the mock repository and return HTTP 501; malformed paths return 400 and a selected path with no matching node returns 404. PATCH sends direct JSON, targets one existing value, and persists only after whole-DPP validation succeeds.

Typed read methods returning `T` explicitly request `representation=full`. `readCompressedDppById` returns `JsonNode`, so the project-defined provisional compressed summary is never decoded into the caller's DPP model. On the mock server, an omitted representation defaults to `compressed` under EN 18222 Clause 8.1; the concrete compressed shape is not claimed as EN 18223-compliant.

## Endpoint Coverage

Repository client coverage:

- `POST /v1/dpps`
- `GET /v1/dpps/{dppId}`
- `GET /v1/dppsByProductId/{productId}`
- `GET /v1/dppsByIdAndDate/{dppId}?date={instant}`
- `POST /v1/dppsByProductIds`
- `PATCH /v1/dpps/{dppId}`
- `DELETE /v1/dpps/{dppId}`
- `GET /v1/dpps/{dppId}/elements/{elementIdPath}`
- `PATCH /v1/dpps/{dppId}/elements/{elementIdPath}`

Registry client coverage:

- `POST /v1/registerDPP`

The mock registry in `dpp-sdk-demo` also exposes metadata lookup endpoints, but those lookup endpoints are demo-local and are not part of `dpp-sdk-clients`.

## Exception Handling

Catch the client-specific base exception when you want one boundary for API, HTTP, mapping, validation, and network failures.

```java
try {
    repoClient.createDpp(dpp);
} catch (dpp.repo.client.exception.DppClientException exception) {
    System.err.println(exception.getMessage());
}

try {
    registryClient.postNewDppToRegistry(request);
} catch (dpp.registry.client.exception.DppClientException exception) {
    System.err.println(exception.getMessage());
}
```

## Boundaries

- The client modules are generic and model-independent.
- They do not own SDK model classes, validators, payload mappers, or JSON codecs for concrete DPP types.
- They do not own persistence logic, mock-service behavior, or demo orchestration.
- They do not document or guarantee production auth, retries, pagination, caching, or async behavior.
