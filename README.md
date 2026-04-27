# DPP Client

Generic Java HTTP clients for interacting with standardized Digital Product Passport (DPP) registry and repository endpoints.

This project is intentionally small. It provides client-side transport behavior only:

- generic client interfaces
- Java `HttpClient` implementations
- in-memory mock client implementations for tests and demos
- configuration classes
- response classes
- client exception hierarchy

It does not implement DPP domain models, validation rules, mapping logic, payload classes, Spring Boot services, HTTP mock services, databases, or backend/demo wiring.

## Quick Start

The clients need three things from the consumer:

- a `DppCodec<T>` to convert your DPP object to and from JSON
- a `DppValidator<T>` to validate your DPP before send/store
- a `DppClientConfig` plus `DppEndpointConfig` to define base URL, timeouts, auth, and paths

Default values are already built in for the common case:

- `authHeaderName = "Authorization"`
- `connectTimeout = 5 seconds`
- `requestTimeout = 10 seconds`
- `standardRegistry()` uses `POST /dpps`
- `standardRepo()` uses `POST /dpps`, `GET /dpps/{id}`, `PUT /dpps/{id}`, `DELETE /dpps/{id}`, `GET /dpps`

Minimal HTTP setup:

```java
DppClientConfig config = DppClientConfig.builder("https://api.example.com")
    .build();

DppRepoClient<MyDpp> repoClient = new HttpDppRepoClient<>(
    config,
    DppEndpointConfig.standardRepo(),
    new MyDppCodecAdapter(),
    new MyDppValidatorAdapter()
);
```

Registry use:

```java
DppRegistryClient<MyDpp> registryClient = new HttpDppRegistryClient<>(
    config,
    DppEndpointConfig.standardRegistry(),
    new MyDppCodecAdapter(),
    new MyDppValidatorAdapter()
);
```

Mock use:

```java
MockDppRepoClient<MyDpp> mockRepo = new MockDppRepoClient<>(
    new MyDppValidatorAdapter(),
    MyDpp::id
);
```

If you want mock serialization simulation, pass a codec and `true`:

```java
MockDppRepoClient<MyDpp> mockRepo = new MockDppRepoClient<>(
    new MyDppValidatorAdapter(),
    MyDpp::id,
    new MyDppCodecAdapter(),
    true
);
```

## Exposed Methods

Registry client:

- `registerDpp(T dpp)`

Repo client:

- `create(T dpp)`
- `get(String id)`
- `update(String id, T dpp)`
- `delete(String id)`
- `list()`

Mock registry client:

- `registerDpp(T dpp)`
- `contains(String id)`
- `findRegistered(String id)`
- `registeredIds()`
- `clear()`

Mock repo client:

- `create(T dpp)`
- `get(String id)`
- `update(String id, T dpp)`
- `delete(String id)`
- `list()`
- `contains(String id)`
- `size()`
- `clear()`

## Customization

You can customize:

- base URL with `DppClientConfig.builder("https://api.example.com")`
- auth token with `authToken(...)`
- auth header name with `authHeaderName(...)`
- connect timeout with `connectTimeout(...)`
- request timeout with `requestTimeout(...)`
- registry endpoint paths with `DppEndpointConfig.standardRegistry()` or `DppEndpointConfig.builder()`
- repo endpoint paths with `DppEndpointConfig.standardRepo()` or `DppEndpointConfig.builder()`
- mock ID extraction with `Function<T, String>`
- mock validation with `DppValidator<T>`
- optional mock serialization simulation with `DppCodec<T>` and `true`
- deterministic mock failures with `failNextRequestWithNetworkError()`, `failNextRequestWithHttpError(...)`, `setAlwaysFailNetwork(...)`, and `setAlwaysFailHttp(...)`

ID path templates use `{id}` and the client URL-encodes the value before sending.

## Relationship to the DPP SDK

The existing DPP SDK remains responsible for:

- DPP domain representation
- validation
- mapping
- payloads
- JSON transport conversion

This client library does not duplicate that logic. Consumers provide small adapters around SDK functionality through these generic interfaces:

```java
public interface DppCodec<T> {
    String toJson(T dpp);
    T fromJson(String json);
}

public interface DppValidator<T> {
    void validate(T dpp);
}
```

The client uses those abstractions and does not directly depend on a specific DPP type such as `Cir4FunFurnitureDpp`.

## Registry Client vs Repo Client

`DppRegistryClient<T>` is for registration/submission to a registry:

```java
RegistryResponse registerDpp(T dpp);
```

The HTTP implementation validates the DPP, serializes it, posts JSON to the configured registry endpoint, and returns a simple `RegistryResponse`.

`DppRepoClient<T>` is for repository CRUD:

```java
RepoResponse create(T dpp);
T get(String id);
RepoResponse update(String id, T dpp);
RepoResponse delete(String id);
List<String> list();
```

Create and update validate and serialize before sending. Get deserializes response JSON into `T`. Delete returns a simple response object. List assumes either a JSON string array such as `["id-1","id-2"]` or an object with an `ids` string array such as `{"ids":["id-1","id-2"]}`.

## Configuration

Use `DppClientConfig` for transport settings:

```java
DppClientConfig config = DppClientConfig.builder("https://repo.example")
    .authToken("token-or-api-key")
    .authHeaderName("Authorization")
    .connectTimeout(Duration.ofSeconds(5))
    .requestTimeout(Duration.ofSeconds(10))
    .build();
```

If an auth token is configured, it is sent using the configured header name. No OAuth, token refresh, credential validation, retries, pagination, caching, or async behavior is implemented.
The token value is sent as provided, so consumers should include any scheme prefix such as `Bearer ` when their endpoint expects it.

