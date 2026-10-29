package dppsdk.core.semantic;

import java.util.List;
import java.util.Optional;

/** Optional source of fixed DataElement semantics omitted from compressed JSON. */
@FunctionalInterface
public interface SemanticResolver {
    Optional<SemanticDefinition> resolve(List<String> contentSpecificationIds, SemanticPath path);
}
