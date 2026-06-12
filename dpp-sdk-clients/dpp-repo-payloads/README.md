# dpp-repo-payloads

`dpp-repo-payloads` contains repository API communication contracts for the draft-prEN-18222-aligned DPP repository surface.

This module contains DTOs only:

- repository wrapper DTOs such as `DppApiResponse`, `DppApiMessage`, `DppStatusCode`, and `MessageType`
- repository request/response DTOs such as `CreateDppResponse`, `DeleteDppResponse`, `ReadDppIdsRequest`, `ReadDppIdsResponse`, and `UpdateDataElementRequest`

This module must not contain:

- HTTP clients or transport behavior
- SDK or Cir4Fun domain models
- validators or mappers
- persistence or mock service logic
- registry client code

Dependency snippet:

```xml
<dependency>
    <groupId>dpp.client</groupId>
    <artifactId>dpp-repo-payloads</artifactId>
    <version>0.3.0</version>
</dependency>
```

Use this artifact when a backend service, contract test, or integration layer only needs the repository request/response contracts and does not need the HTTP client implementation.
