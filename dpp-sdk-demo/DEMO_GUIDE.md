# DPP SDK Demo Guide

A presentation sequence for the SDK and demo-service flow. The [demo README](README.md) owns build, configuration, health checks and cleanup — check those first.

## Preflight

Start both APIs, confirm their health, and have the Swagger or Postman views you plan to show ready. Then run `all` from the repository root — it runs the SDK capability flow first, then the HTTP lifecycle flow:

```bash
java -jar dpp-sdk-demo/dpp-integration-demo/target/dpp-integration-demo-0.6.0.jar all --debug=false
```

Expect `SDK capability demo complete` followed by `HTTP services demo complete`. For an SDK-only presentation use `sdk` instead; it needs no services.

## Suggested flow

1. **Set the boundary.** A working demo repository service — SDK validation, PostgreSQL-backed revisions, history — plus a metadata-only *mock* registry standing in for a real EU registry. Neither is production-hardened, and neither is a certification claim.
2. **Health and API discovery.** Refresh both `/health` endpoints, then the Swagger UIs. `/v1/...` is the public API; `/internal/...` is demo support, and the registry's lookup routes there are not part of the public client.
3. **SDK capability flow.** In the runner output, point out `validation: valid`, the typed Chair DPP fields, the immutable edit with `original retained unchanged: true`, and FULL/COMPRESSED serialization. The runner also shows validation and malformed-JSON errors.
4. **Create and read.** The HTTP flow converts the typed object to neutral JSON at the application boundary, then uses the JSON-only repository client. Show the created DPP ID, and the compressed read beside the explicit FULL one — an omitted representation is compressed; FULL carries the complete model data.
5. **Update and inspect history.** Show the product-ID lookup, the typed immutable edit submitted through the generic client, the changed brand, and the historical read.
6. **Fine-granular operations.** Use `$.elements[1].elements[5]` for the Nameplate `brand` DataElement, or `$.elements[0].elements[0].value[0].elements[0]` for the first BOM material name. These indexes follow the runner fixture in FULL representation, and only singular root/member/index selectors are supported.
7. **Register.** Show the returned registration ID. The registry stores the supplied identifiers and repository endpoint as metadata; it does not contact the repository or copy its JSON.
8. **Handled failures.** Point out the expected 404 for a missing DPP, the handled network error for an unreachable registry, and the final soft delete (`SuccessNoContent`).

## Points worth making

- Builders create immutable canonical models; profile validation reports missing required values and semantic errors.
- A valid full DPP can be created, read, partially updated, addressed at a data element, read historically, and soft-deleted.
- Registry registration is independent of repository reachability and of the DPP existing at all.
- The HTTP demo's *application* validates its deliberately incomplete typed DPP before sending; the repository client itself stays model-independent and just transports JSON. The repository validates what it receives.
- Fine-granular paths: malformed → 400, valid but missing → 404, unsupported selector → 501. Do not present this bounded subset as full RFC 9535 support.

## Postman sequence

1. `dpp-lifecycle-api.verified-export-shape.postman_collection.json` — create/read/update/version/delete.
2. `dpp-fine-granular-api.import-safe.postman_collection.json` — singular reads and direct JSON PATCH values.
3. `dpp-registry-api.verified-export-shape.postman_collection.json` — register metadata, inspect the registry's demo lookup responses.

Run the repository collection first only for the combined create-and-register story. The collections mutate their target data, so prefer a disposable memory-mode session for repeatable demonstrations.

## If something goes wrong

- `all` exits during preflight: confirm both health endpoints, then see the README's [troubleshooting](README.md#troubleshooting).
- A Postman request fails after data changed: switch to a fresh memory-mode session. PostgreSQL data persists.
- Asked about an unsupported API or standard requirement: separate what the demo implements from complete standards conformity, and point to the relevant module guide.

Keep the presentation on the DPP flow and its observable API behavior, not Docker details — send setup questions to the [README](README.md). The takeaway: the SDK separates domain modeling, validation, transport, persistence, repository access and registry metadata, while the demo runtime stays replaceable.
