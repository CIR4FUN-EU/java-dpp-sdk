package dppsdk.dpp4fun.semantic;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import java.util.List;

/** A source-backed D4.1 minimum applied to matching repeated composite items. */
public record Dpp4FunSemanticRule(
        String id,
        String pathSuffix,
        String requiredField,
        List<String> requiredAnyFields,
        List<String> excludedPaths) {

    public Dpp4FunSemanticRule {
        if (id == null || id.isBlank() || pathSuffix == null || pathSuffix.isBlank()
                || requiredAnyFields == null || excludedPaths == null
                || (requiredField == null && requiredAnyFields.isEmpty())) {
            throw new IllegalArgumentException("semantic validation rule is incomplete");
        }
        requiredAnyFields = List.copyOf(requiredAnyFields);
        excludedPaths = List.copyOf(excludedPaths);
    }
}
