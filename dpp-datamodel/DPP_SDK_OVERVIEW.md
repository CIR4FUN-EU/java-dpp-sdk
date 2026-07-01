# DPP SDK Scope

This project is a v1 reference Java SDK for a Dpp4Fun furniture Digital Product Passport data model.

The repository is organized as a multi-module Maven build:

- parent/aggregator artifact: `dpp.datamodel:dpp-datamodel:0.3.0`
- reusable core module: `dpp-core`
- furniture-specific module: `dpp4fun`

## Implemented

- immutable core and furniture domain models with builders
- reusable common DPP structure through `DppCore`
- furniture aggregate model through `Dpp4Fun`
- semantic validation through core validators and `Dpp4FunValidationService`
- domain/payload mappers with canonical nested `coreDpp` payload structure
- JSON serialization and deserialization through `Dpp4FunJsonCodec`
- flat outbound / nested inbound compatibility for transport JSON
- automated tests and test fixtures

The mapper layer is the direct conversion layer between domain objects and payload DTOs. `Dpp4FunJsonCodec` builds on top of it when callers want JSON plus optional validation in one call.

## Identifiers Used By Standard APIs

- DPP ID maps to `PassportMetadata.uniqueProductIdentifier`.
- Product ID maps to `Nameplate.gtinCode`.
- Callers can use `dpp.getDppId()`, `dpp.getProductId()`, `DppIdentifiers.dppId(dpp)`, and `DppIdentifiers.productId(dpp)`.

## Not Implemented

- persistence or database integration
- authentication, authorization, or security policy
- registry submission or registry client logic
- external API validation, URL checks, GLN checks, or taxonomy lookups
- full EU regulatory completeness
- the full draft data model
- AAS adapters or export modules
- warning collection or partial validation modes
- backup-related fields

The SDK is a domain/modeling foundation. It should not be treated as a production compliance system by itself.
