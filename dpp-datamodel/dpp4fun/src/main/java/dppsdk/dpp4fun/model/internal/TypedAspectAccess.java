package dppsdk.dpp4fun.model.internal;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiLanguageValue;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.SingleValuedDataElement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import dppsdk.core.semantic.SemanticPath;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;

/** Shared package-private value access for typed views over catalog-bound aspect subtrees. */
public final class TypedAspectAccess {
    private final ProfiledAspectState aspect;

    public TypedAspectAccess(ProfiledAspectState aspect) {
    this.aspect = Objects.requireNonNull(aspect, "aspect");
    }

    public String string(String field) { return typedValue(field, String.class); }

    public String optionalString(String field) { return typedValueOrNull(field, String.class); }

    public Boolean bool(String field) { return typedValueOrNull(field, Boolean.class); }

    public Double decimal(String field) {
    Object value = optionalValue(field);
    if (value == null) return null;
    if (!(value instanceof Number number)) throw typeError(field, "number");
    return number.doubleValue();
    }

    public Integer integer(String field) {
    Object value = optionalValue(field);
    if (value == null) return null;
    if (!(value instanceof Number number)) throw typeError(field, "integer");
        try {
        if (number instanceof BigInteger integer) return integer.intValueExact();
        if (number instanceof BigDecimal decimal) return decimal.intValueExact();
        return new BigDecimal(number.toString()).intValueExact();
    } catch (ArithmeticException | NumberFormatException exception) {
        throw new IllegalArgumentException("Dpp4Fun field '" + field + "' must contain an integer", exception);
        }
    }

    public URI uri(String field) {
    String value = optionalString(field);
    return value == null ? null : URI.create(value);
    }

    public Map<String, String> languages(String field) {
    DataElement child = requiredChild(field);
    if (!(child instanceof MultiLanguageDataElement values)) throw typeError(field, "multi-language value");
    Map<String, String> result = new LinkedHashMap<>();
    for (MultiLanguageValue value : values.getValues()) result.put(value.getLanguage(), value.getValue());
    return Collections.unmodifiableMap(result);
    }

    public Map<String, String> optionalLanguages(String field) {
    if (childOrNull(field) == null) return Map.of();
    return languages(field);
    }

    public DataElement dataElement(String field) { return childOrNull(field); }

    public DataElementCollection collection(String field) {
    DataElement child = childOrNull(field);
    if (child == null) return null;
    if (!(child instanceof DataElementCollection value)) throw typeError(field, "collection");
        return value;
    }

    public List<DataElement> values(String field) {
    DataElement child = childOrNull(field);
    if (child == null) return List.of();
    if (!(child instanceof MultiValuedDataElement value)) throw typeError(field, "list");
    return value.getDataElements();
    }

    /** Returns a built-in typed quantity when both the value and a recognized catalog unit are present. */
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> quantity(String field) {
    Object value = optionalValue(field);
    if (value == null) return Optional.empty();
    if (!(value instanceof Number number)) throw typeError(field, "number");
    Dpp4FunSemanticProfile catalog = Dpp4FunSemanticProfile.loadDefault();
    var binding = catalog.definitionFor(catalog.contentSpecificationIds(aspect.variant()),
            SemanticPath.of(aspect.elementId(), field)).orElseThrow(() -> typeError(field, "catalog quantity"));
    if (binding.unitDimension() == null || binding.unitSlotPath() == null) {
        throw new IllegalArgumentException("Dpp4Fun field '" + field + "' has no catalog-bound unit slot");
        }
    Object symbolValue = optionalValue(binding.unitSlotPath());
    if (symbolValue == null) return Optional.empty();
    if (!(symbolValue instanceof String symbol)) throw typeError(binding.unitSlotPath(), "unit symbol");
    UnitDimension dimension = UnitDimension.valueOf(binding.unitDimension().toUpperCase(java.util.Locale.ROOT));
    SemanticUnit unit = SemanticUnit.known(symbol, dimension).orElse(null);
    if (unit == null) return Optional.empty();
    return Optional.of(new Dpp4FunQuantity<>(number.doubleValue(), unit));
    }

    public TypedAspectAccess withString(String field, String value) {
    return setScalar(field, value);
    }

    public TypedAspectAccess withBoolean(String field, Boolean value) {
    return setScalar(field, value);
    }

    public TypedAspectAccess withDecimal(String field, Double value) {
    return setScalar(field, value);
    }

    public TypedAspectAccess withInteger(String field, Integer value) {
    return setScalar(field, value);
    }

    public TypedAspectAccess withUri(String field, URI value) {
    return setScalar(field, value == null ? null : value.toString());
    }

    public TypedAspectAccess without(String field) { return removeDirect(field); }

    public TypedAspectAccess withLanguages(String field, Map<String, String> values) {
    Objects.requireNonNull(values, "values");
    if (values.isEmpty()) return removeDirect(field);
    ProfiledAspectState.Builder builder = aspect.toBuilder();
    builder.languages(field, values);
    return new TypedAspectAccess(builder.build());
    }

    public TypedAspectAccess withQuantity(String field, Dpp4FunQuantity<? extends SemanticUnit> quantity) {
    Objects.requireNonNull(quantity, "quantity");
    ProfiledAspectState.Builder builder = aspect.toBuilder();
    builder.quantity(field, quantity);
    return new TypedAspectAccess(builder.build());
    }

    public ProfiledAspectState asAspect() { return aspect; }

    private TypedAspectAccess setScalar(String field, Object value) {
    if (value == null) return removeDirect(field);
    ProfiledAspectState.Builder builder = aspect.toBuilder();
    builder.field(field, value);
    return new TypedAspectAccess(builder.build());
    }

    private TypedAspectAccess removeDirect(String field) {
    DataElementCollection root = aspect.asDataElement();
    List<DataElement> retained = new ArrayList<>(root.getDataElements());
    retained.removeIf(child -> child.getElementId().equals(field));
    DataElementCollection replacement = root.toBuilder().dataElements(retained).build();
    return new TypedAspectAccess(new ProfiledAspectState(root.getElementId(), replacement, aspect.variant()));
    }

    private Object optionalValue(String field) {
    DataElement child = childOrNull(field);
    if (child == null) return null;
    if (!(child instanceof SingleValuedDataElement scalar)) throw typeError(field, "single value");
    return scalar.getValue();
    }

    private Object requiredValue(String field) {
    Object value = optionalValue(field);
    if (value == null) throw new IllegalArgumentException("Dpp4Fun field '" + field + "' is absent");
        return value;
    }

    private DataElement requiredChild(String field) {
    DataElement child = childOrNull(field);
    if (child == null) throw new IllegalArgumentException("Dpp4Fun field '" + field + "' is absent");
        return child;
    }

    private DataElement childOrNull(String field) {
    return aspect.asDataElement().getDataElements().stream()
            .filter(child -> child.getElementId().equals(field)).findFirst().orElse(null);
    }

    private <T> T typedValue(String field, Class<T> type) {
    Object value = requiredValue(field);
    if (!type.isInstance(value)) throw typeError(field, type.getSimpleName());
    return type.cast(value);
    }

    private <T> T typedValueOrNull(String field, Class<T> type) {
    Object value = optionalValue(field);
    if (value == null) return null;
    if (!type.isInstance(value)) throw typeError(field, type.getSimpleName());
    return type.cast(value);
    }

    private static IllegalArgumentException typeError(String field, String expected) {
    return new IllegalArgumentException("Dpp4Fun field '" + field + "' must contain a " + expected);
    }
}
