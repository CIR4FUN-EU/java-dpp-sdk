package dppsdk.dpp4fun.transport;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.core.transport.json.CompressedExpansionResult;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.validation.ValidationException;
import dppsdk.core.validation.ValidationResult;
import dppsdk.core.validation.identifier.IdentifierValidationContext;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticResolver;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;

/** Dpp4Fun convenience facade over the canonical core expanded JSON codec. */
public final class Dpp4FunJsonCodec {
    private final DppJsonCodec codec = new DppJsonCodec();
    private final Dpp4FunValidationService validation = new Dpp4FunValidationService();

    public String toJson(Dpp4Fun value) { return toExpandedJson(value); }
    public String toExpandedJson(Dpp4Fun value) {
        return codec.toExpandedJson(value == null ? null : value.asDigitalProductPassport());
    }
    public String toCompressedJson(Dpp4Fun value) {
        return codec.toCompressedJson(value == null ? null : value.asDigitalProductPassport());
    }
    public Dpp4Fun fromJson(String json) { return fromExpandedJson(json); }
    public Dpp4Fun fromExpandedJson(String json) { return Dpp4Fun.from(codec.fromExpandedJson(json)); }

    /** Expands using only semantics selected by the Dpp4Fun SDK content-specification IDs. */
    public CompressedExpansionResult expandCompressedJson(String json) {
        return codec.expandCompressedJsonWithResolver(json, new Dpp4FunSemanticResolver());
    }

    /** Combines Dpp4Fun catalog metadata with compatible metadata from a previous canonical passport. */
    public CompressedExpansionResult expandCompressedJson(String json, DigitalProductPassport previous) {
        return codec.expandCompressedJson(json, previous, new Dpp4FunSemanticResolver());
    }

    public Dpp4Fun fromCompressedJson(String json) {
        return Dpp4Fun.from(expandCompressedJson(json).passport());
    }

    public Dpp4Fun fromCompressedJsonAndValidate(String json) {
        Dpp4Fun value = fromCompressedJson(json);
        requireValid(validation.validate(value));
        return value;
    }

    /** Parses, then applies mandatory core semantic/format and Dpp4Fun profile validation. */
    public Dpp4Fun fromJsonAndValidate(String json) {
        Dpp4Fun value = fromJson(json);
        requireValid(validation.validate(value));
        return value;
    }

    /** Parses, then applies core, profile, and caller-context identifier validation. */
    public Dpp4Fun fromJsonAndValidate(String json, IdentifierValidationContext context) {
        Dpp4Fun value = fromJson(json);
        requireValid(validation.validate(value, context));
        return value;
    }

    private static void requireValid(ValidationResult result) {
        if (!result.isValid()) throw new ValidationException("Dpp4Fun validation failed: " + result.issues());
    }
}
