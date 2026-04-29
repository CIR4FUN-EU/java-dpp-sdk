# DPP Client

Generic Java HTTP clients for standardized Digital Product Passport (DPP) registry and repository endpoints.

This library is intentionally small. It provides only:

- generic client interfaces
- Java `HttpClient` implementations
- simple response classes
- separated client exceptions

It does not provide DPP domain models, builders, validation rules, mapping logic, payload classes, authentication, endpoint customization, mock clients, databases, or backend wiring.

## Local Dependency

If you installed this project locally with `mvn install`, `./mvnw install`, or `mvnw.cmd install`, consume it from another Maven project with:

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

Consumers provide:

- a `DppCodec<T>` to convert a DPP object to and from JSON
- a `DppValidator<T>` to validate a DPP before write operations
- a base URL for the registry or repository service

```java
DppRepoClient<MyDpp> repoClient = new HttpDppRepoClient<>(
    "https://api.example.com",
    new MyDppCodecAdapter(),
    new MyDppValidatorAdapter()
);
```

```java
DppRegistryClient<MyDpp> registryClient = new HttpDppRegistryClient<>(
    "https://api.example.com",
    new MyDppCodecAdapter(),
    new MyDppValidatorAdapter()
);
```

In this SDK setup, those codec and validator implementations typically wrap classes from `dpp-data-model`. For the Cir4Fun model, that usually means `dppsdk.transport.DppJsonCodec` for JSON and `dppsdk.validation.ValidationService` for validation. This client does not depend on a specific DPP model, though, so it will also accept implementations for other DPP types as long as they provide `DppCodec<T>` and `DppValidator<T>`.

```text
DPP type T
  -> DppCodec<T>
  -> DppValidator<T>
  -> HttpDppRegistryClient<T> / HttpDppRepoClient<T>
```

Example:

```java
import dppsdk.model.Cir4FunFurnitureDpp;
import dppsdk.transport.DppJsonCodec;
import dppsdk.validation.ValidationService;

Cir4FunFurnitureDpp dpp = ...;

DppJsonCodec jsonCodec = new DppJsonCodec();
ValidationService validationService = new ValidationService();

DppCodec<Cir4FunFurnitureDpp> codec = new DppCodec<>() {
    @Override
    public String toJson(Cir4FunFurnitureDpp dpp) {
        return jsonCodec.toJson(dpp);
    }

    @Override
    public Cir4FunFurnitureDpp fromJson(String json) {
        return jsonCodec.fromJson(json);
    }
};

DppValidator<Cir4FunFurnitureDpp> validator = validationService::validate;

DppRepoClient<Cir4FunFurnitureDpp> repoClient = new HttpDppRepoClient<>(
    "https://api.example.com",
    codec,
    validator
);

RepoResponse created = repoClient.create(dpp);
Cir4FunFurnitureDpp loaded = repoClient.get(dpp.id());
```

## Standard Endpoints

The clients use fixed endpoint paths.

Registry:

- `POST /registry/dpps/register`

Repository:

- `POST /repo/dpps`
- `GET /repo/dpps/{id}`
- `PUT /repo/dpps/{id}`
- `DELETE /repo/dpps/{id}`
- `GET /repo/dpps`

The `{id}` path segment is URL-encoded before requests are sent.

## Public Interfaces

Registry client:

```java
RegistryResponse registerDpp(T dpp);
```

Repository client:

```java
RepoResponse create(T dpp);
T get(String id);
RepoResponse update(String id, T dpp);
RepoResponse delete(String id);
List<String> list();
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

Full DPP JSON serialization and deserialization is handled by the injected `DppCodec<T>`, not by this client library. The client only reads small response envelope fields such as `success`, `status`, `message`, `dppId`, and list IDs.

## Behavior

`HttpDppRegistryClient<T>` validates, serializes, and posts a DPP to the fixed registry endpoint.

`HttpDppRepoClient<T>` supports repository CRUD:

- create and update validate and serialize before sending
- get deserializes the DPP response through the supplied codec
- delete returns a simple `RepoResponse`
- list accepts either `["id-1","id-2"]` or `{"ids":["id-1","id-2"]}`

Requests send JSON `Accept` headers and `Content-Type` only when a request body exists. Auth headers are not supported for now.

## Errors

Client failures use runtime exceptions:

- `DppValidationClientException` for validator failures before a request is sent
- `DppMappingClientException` for codec or response mapping failures
- `DppHttpClientException` for non-2xx HTTP responses, including status code and response body
- `DppNetworkClientException` for network, timeout, I/O, or interruption failures

## Current Scope

For this SDK foundation, the client intentionally does not support:

- authentication or custom headers
- configurable endpoint templates
- mock clients
- retries, pagination, caching, or async behavior
