# Validation Rules

This file lists the validation rules currently implemented by the SDK. All rules are fail-fast errors.

## Aggregate Rules

`Dpp4Fun` must include:

- `coreDpp`
- `classification`
- `characteristics`

`DppCore` must include:

- `passportMetadata`
- `nameplate`

`documentation` and `billOfMaterials` are optional unless another rule below makes a specific object state invalid.

## Cross-Object Rules

- If `DppCore.passportMetadata.externalDocumentationLink` exists, `DppCore.documentation` must exist.
- If both `ProductClassification.category` and `Characteristics.productType` exist, they must not be obviously inconsistent according to the current lightweight string matching rule.

## Object Rules

### PassportMetadata

- `uniqueProductIdentifier` must exist
- `passportUpdateDates` must not be empty
- `passportUpdateDates` must not contain null values
- `passportUpdateDates` must not contain future dates
- `externalDocumentationLink` must not be blank if present
- `qrCodeOrDigitalTag` must not be blank if present

### ProductClassification

- `sector` must exist
- `category` must exist
- `group` must not be blank if present
- `subCategory` must not be blank if present
- `tags` must not contain null, blank, or duplicate values

### Characteristics

- `productName` must exist
- `weight` must be non-negative if present
- `features` must not contain null, blank, or duplicate values
- `dimensions` must be valid if present

### Dimensions

- if any of `width`, `height`, or `depth` is present, `unit` must exist
- dimension values must be non-negative
- at least one dimension value must exist if a `Dimensions` object exists

### Nameplate

- `gtinCode` must exist
- at least one of `manufacturer` or `supplier` must exist
- `manufacturer.role` must be `MANUFACTURER` if `manufacturer` exists
- `supplier.role` must be `SUPPLIER` if `supplier` exists

### Organization

- `name` must exist
- `uri` must not be blank if present
- `contact` must be valid if present

### Contact

- `organization` must exist
- at least one of `address`, `email`, or `telephone` must exist
- any provided contact channel must be valid

### Address

- `country` must exist
- `town` must exist
- optional address fields must not be blank if present

### Email

- `emailAddress` must exist
- `emailAddress` must contain `@`
- `typeOfEmail` must not be blank if present

### Telephone

- `telephoneNumber` must exist
- `typeOfTelephone` must not be blank if present

### Documentation

- `Documentation` is optional
- documentation links must not be blank if present
- if `downloadable` is true, at least one documentation link must exist
- `availableForYears` must be non-negative if present
- if `availableForYears` exists, at least one documentation link must exist

### BillOfMaterials

- `BillOfMaterials` is optional
- `materials`, `components`, and `parts` must not contain null entries
- each entry must be valid
- entries must be unique by `name + reference` when reference exists, otherwise by `name`

### Material

- `name` must exist
- `portion` must be non-negative
- if `mandatory` is true, `portion` must be greater than zero
- `reference` must not be blank if present

### Component

- `name` must exist
- `reference` must not be blank if present

### Part

- `name` must exist
- `reference` must not be blank if present