Use `DppEndpointConfig` for paths:

```java
DppEndpointConfig registryEndpoints = DppEndpointConfig.standardRegistry();
DppEndpointConfig repoEndpoints = DppEndpointConfig.standardRepo();
```

Defaults:

- registry: `POST /dpps`
- repo: `POST /dpps`, `GET /dpps/{id}`, `PUT /dpps/{id}`, `DELETE /dpps/{id}`, `GET /dpps`

## Mock Clients

The library also includes in-memory mock clients in `dpp.client.mock`:

- `MockDppRegistryClient<T>`
- `MockDppRepoClient<T>`

These are reusable test/demo clients. They implement the same public interfaces as the HTTP clients, but they do not perform HTTP calls and do not represent real registry or repository integration.

Mock clients still use the consumer-provided validator:

```java
MockDppRepoClient<Cir4FunFurnitureDpp> repo = new MockDppRepoClient<>(
    new Cir4FunDppValidatorAdapter(),
    Cir4FunFurnitureDpp::id
);
```

Because the client is generic, mocks do not know how to read an ID from `T`. Consumers provide an ID extractor:

```java
Function<Cir4FunFurnitureDpp, String> idExtractor = dpp -> dpp.id();
```

If the extractor returns `null` or blank, the mock throws `DppMappingClientException` and does not store the DPP.

`MockDppRegistryClient<T>` supports registration plus test inspection helpers:

```java
RegistryResponse response = registry.registerDpp(dpp);
boolean exists = registry.contains("dpp-1");
Optional<Cir4FunFurnitureDpp> stored = registry.findRegistered("dpp-1");
List<String> ids = registry.registeredIds();
registry.clear();
```

`MockDppRepoClient<T>` supports the full repository interface and helper methods:

```java
repo.create(dpp);
Cir4FunFurnitureDpp loaded = repo.get("dpp-1");
repo.update("dpp-1", updatedDpp);
repo.delete("dpp-1");
List<String> ids = repo.list();

int count = repo.size();
boolean exists = repo.contains("dpp-1");
repo.clear();
```

Missing repository IDs are treated like remote 404 responses and throw `DppHttpClientException` with status `404`.

### Optional Serialization Simulation

Mocks can optionally simulate JSON transport by round-tripping through `DppCodec<T>` before storing:

```java
MockDppRepoClient<Cir4FunFurnitureDpp> repo = new MockDppRepoClient<>(
    validator,
    idExtractor,
    codec,
    true
);
```

When enabled, `codec.toJson(dpp)` and `codec.fromJson(json)` are called for create/register/update. Codec failures become `DppMappingClientException`, matching the HTTP client error model.

### Failure Simulation

Both mock clients support deterministic failure simulation:

```java
repo.failNextRequestWithNetworkError();
repo.failNextRequestWithHttpError(503, "{\"error\":\"unavailable\"}");

repo.setAlwaysFailNetwork(true);
repo.setAlwaysFailNetwork(false);

repo.setAlwaysFailHttp(500, "{\"error\":\"down\"}");
repo.clearAlwaysFailHttp();
```

Simulated network failures throw `DppNetworkClientException`. Simulated HTTP failures throw `DppHttpClientException`. One-shot failures apply to the next client operation only.

## Example Backend Wiring

A future backend/demo project can adapt SDK types to the generic client. Those adapters should live in the consumer project, not in this library.

```java
final class Cir4FunDppCodecAdapter implements DppCodec<Cir4FunFurnitureDpp> {
    @Override
    public String toJson(Cir4FunFurnitureDpp dpp) {
        // delegate to the existing SDK JSON transport codec
    }

    @Override
    public Cir4FunFurnitureDpp fromJson(String json) {
        // delegate to the existing SDK JSON transport codec
    }
}

final class Cir4FunDppValidatorAdapter implements DppValidator<Cir4FunFurnitureDpp> {
    @Override
    public void validate(Cir4FunFurnitureDpp dpp) {
        // delegate to the existing SDK validation service
    }
}

DppRepoClient<Cir4FunFurnitureDpp> repoClient = new HttpDppRepoClient<>(
    config,
    DppEndpointConfig.standardRepo(),
    new Cir4FunDppCodecAdapter(),
    new Cir4FunDppValidatorAdapter()
);
```

## Error Handling

Failures are separated by source:

- `DppValidationClientException`: local validation failed before a request was sent.
- `DppMappingClientException`: serialization failed before sending, or deserialization/parsing failed after receiving.
- `DppHttpClientException`: the server returned a non-2xx response. Includes HTTP status code and response body.
- `DppNetworkClientException`: the request could not complete due to I/O, timeout, connection, DNS, or interruption. Interrupted requests restore the interrupt flag before throwing.

This separation keeps local data problems, server rejections, and transport failures distinct.

## Build and Test

```bash
.\mvnw.cmd test
```

On Unix-like shells, use `./mvnw test` instead. The first run downloads the Maven distribution into your local wrapper cache.

The tests use Java's lightweight in-process HTTP server and do not require real registry or repository services.

## Local Maven Dependency

If you want to use this library from another local Maven project before publishing it anywhere, install it into your local Maven repository:

```bash
.\mvnw.cmd install
```

That publishes:

- `groupId`: `dpp.client`
- `artifactId`: `dpp-client`
- `version`: `1.0.0-SNAPSHOT`

Then add it to the other project as a normal Maven dependency:

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

If the other project is in the same workspace, it can just depend on this artifact after you run `.\mvnw.cmd install` here.
Use `.\mvnw.cmd install` in this repository so you do not need a global Maven install.
