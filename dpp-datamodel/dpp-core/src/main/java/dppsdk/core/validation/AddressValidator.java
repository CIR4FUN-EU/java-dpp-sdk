package dppsdk.core.validation;

import dppsdk.core.model.Address;

/**
 * Validator for Address value object.
 * Business rules:
 * - country must be meaningfully present
 * - town must be meaningfully present
 * - optional fields (zipCode, region, street) must not be blank if present
 */
public class AddressValidator implements Validator<Address> {
    
    @Override
    public void validate(Address address) throws ValidationException {
        if (address == null) {
            return; // Address is optional
        }

        // country is semantically required
        ValidationUtils.requireNotBlank(address.getCountry(), "Address.country");

        // town is semantically required
        ValidationUtils.requireNotBlank(address.getTown(), "Address.town");

        // Optional fields must not be blank if present
        if (address.getZipCode() != null) {
            ValidationUtils.requireNotBlank(address.getZipCode(), "Address.zipCode");
        }
        if (address.getRegion() != null) {
            ValidationUtils.requireNotBlank(address.getRegion(), "Address.region");
        }
        if (address.getStreet() != null) {
            ValidationUtils.requireNotBlank(address.getStreet(), "Address.street");
        }
    }
}

