# Model And Validation Guide

This SDK has two model packages:

- `dppsdk.core.model`
- `dppsdk.dpp4fun.model`

The same module also owns the validation packages:

- `dppsdk.core.validation`
- `dppsdk.dpp4fun.validation`

Use this document as the single reference for both model structure and validation behavior.

## Core Principles

- models are immutable after construction
- builders enforce local structural rules only
- validators enforce semantic and cross-object rules
- validation is fail-fast and throws `ValidationException` on the first error
- model classes do not perform HTTP, persistence, mapping, or JSON work

## Object Graph

```text
Dpp
+-- Dpp4Fun
    +-- DppCore
    |   +-- PassportMetadata
    |   +-- Nameplate
    |   +-- Documentation
    +-- ProductClassification
    +-- Characteristics
    |   +-- Dimensions
    +-- BillOfMaterials
        +-- Material
        +-- Component
        +-- Part
```

`Dpp` is the abstract base type. `Dpp4Fun` is the concrete aggregate currently implemented in this repository.

## Validation Entry Points

Core validation:

- `ValidationService`
- `DppCoreValidator`
- the individual core validators for nested value objects

`Dpp4Fun` validation:

- `Dpp4FunValidationService`
- `Dpp4FunValidator`
- the individual furniture-specific validators

`ValidationService` registers the core validators. `Dpp4FunValidationService` layers the `dpp4fun` validators on top of that core service.

## Convenience Accessors

`Dpp` and `Dpp4Fun` expose convenience getters that delegate into nested objects instead of duplicating state:

```java
String dppId = dpp.getDppId();
String productId = dpp.getProductId();
String gtin = dpp.getGtinCode();
String productName = dpp.getProductName();
String category = dpp.getCategory();
```

## Builder And Validator Rules By Type

### `Dpp`

Purpose:
- abstract base class for DPP aggregates
- exposes convenience accessors for `passportMetadata`, `nameplate`, `documentation`, `dppId`, and `productId`

Rules:
- `getDppId()` throws if `PassportMetadata.uniqueProductIdentifier` is missing
- `getProductId()` throws if `Nameplate.gtinCode` is missing

### `DppCore`

Shape:
- `passportMetadata`
- `nameplate`
- optional `documentation`

Builder rules:
- `passportMetadata` is required
- `nameplate` is required

Validator:
- `DppCoreValidator`

Validator rules:
- `DppCore` must not be null
- `passportMetadata` must not be null
- `nameplate` must not be null
- delegates to `PassportMetadataValidator`, `NameplateValidator`, and `DocumentationValidator`

### `PassportMetadata`

Shape:
- `uniqueProductIdentifier`
- `passportUpdateDates`
- optional `qrCodeOrDigitalTag`
- optional `externalDocumentationLink`

Builder rules:
- none beyond normal object construction

Validator:
- `PassportMetadataValidator`

Validator rules:
- object must not be null
- `uniqueProductIdentifier` is required
- `passportUpdateDates` must not be null or empty
- `passportUpdateDates` must not contain null values
- `passportUpdateDates` must not contain future dates
- `qrCodeOrDigitalTag` must not be blank if present
- `externalDocumentationLink` must not be blank if present

### `Nameplate`

Shape:
- `gtinCode`
- optional `internalArticleNumber`
- optional `batchNumber`
- optional `customsTariffNumber`
- optional `uriOfTheProduct`
- optional `manufacturer`
- optional `supplier`

Builder rules:
- `gtinCode` is required and must not be blank
- optional string fields must not be blank if present

Validator:
- `NameplateValidator`

Validator rules:
- object must not be null
- `gtinCode` is required
- optional identifier fields must not be blank if present
- at least one of `manufacturer` or `supplier` must exist
- `manufacturer` is validated if present and must have role `MANUFACTURER`
- `supplier` is validated if present and must have role `SUPPLIER`

### `Organization`

Shape:
- `name`
- optional `gln`
- optional `uri`
- optional `role`
- optional `contact`

Builder rules:
- check the class directly for local construction rules when editing this type

Validator:
- `OrganizationValidator`

Validator rules:
- object must not be null when validator is invoked
- `name` is required
- `uri` must not be blank if present
- `contact` is validated if present
- `role` stays optional here; slot-specific role rules are enforced by parent validators such as `NameplateValidator`

### `Contact`

Shape:
- `organization`
- optional `address`
- optional `email`
- optional `telephone`

Builder rules:
- local construction only

Validator:
- `ContactValidator`

Validator rules:
- `organization` is required
- at least one of `address`, `email`, or `telephone` must exist
- nested values are validated if present

### `Address`

Shape:
- optional `street`
- optional `zipCode`
- required semantic `town`
- optional `region`
- required semantic `country`

Builder rules:
- local construction only

Validator:
- `AddressValidator`

Validator rules:
- `country` is required
- `town` is required
- `zipCode`, `region`, and `street` must not be blank if present

### `Email`

