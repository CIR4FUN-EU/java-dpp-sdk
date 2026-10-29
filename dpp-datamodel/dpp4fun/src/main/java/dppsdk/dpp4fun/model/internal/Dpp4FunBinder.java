package dppsdk.dpp4fun.model.internal;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.semantic.DataElementKind;
import dppsdk.core.semantic.SemanticPath;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;
import dppsdk.dpp4fun.semantic.Dpp4FunElementDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Constructs canonical EN nodes using only the reviewed Dpp4Fun binding catalog. */
final class Dpp4FunBinder {
    private final Dpp4FunSemanticProfile catalog;
    private final List<String> contentSpecificationIds;

    Dpp4FunBinder(Dpp4FunSemanticProfile catalog, List<String> contentSpecificationIds) {
        this.catalog = catalog;
        this.contentSpecificationIds = contentSpecificationIds;
    }

    DataElement bind(String rootId, ProfiledAspectState.Node root, boolean editingExisting) {
        return buildNode(root, List.of(rootId), editingExisting);
    }

    private DataElement buildNode(ProfiledAspectState.Node node, List<String> semanticSegments,
            boolean editingExisting) {
        Dpp4FunElementDefinition binding = catalog.definitionFor(contentSpecificationIds,
                semanticPath(semanticSegments))
                .orElseThrow(() -> new IllegalArgumentException("No catalog binding for "
                        + String.join(".", semanticSegments)));
        String elementId = resolvedElementId(binding, semanticSegments);
        if (node.value != null) {
            return new SingleValuedDataElement.Builder().elementId(elementId)
                    .dictionaryReference(binding.dictionaryReference()).value(node.value)
                    .valueDataType(binding.valueDataType()).build();
        }
        if (node.languages != null) {
            return new MultiLanguageDataElement.Builder().elementId(elementId)
                    .dictionaryReference(binding.dictionaryReference()).values(node.languages).build();
        }
        if (binding.kind() == DataElementKind.COLLECTION) {
            List<DataElement> children = new ArrayList<>();
            for (Map.Entry<String, ProfiledAspectState.Node> child : node.children.entrySet()) {
                List<String> childPath = append(semanticSegments, child.getKey());
                if (child.getValue().isEmpty()) continue;
                children.add(buildNode(child.getValue(), childPath, editingExisting));
            }
            return new DataElementCollection.Builder().elementId(elementId)
                    .dictionaryReference(binding.dictionaryReference()).dataElements(children).build();
        }
        if (binding.kind() == DataElementKind.MULTI_VALUED) {
            List<DataElement> items = new ArrayList<>();
            List<Map.Entry<String, ProfiledAspectState.Node>> orderedItems = node.children.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(java.util.Comparator.comparingInt(Integer::parseInt)))
                    .toList();
            for (int position = 0; position < orderedItems.size(); position++) {
                Map.Entry<String, ProfiledAspectState.Node> item = orderedItems.get(position);
                if (!editingExisting && !item.getKey().equals(Integer.toString(position))) {
                    throw new IllegalArgumentException("Repeated item indexes must be contiguous from zero at "
                            + String.join(".", semanticSegments));
                }
                if (item.getValue().isEmpty()) continue;
                items.add(buildNode(item.getValue(), append(semanticSegments, item.getKey()), editingExisting));
            }
            return new MultiValuedDataElement.Builder().elementId(elementId)
                    .dictionaryReference(binding.dictionaryReference()).valueDataType(binding.valueDataType())
                    .dataElements(items).build();
        }
        throw new IllegalArgumentException("Cannot construct an empty or unsupported EN element at "
                + String.join(".", semanticSegments));
    }

    private static List<String> append(List<String> path, String value) {
        List<String> result = new ArrayList<>(path);
        result.add(value);
        return result;
    }

    private static String resolvedElementId(Dpp4FunElementDefinition binding, List<String> path) {
        if (binding.repeatedItemPolicy() == null || binding.repeatedItemPolicy().elementIdPattern() == null) {
            return binding.elementId();
        }
        int index = Integer.parseInt(path.get(path.size() - 1));
        return String.format(java.util.Locale.ROOT, binding.repeatedItemPolicy().elementIdPattern(), index);
    }

    private static SemanticPath semanticPath(List<String> segments) {
        return SemanticPath.of(segments.get(0), segments.subList(1, segments.size()).toArray(String[]::new));
    }
}
