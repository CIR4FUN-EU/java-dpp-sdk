# DPP Backend Demo

Small partner-facing backend demo for the Cir4Fun DPP SDK.

It contains two Spring Boot services:

- `mock-eu-registry-service`: simulates an EU DPP registry on port `8081`
- `dpp-producer-service`: command-line producer demo that builds DPPs with the SDK and sends them to the registry

The demo intentionally avoids production infrastructure. There is no database, no real authentication, no Kafka, no Docker, and no real EU registry integration.

The producer also includes one explicit SDK `TestDataFactory` example. That factory is part of the SDK test JAR, so it is useful for demos and integration tests, while normal application code should prefer SDK builders or its own local factories.

## What This Shows

- Building `Cir4FunFurnitureDpp` objects with SDK builders
- Serializing and deserializing DPP JSON with SDK `DppJsonCodec`
- Validating DPPs with SDK `ValidationService`
- Sending DPP JSON between two backend services over HTTP
- Demonstrating accepted, rejected, malformed, query, update, and delete flows

## Prerequisite

Build and install the SDK locally first:

```powershell
cd DPP_SDK
mvn clean install
```

or, if the SDK project uses the Maven wrapper:

```powershell
cd DPP_SDK
.\mvnw.cmd clean install
```

This demo depends on:

```xml
<groupId>com.example.dppsdk</groupId>
<artifactId>dpp-sdk</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

## Build The Demo

```powershell
cd dpp-backend-demo
mvn clean package
```

If Maven is not on PATH but IntelliJ is installed, its bundled Maven can also be used.

## Run The Registry

Terminal 1:

```powershell
java -jar mock-eu-registry-service\target\mock-eu-registry-service-1.0.0-SNAPSHOT.jar --debug=false
```

Registry base URL:

```text
http://localhost:8081
```

## Run The Producer

Terminal 2:

```powershell
java -jar dpp-producer-service\target\dpp-producer-service-1.0.0-SNAPSHOT.jar --debug=false
```

To use a different registry URL:

```powershell
java -jar dpp-producer-service\target\dpp-producer-service-1.0.0-SNAPSHOT.jar http://localhost:8081 --debug=false
```

## Expected Demo Output

The producer runs these scenarios:

1. Valid manual DPP accepted
2. Valid factory DPP accepted
3. SDK `TestDataFactory` valid DPP accepted
4. Invalid validation case rejected
5. SDK `TestDataFactory` invalid documentation case rejected
6. Malformed JSON rejected
7. Accepted DPP queried back and mapped to SDK domain object
8. Valid DPP update accepted
9. DPP list and delete confirmed

You should see output like:

```text
=== 1. Valid manual DPP ===
success : true
status  : ACCEPTED

=== 3. SDK TestDataFactory quick example ===
success : true
status  : ACCEPTED

=== 4. Invalid validation case ===
success : false
status  : REJECTED
message : Validation failed: Nameplate.supplier must have role SUPPLIER, but got DISTRIBUTOR

=== 7. Query back and map with SDK codec ===
Product name   : Cir4Fun Platform Bed
DPP ID         : 49192c87-20c8-4b6f-88de-48b56ca4c211
Classification : Furniture / Beds
Manufacturer   : Cir4Fun Furniture GmbH
```

## Registry Endpoints

Base URL: `http://localhost:8081`

- `POST /dpps`: accepts DPP JSON, maps with SDK `DppJsonCodec`, validates with SDK `ValidationService`, stores in memory
- `GET /dpps/{id}`: returns a stored DPP as SDK-produced JSON inside `jsonPayload`
- `PUT /dpps/{id}`: maps, validates, and updates an existing DPP
- `DELETE /dpps/{id}`: deletes a stored DPP
- `GET /dpps`: lists stored DPP summaries

## What Is Mocked

- EU registry behavior
- Credentials/API key handling
- Persistence, using only an in-memory `Map`

## What Is Not Implemented

- Real EU registry API calls
- Real authentication or authorization
- Database persistence
- Production security, retries, auditing, or monitoring
- Kafka, Docker, or external infrastructure

## SDK Usage

Both services use the SDK as a Maven dependency.

The registry uses:

- `DppJsonCodec#fromJson`
- `DppJsonCodec#toJson`
- `ValidationService#validate`

The producer uses:

- SDK builders for `Cir4FunFurnitureDpp` and nested domain objects
- SDK `dppsdk.support.TestDataFactory` for one quick demo/test-fixture example
- `DppJsonCodec#toJson` before sending HTTP requests
- `DppJsonCodec#fromJson` after receiving stored DPP JSON from the registry

The `TestDataFactory` dependency is intentionally limited to the producer demo:

```xml
<dependency>
    <groupId>com.example.dppsdk</groupId>
    <artifactId>dpp-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <classifier>tests</classifier>
</dependency>
```

For real application code, treat that factory as a convenience for demos/tests, not as the main SDK API.
