package dppsdk.core.validation;

import dppsdk.core.model.Address;
import dppsdk.core.model.Contact;
import dppsdk.core.model.Documentation;
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Email;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.PassportMetadata;
import dppsdk.core.model.Telephone;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime validation facade that dispatches supported core objects to their validators.
 */
public final class ValidationService {

    private final Map<Class<?>, Validator<?>> validators = new ConcurrentHashMap<>();

    public ValidationService() {
        registerDefaults();
    }

    public <T> ValidationService register(Class<T> type, Validator<? super T> validator) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(validator, "validator must not be null");
        validators.put(type, validator);
        return this;
    }

    public void validate(Object value) throws ValidationException {
        if (value == null) {
            throw new ValidationException("Cannot validate null");
        }

        Validator<Object> validator = resolveValidator(value.getClass());
        if (validator == null) {
            throw new ValidationException("No validator registered for type: " + value.getClass().getName());
        }

        validator.validate(value);
    }

    public boolean supports(Class<?> type) {
        return type != null && resolveValidator(type) != null;
    }

    public boolean supportsExact(Class<?> type) {
        return type != null && validators.containsKey(type);
    }

    public int size() {
        return validators.size();
    }

    private void registerDefaults() {
        register(Email.class, new EmailValidator());
        register(Telephone.class, new TelephoneValidator());
        register(Address.class, new AddressValidator());
        register(Contact.class, new ContactValidator());

        register(DppCore.class, new DppCoreValidator());
        register(PassportMetadata.class, new PassportMetadataValidator());
        register(Nameplate.class, new NameplateValidator());
        register(Organization.class, new OrganizationValidator());
        register(Documentation.class, new DocumentationValidator());
    }

    @SuppressWarnings("unchecked")
    private Validator<Object> resolveValidator(Class<?> runtimeType) {
        Validator<?> exact = validators.get(runtimeType);
        if (exact != null) {
            return (Validator<Object>) exact;
        }

        Class<?> bestMatch = null;
        Validator<?> bestValidator = null;
        for (Map.Entry<Class<?>, Validator<?>> entry : validators.entrySet()) {
            Class<?> registeredType = entry.getKey();
            if (registeredType.isAssignableFrom(runtimeType)) {
                if (bestMatch == null || bestMatch.isAssignableFrom(registeredType)) {
                    bestMatch = registeredType;
                    bestValidator = entry.getValue();
                }
            }
        }

        return (Validator<Object>) bestValidator;
    }
}
