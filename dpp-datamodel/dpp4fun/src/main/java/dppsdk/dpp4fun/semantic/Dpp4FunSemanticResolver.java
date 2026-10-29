package dppsdk.dpp4fun.semantic;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.semantic.SemanticDefinition;
import dppsdk.core.semantic.SemanticPath;
import dppsdk.core.semantic.SemanticResolver;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Resolves semantic metadata recorded in the single bundled Dpp4Fun profile. */
public final class Dpp4FunSemanticResolver implements SemanticResolver {
    private final Dpp4FunSemanticProfile profile;

    public Dpp4FunSemanticResolver() { this(Dpp4FunSemanticProfile.loadDefault()); }

    public Dpp4FunSemanticResolver(Dpp4FunSemanticProfile profile) {
        this.profile = java.util.Objects.requireNonNull(profile, "profile");
    }

    public Dpp4FunSemanticProfile profile() { return profile; }

    @Override
    public Optional<SemanticDefinition> resolve(List<String> contentSpecificationIds, SemanticPath path) {
        Optional<Dpp4FunElementDefinition> match = profile.definitionFor(contentSpecificationIds, path);
        if (match.isEmpty()) return Optional.empty();
        Dpp4FunElementDefinition entry = match.get();
        String itemIdPattern = entry.repeatedItemPolicy() == null
                ? null : entry.repeatedItemPolicy().elementIdPattern();
        String elementId = itemIdPattern == null ? entry.elementId()
                : String.format(Locale.ROOT, itemIdPattern, itemIndex(entry.path(), path));
        return Optional.of(new SemanticDefinition(Optional.of(entry.kind()), Optional.of(elementId),
                Optional.ofNullable(entry.dictionaryReference()), Optional.ofNullable(entry.valueDataType()),
                entry.dictionaryReferenceResolved(), entry.valueDataTypeResolved()));
    }

    private static int itemIndex(String pattern, SemanticPath path) {
        String[] expected = pattern.split("\\.");
        int itemIndex = -1;
        for (int index = 0; index < expected.length; index++) {
            if (expected[index].endsWith("[*]")) {
                itemIndex = Integer.parseInt(path.segments().get(index + 1));
                index++;
            }
        }
        if (itemIndex < 0) throw new IllegalStateException("Repeated item path has no index: " + pattern);
        return itemIndex;
    }

}
