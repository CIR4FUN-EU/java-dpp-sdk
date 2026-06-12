# Validation Guide

Validation is separated from the model layer. Builders check local structure; validators check semantic rules.

## Validation Layers

### Core Validation

```text
ValidationService
+-- DppCoreValidator
    +-- PassportMetadataValidator
    +-- NameplateValidator
    +-- DocumentationValidator
    +-- OrganizationValidator
    +-- ContactValidator
    +-- AddressValidator
    +-- EmailValidator
    +-- TelephoneValidator
```

`dppsdk.core.validation.ValidationService` is core-safe and does not depend on `dpp4fun`.

### Dpp4Fun Validation

```text
Dpp4FunValidationService
+-- ValidationService
+-- Dpp4FunValidator
    +-- DppCoreValidator
    +-- ProductClassificationValidator
    +-- CharacteristicsValidator
    +-- BillOfMaterialsValidator
```

`Dpp4FunValidator` owns the current cross-object rules:

- classification category versus characteristics product type
- metadata external documentation link versus documentation object presence

## What Validation Does Not Do

Validators do not:

- call external APIs
- check URLs against live services
- verify GLNs against a registry
- persist data
- mutate models
- enrich or default missing values
- collect warnings

Validation is fail-fast: the first failing rule throws `ValidationException`.
