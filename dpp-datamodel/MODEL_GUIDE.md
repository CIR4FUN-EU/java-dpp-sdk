# Model Guide

The SDK now has two model packages:

- `dppsdk.core.model`
- `dppsdk.dpp4fun.model`

These classes are the canonical in-memory DPP representation.

## Model Principles

- models are immutable after construction
- builders perform local structural checks only
- models do not perform semantic validation, mapping, JSON serialization, persistence, or network calls
- collection fields are copied on input/output where applicable

## Main Object Structure

```text
Dpp
+-- Dpp4Fun
    +-- DppCore
    |   +-- PassportMetadata
    |   +-- Nameplate
    |   +-- Documentation
    +-- ProductClassification
    +-- Characteristics
    +-- BillOfMaterials
```

## Main Models

### Core

- `DppCore`: common DPP fields shared by DPP types
- `PassportMetadata`: DPP identifier, update dates, QR/digital tag, and optional external documentation link
- `Nameplate`: GTIN and manufacturer/supplier identity data
- `Documentation`: optional product documentation links and availability flags
- `Organization`, `Contact`, `Address`, `Email`, `Telephone`: reusable organizational/contact submodels

### Dpp4Fun

- `Dpp4Fun`: furniture-specific aggregate root
- `ProductClassification`: sector/category grouping and tags
- `Characteristics`: product name, description, brand, product type, dimensions, weight, and features
- `BillOfMaterials`: optional materials, components, and parts

## Reading Common Fields

`Dpp4Fun` keeps the structured submodels available, but also exposes read-only convenience getters for common application use:

```java
UUID dppId = dpp.getUniqueProductIdentifier();
String gtin = dpp.getGtinCode();
String productName = dpp.getProductName();
String category = dpp.getCategory();
```

These methods delegate to `DppCore`, `ProductClassification`, or `Characteristics`. They do not duplicate state.

## Required Structure

At the model-builder level:

- `DppCore` requires `passportMetadata` and `nameplate`
- `Dpp4Fun` requires `coreDpp`, `classification`, and `characteristics`
- `Documentation` and `BillOfMaterials` are optional

Semantic rules beyond these local checks are handled by validators, not model builders.

## `toBuilder()`

Models provide `toBuilder()` so callers can create modified copies without mutating an existing instance.

```java
Characteristics updated = oldCharacteristics.toBuilder()
        .productName("Updated Name")
        .build();
```

Use the same pattern to remove optional nested data or list entries:

```java
Dpp4Fun withoutDocumentation = dpp.toBuilder()
        .coreDpp(dpp.getCoreDpp().toBuilder()
                .documentation(null)
                .build())
        .build();

BillOfMaterials smallerBom = bom.toBuilder()
        .removeMaterial(material)
        .build();
```