Shape:
- `emailAddress`
- optional `typeOfEmail`

Builder rules:
- local construction only

Validator:
- `EmailValidator`

Validator rules:
- `emailAddress` is required
- `emailAddress` must contain `@`
- `typeOfEmail` must not be blank if present

### `Telephone`

Shape:
- `telephoneNumber`
- optional `typeOfTelephone`

Builder rules:
- local construction only

Validator:
- `TelephoneValidator`

Validator rules:
- `telephoneNumber` is required
- `typeOfTelephone` must not be blank if present

### `Documentation`

Shape:
- optional `digitalInstructionsLink`
- optional `safetyInstructionsLink`
- `downloadable`
- optional `availableForYears`
- `paperCopyAvailableOnRequest`

Builder rules:
- `availableForYears` must be non-negative if present
- documentation links must not be blank if present

Validator:
- `DocumentationValidator`

Validator rules:
- object is optional
- documentation links must not be blank if present
- if `downloadable` is `true`, at least one documentation link must exist
- `availableForYears` must be non-negative if present
- if `availableForYears` is set, at least one documentation link must exist

### `Dpp4Fun`

Shape:
- `coreDpp`
- `classification`
- `characteristics`
- optional `billOfMaterials`

Builder rules:
- `coreDpp` is required
- `classification` is required
- `characteristics` is required

Validator:
- `Dpp4FunValidator`

Validator rules:
- object must not be null
- `coreDpp`, `classification`, and `characteristics` must exist
- delegates to `DppCoreValidator`, `ProductClassificationValidator`, `CharacteristicsValidator`, and `BillOfMaterialsValidator`
- cross-object rules:
  - if both `classification.category` and `characteristics.productType` are present, one must contain the other after lowercasing and trimming
  - if `externalDocumentationLink` exists, `documentation` must also exist

### `ProductClassification`

Shape:
- `sector`
- optional `group`
- `category`
- optional `subCategory`
- `tags`

Builder rules:
- `sector` is required
- `category` is required
- `group` must not be blank if present
- `subCategory` must not be blank if present

Validator:
- `ProductClassificationValidator`

Validator rules:
- object must not be null
- `sector` is required
- `category` is required
- `group` and `subCategory` must not be blank if present
- if `subCategory` has text, `category` must also have text
- if `group` has text, `sector` must also have text
- `tags` must not contain null, blank, or duplicate values

### `Characteristics`

Shape:
- `productName`
- optional `description`
- optional `brand`
- optional `productType`
- optional `dimensions`
- optional `weight`
- optional `color`
- `features`

Builder rules:
- `productName` is required
- `description`, `brand`, `productType`, and `color` must not be blank if present
- `weight` must be non-negative if present

Validator:
- `CharacteristicsValidator`

Validator rules:
- object must not be null
- `productName` is required
- `weight` must be non-negative if present
- `dimensions` is validated if present
- `features` must not contain null, blank, or duplicate values

### `Dimensions`

Shape:
- optional `width`
- optional `height`
- optional `depth`
- optional `unit`

Builder rules:
- local construction only

Validator:
- `DimensionsValidator`

Validator rules:
- object is optional
- `width`, `height`, and `depth` must be non-negative if present
- at least one of `width`, `height`, or `depth` must exist when a `Dimensions` object exists
- if any dimension value exists, `unit` is required

### `BillOfMaterials`

Shape:
- `materials`
- `components`
- `parts`

Builder rules:
- none; empty lists are allowed
- supports `add*`, `remove*`, and `toBuilder()` copy/edit flows

Validator:
- `BillOfMaterialsValidator`

Validator rules:
- object is optional
- `materials`, `components`, and `parts` lists must not contain null entries
- each nested item is validated
- duplicates are rejected per list using a lowercased `name|reference` key

### `Material`

Shape:
- `name`
- `mandatory`
- `portion`
- optional `reference`

Builder rules:
- local construction only

Validator:
- `MaterialValidator`

Validator rules:
- `name` is required
- `portion` must be non-negative
- `reference` must not be blank if present
- if `mandatory` is `true`, `portion` must be greater than zero

### `Component`

Shape:
- `name`
- optional `reference`

Builder rules:
- local construction only

Validator:
- `ComponentValidator`

Validator rules:
- `name` is required
- `reference` must not be blank if present

### `Part`

Shape:
- `name`
- optional `reference`

Builder rules:
- local construction only

Validator:
- `PartValidator`

Validator rules:
- `name` is required
- `reference` must not be blank if present

## Copy / Edit Pattern

Most model classes expose `toBuilder()` so callers can make immutable edits:

```java
Dpp4Fun updated = dpp.toBuilder()
        .characteristics(dpp.getCharacteristics().toBuilder()
                .productName("Updated Name")
                .build())
        .build();
```

List-style value objects also support targeted removal through the builder:

```java
BillOfMaterials smallerBom = dpp.getBillOfMaterials().toBuilder()
        .removeMaterial(material)
        .build();
```
