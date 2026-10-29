package dppsdk.dpp4fun.model.internal;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Package-private operations for the one explicit Dpp4Fun element profile. */
public final class ProfileElementSupport {
    public static final String ITEM_ID = "item-0";

    private ProfileElementSupport() { }

    public static DataElementCollection empty(String elementId) {
        return new DataElementCollection.Builder().elementId(elementId).build();
    }

    public static DataElementCollection collection(DataElement element, String path) {
        if (!(element instanceof DataElementCollection collection)) {
            throw new IllegalArgumentException(path + " must be a DataElementCollection");
        }
        return collection;
    }

    public static void requireUniqueKnownIds(DataElementCollection collection, String... ids) {
        for (String id : ids) find(collection, id);
    }

    public static DataElement find(DataElementCollection collection, String id) {
        DataElement found = null;
        for (DataElement child : collection.getDataElements()) {
            if (id.equals(child.getElementId())) {
                if (found != null) throw new IllegalArgumentException("Duplicate Dpp4Fun element '" + id + "'");
                found = child;
            }
        }
        return found;
    }

    public static Object value(DataElementCollection collection, String id) {
        DataElement child = find(collection, id);
        if (child == null) return null;
        if (!(child instanceof SingleValuedDataElement single)) {
            throw new IllegalArgumentException("Dpp4Fun element '" + id + "' must be single-valued");
        }
        return single.getValue();
    }

    public static String string(DataElementCollection collection, String id) {
        Object value = value(collection, id);
        if (value == null) return null;
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("Dpp4Fun element '" + id + "' must contain text");
        }
        return text;
    }

    public static Double decimal(DataElementCollection collection, String id) {
        Object value = value(collection, id);
        if (value == null) return null;
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Dpp4Fun element '" + id + "' must contain a number");
        }
        return number.doubleValue();
    }

    public static Integer integer(DataElementCollection collection, String id) {
        Object value = value(collection, id);
        if (value == null) return null;
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Dpp4Fun element '" + id + "' must contain an integer");
        }
        try {
            if (number instanceof BigDecimal decimal) return decimal.intValueExact();
            if (number instanceof BigInteger integer) return integer.intValueExact();
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("Dpp4Fun element '" + id + "' must contain an integer", exception);
        }
    }

    public static boolean bool(DataElementCollection collection, String id) {
        Object value = value(collection, id);
        if (value == null) return false;
        if (!(value instanceof Boolean bool)) {
            throw new IllegalArgumentException("Dpp4Fun element '" + id + "' must contain a boolean");
        }
        return bool;
    }

    public static DataElementCollection childCollection(DataElementCollection collection, String id) {
        DataElement child = find(collection, id);
        return child == null ? null : collection(child, id);
    }

    public static List<DataElement> multiValues(DataElementCollection collection, String id) {
        DataElement child = find(collection, id);
        if (child == null) return List.of();
        if (!(child instanceof MultiValuedDataElement multi)) {
            throw new IllegalArgumentException("Dpp4Fun element '" + id + "' must be multi-valued");
        }
        return multi.getDataElements();
    }

    public static List<String> strings(DataElementCollection collection, String id) {
        List<String> values = new ArrayList<>();
        List<DataElement> items = multiValues(collection, id);
        requireUniqueListItemIds(items, id);
        for (DataElement item : items) {
            // Only item-N children belong to this profile's list representation. Other IDs
            // are extensions owned by the caller and must remain invisible to the typed list.
            if (!isListItemId(item.getElementId())) continue;
            if (!(item instanceof SingleValuedDataElement single)) {
                throw new IllegalArgumentException("Dpp4Fun list '" + id + "' must contain text values");
            }
            Object value = single.getValue();
            if (value != null && !(value instanceof String)) {
                throw new IllegalArgumentException("Dpp4Fun list '" + id + "' must contain text values");
            }
            values.add((String) value);
        }
        // The returned collection is detached from core state. Preserve the legacy convenience of
        // callers being able to mutate their local result without mutating this typed view.
        return values;
    }

    public static SingleValuedDataElement scalar(String id, Object value) {
        return new SingleValuedDataElement.Builder().elementId(id).value(value).build();
    }

    public static MultiValuedDataElement multi(String id, List<? extends DataElement> values) {
        return new MultiValuedDataElement.Builder().elementId(id).dataElements(new ArrayList<>(values)).build();
    }

    public static List<DataElement> stringElements(List<String> values) {
        List<DataElement> elements = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++)
            elements.add(scalar(listItemId(index), values.get(index)));
        return elements;
    }

    public static String listItemId(int index) { return "item-" + index; }
    public static void requireUniqueListItemIds(List<DataElement> values, String listId) {
        Set<String> seen = new HashSet<>();
        for (DataElement value : values) {
            String id = value.getElementId();
            if (isListItemId(id) && !seen.add(id)) {
                throw new IllegalArgumentException("Duplicate Dpp4Fun list item '" + id + "' in '" + listId + "'");
            }
        }
    }
    public static boolean isListItemId(String id) {
        if (id == null || !id.startsWith("item-")) return false;
        try { return Integer.parseInt(id.substring(5)) >= 0; }
        catch (NumberFormatException ignored) { return false; }
    }

    public static DataElementCollection withId(DataElementCollection collection, String id) {
        return collection.toBuilder().elementId(id).build();
    }

    public static DataElementCollection patchCollection(DataElementCollection parent, String id,
                                                DataElementCollection replacement, List<String> knownOrder) {
        if (replacement == null) return patch(parent, id, null, knownOrder);
        DataElement existing = find(parent, id);
        DataElementCollection retained = replacement;
        if (existing instanceof DataElementCollection old) retained = mergeProfile(old, replacement, id);
        String dictionaryReference = existing == null ? retained.getDictionaryReference() : existing.getDictionaryReference();
        retained = retained.toBuilder().elementId(id).dictionaryReference(dictionaryReference).build();
        return patch(parent, id, retained, knownOrder);
    }

    private static DataElementCollection mergeProfile(DataElementCollection original,
                                                       DataElementCollection replacement, String profileId) {
        List<String> order = nestedOrder(profileId);
        if (order == null) return replacement;
        DataElementCollection result = original;
        for (String id : order) {
            DataElement next = find(replacement, id);
            DataElement previous = find(original, id);
            if (next == null) {
                if (previous instanceof DataElementCollection && nestedOrder(id) != null)
                    result = removeNestedKnown(result, id, nestedOrder(id), order);
                else if (previous instanceof MultiValuedDataElement)
                    result = mergeMultiValue(result, id, null, order);
                else result = patch(result, id, null, order);
            } else if (next instanceof DataElementCollection nextCollection
                    && previous instanceof DataElementCollection previousCollection
                    && nestedOrder(id) != null) {
                result = patchCollection(result, id, mergeProfile(previousCollection, nextCollection, id), order);
            } else if (next instanceof MultiValuedDataElement nextMulti
                    && previous instanceof MultiValuedDataElement) {
                result = mergeMultiValue(result, id, nextMulti, order);
            } else if (next instanceof SingleValuedDataElement nextSingle
                    && previous instanceof SingleValuedDataElement) {
                result = patchScalar(result, id, nextSingle.getValue(), order);
            } else {
                result = patch(result, id, next, order);
            }
        }
        // Carry unknown additions supplied by the replacement while retaining original extension order.
        for (DataElement child : replacement.getDataElements()) {
            if (!order.contains(child.getElementId()) && !original.getDataElements().contains(child)) {
                result = patch(result, child.getElementId(), child, order);
            }
        }
        return result.toBuilder().dictionaryReference(original.getDictionaryReference()).build();
    }

    private static DataElementCollection mergeMultiValue(DataElementCollection parent, String id,
                                                          MultiValuedDataElement replacement,
                                                          List<String> parentOrder) {
        DataElement existingElement = find(parent, id);
        if (!(existingElement instanceof MultiValuedDataElement existing)) {
            return replacement == null ? parent : patch(parent, id, replacement, parentOrder);
        }

        List<DataElement> oldChildren = existing.getDataElements();
        List<DataElement> newChildren = replacement == null ? List.of() : replacement.getDataElements();
        List<String> entryFields = entryOrder(id);
        boolean structuredEntries = entryFields != null;
        List<DataElement> merged = new ArrayList<>();
        List<String> consumedIds = new ArrayList<>();

        // Walk the existing list so extensions keep their exact relative order and known
        // replacements stay in their original slots.
        for (DataElement oldChild : oldChildren) {
            if (!isListItemId(oldChild.getElementId())) {
                merged.add(oldChild);
                continue;
            }
            DataElement newChild = findListItem(newChildren, oldChild.getElementId());
            if (newChild == null) continue;
            DataElement preserved = newChild;
            if (!structuredEntries && oldChild instanceof SingleValuedDataElement oldSingle
                    && newChild instanceof SingleValuedDataElement newSingle) {
                preserved = oldSingle.toBuilder().elementId(oldChild.getElementId())
                        .value(newSingle.getValue()).build();
            } else if (structuredEntries && oldChild instanceof DataElementCollection oldEntry
                    && newChild instanceof DataElementCollection newEntry) {
                preserved = mergeKnownCollection(oldEntry, newEntry, entryFields);
            }
            merged.add(withListItemId(preserved, oldChild.getElementId()));
            consumedIds.add(newChild.getElementId());
        }

        // New known list entries are inserted after existing known entries and before trailing
        // extensions, preserving all extension ordering from the wrapped core tree.
        int insertion = 0;
        for (int index = 0; index < merged.size(); index++) {
            if (isListItemId(merged.get(index).getElementId())) insertion = index + 1;
        }
        for (DataElement newChild : newChildren) {
            if (!isListItemId(newChild.getElementId())) {
                if (!merged.contains(newChild)) merged.add(newChild);
                continue;
            }
            if (consumedIds.contains(newChild.getElementId())) continue;
            String itemId = newChild.getElementId();
            DataElement inserted = newChild;
            if (structuredEntries && inserted instanceof DataElementCollection newEntry) {
                DataElement oldMatch = findListItem(oldChildren, itemId);
                if (oldMatch instanceof DataElementCollection oldEntry)
                    inserted = mergeKnownCollection(oldEntry, newEntry, entryFields);
            }
            merged.add(insertion++, withListItemId(inserted, itemId));
        }

        if (merged.isEmpty()) return patch(parent, id, null, parentOrder);
        MultiValuedDataElement updated = existing.toBuilder().elementId(id).dataElements(merged).build();
        return patch(parent, id, updated, parentOrder);
    }

    private static DataElement findListItem(List<DataElement> values, String id) {
        for (DataElement value : values) {
            if (id.equals(value.getElementId()) && isListItemId(id)) return value;
        }
        return null;
    }

    private static int findAvailableListValue(List<DataElement> values, Set<Integer> used, String id) {
        for (int index = 0; index < values.size(); index++) {
            if (!used.contains(index) && id.equals(values.get(index).getElementId())) return index;
        }
        return -1;
    }

    private static int findAvailableListValueByPosition(List<DataElement> values, Set<Integer> used, int position) {
        return position < values.size() && !used.contains(position) ? position : -1;
    }

    private static DataElementCollection mergeKnownCollection(DataElementCollection original,
                                                                DataElementCollection replacement,
                                                                List<String> knownOrder) {
        DataElementCollection result = original;
        for (String field : knownOrder) {
            DataElement next = find(replacement, field);
            DataElement previous = find(original, field);
            if (next == null) result = patch(result, field, null, knownOrder);
            else if (next instanceof SingleValuedDataElement single
                    && previous instanceof SingleValuedDataElement) {
                result = patchScalar(result, field, single.getValue(), knownOrder);
            } else if (next instanceof DataElementCollection nextCollection
                    && previous instanceof DataElementCollection previousCollection) {
                result = patchCollection(result, field,
                        mergeKnownCollection(previousCollection, nextCollection, knownOrder), knownOrder);
            } else result = patch(result, field, next, knownOrder);
        }
        for (DataElement child : replacement.getDataElements()) {
            if (!knownOrder.contains(child.getElementId()) && find(result, child.getElementId()) == null) {
                result = patch(result, child.getElementId(), child, knownOrder);
            }
        }
        return result.toBuilder().dictionaryReference(original.getDictionaryReference()).build();
    }

    public static DataElementCollection patch(
            DataElementCollection source, String id, DataElement replacement, List<String> knownOrder) {
        List<DataElement> children = new ArrayList<>(source.getDataElements());
        int existing = -1;
        for (int index = 0; index < children.size(); index++) {
            if (id.equals(children.get(index).getElementId())) {
                if (existing >= 0) throw new IllegalArgumentException("Duplicate Dpp4Fun element '" + id + "'");
                existing = index;
            }
        }
        if (existing >= 0) {
            if (replacement == null) children.remove(existing);
            else children.set(existing, replacement);
        } else if (replacement != null) {
            int order = knownOrder.indexOf(id);
            int insertion = -1;
            if (order >= 0) {
                for (int index = 0; index < children.size(); index++) {
                    int childOrder = knownOrder.indexOf(children.get(index).getElementId());
                    if (childOrder > order) { insertion = index; break; }
                }
                if (insertion < 0) {
                    for (int index = children.size() - 1; index >= 0; index--) {
                        if (knownOrder.contains(children.get(index).getElementId())) {
                            insertion = index + 1;
                            break;
                        }
                    }
                }
            }
            if (insertion < 0) insertion = children.size();
            children.add(insertion, replacement);
        }
        return source.toBuilder().dataElements(children).build();
    }

    public static DataElementCollection removeNestedKnown(
            DataElementCollection parent, String id, List<String> nestedKnownOrder, List<String> parentOrder) {
        DataElement child = find(parent, id);
        if (!(child instanceof DataElementCollection nested)) return patch(parent, id, null, parentOrder);
        List<DataElement> extensions = extensionOnly(nested, nestedKnownOrder);
        if (extensions.isEmpty()) return patch(parent, id, null, parentOrder);
        DataElementCollection retained = nested.toBuilder().dataElements(extensions).build();
        return patch(parent, id, retained, parentOrder);
    }

    private static List<DataElement> extensionOnly(DataElementCollection source, List<String> knownOrder) {
        List<DataElement> retained = new ArrayList<>();
        for (DataElement child : source.getDataElements()) {
            if (!knownOrder.contains(child.getElementId())) {
                retained.add(child);
                continue;
            }
            if (child instanceof DataElementCollection collection) {
                List<String> nestedOrder = nestedOrder(child.getElementId());
                if (nestedOrder != null) {
                    List<DataElement> descendants = extensionOnly(collection, nestedOrder);
                    if (!descendants.isEmpty()) retained.add(collection.toBuilder().dataElements(descendants).build());
                }
            } else if (child instanceof MultiValuedDataElement multi) {
                List<String> entryOrder = entryOrder(child.getElementId());
                if (entryOrder == null) {
                    List<DataElement> extensions = multi.getDataElements().stream()
                            .filter(entry -> !isListItemId(entry.getElementId())).toList();
                    if (!extensions.isEmpty()) retained.add(multi.toBuilder().dataElements(extensions).build());
                    continue;
                }
                List<DataElement> entries = new ArrayList<>();
                for (DataElement entry : multi.getDataElements()) {
                    if (!isListItemId(entry.getElementId()) || !(entry instanceof DataElementCollection entryCollection)) {
                        entries.add(entry);
                        continue;
                    }
                    List<DataElement> descendants = extensionOnly(entryCollection, entryOrder);
                    if (!descendants.isEmpty()) entries.add(entryCollection.toBuilder().dataElements(descendants).build());
                }
                if (!entries.isEmpty()) retained.add(multi.toBuilder().dataElements(entries).build());
            }
        }
        return List.copyOf(retained);
    }

    private static List<String> nestedOrder(String id) {
        return switch (id) {
            case "$" -> Dpp4FunSemanticProfile.loadDefault().rootElementIds();
            case "nameplate" -> catalogFieldOrder("nameplate");
            case "contact" -> Contact.ORDER;
            case "address" -> Address.ORDER;
            case "email" -> Email.ORDER;
            case "telephone" -> Telephone.ORDER;
            case "characteristics" -> Dpp4FunSemanticProfile.loadDefault().definitions().stream()
                    .map(definition -> definition.path())
                    .filter(path -> path.startsWith("characteristics."))
                    .map(path -> path.substring("characteristics.".length()).split("\\.", 2)[0]
                            .replace("[*]", ""))
                    .distinct().toList();
            case "billOfMaterials" -> catalogFieldOrder("billOfMaterials");
            default -> null;
        };
    }

    private static List<String> catalogFieldOrder(String rootId) {
        String prefix = rootId + ".";
        return Dpp4FunSemanticProfile.loadDefault().definitions().stream()
                .map(definition -> definition.path())
                .filter(path -> path.startsWith(prefix))
                .map(path -> path.substring(prefix.length()).split("\\.", 2)[0].replaceAll("\\[.*", ""))
                .distinct().toList();
    }

    private static List<String> entryOrder(String id) {
        return switch (id) {
            case "materials" -> Material.ORDER;
            case "components" -> Component.ORDER;
            case "parts" -> Part.ORDER;
            default -> null;
        };
    }

    public static DataElementCollection patchScalar(
            DataElementCollection source, String id, Object value, List<String> knownOrder) {
        if (value == null) return patch(source, id, null, knownOrder);
        DataElement existing = find(source, id);
        SingleValuedDataElement replacement = existing instanceof SingleValuedDataElement single
                ? single.toBuilder().elementId(id).value(value).build()
                : scalar(id, value);
        return patch(source, id, replacement, knownOrder);
    }

    public static DataElementCollection patchStringList(DataElementCollection source, String id, List<String> values,
                                                  List<String> knownOrder) {
        DataElement current = find(source, id);
        List<DataElement> original = current instanceof MultiValuedDataElement multi
                ? multi.getDataElements() : List.of();
        List<DataElement> replacements = stringElements(values);
        List<DataElement> merged = new ArrayList<>();
        int next = 0;
        boolean hadKnown = false;
        for (DataElement child : original) {
            if (isListItemId(child.getElementId())) {
                hadKnown = true;
                if (next < replacements.size()) {
                    SingleValuedDataElement previous = (SingleValuedDataElement) child;
                    SingleValuedDataElement replacement = (SingleValuedDataElement) replacements.get(next++);
                    merged.add(previous.toBuilder().elementId(child.getElementId())
                            .value(replacement.getValue()).build());
                }
            } else {
                merged.add(child);
            }
        }
        if (!hadKnown) {
            int insertion = firstExtensionIndex(merged);
            merged.addAll(insertion, replacements);
        }
        else if (next < replacements.size()) {
            int lastKnown = -1;
            for (int i = 0; i < merged.size(); i++) if (isListItemId(merged.get(i).getElementId())) lastKnown = i;
            List<DataElement> additions = new ArrayList<>();
            List<DataElement> occupied = new ArrayList<>(merged);
            for (int index = next; index < replacements.size(); index++) {
                String itemId = listItemId(nextAvailableListItemIndex(occupied));
                DataElement addition = ((SingleValuedDataElement) replacements.get(index)).toBuilder().elementId(itemId).build();
                additions.add(addition);
                occupied.add(addition);
            }
            merged.addAll(lastKnown < 0 ? merged.size() : lastKnown + 1, additions);
        }
        MultiValuedDataElement replacement = merged.isEmpty() ? null
                : current instanceof MultiValuedDataElement existing
                ? existing.toBuilder().elementId(id).dataElements(merged).build()
                : multi(id, merged);
        return patch(source, id, replacement, knownOrder);
    }

    public static DataElementCollection patchEntryList(DataElementCollection source, String id, List<DataElement> values,
                                                 List<String> knownOrder) {
        DataElement current = find(source, id);
        List<DataElement> original = current instanceof MultiValuedDataElement multi
                ? multi.getDataElements() : List.of();
        List<DataElement> merged = new ArrayList<>();
        boolean hadKnown = false;
        Set<Integer> usedValues = new HashSet<>();
        Set<String> replacementIds = new HashSet<>();
        boolean uniqueReplacementIds = true;
        boolean replacementIdsOverlap = false;
        for (DataElement value : values) {
            String valueId = value.getElementId();
            if (!replacementIds.add(valueId)) uniqueReplacementIds = false;
            if (isListItemId(valueId) && findListItem(original, valueId) != null) replacementIdsOverlap = true;
        }
        boolean useStableIds = uniqueReplacementIds && replacementIdsOverlap;
        int oldKnownIndex = 0;
        for (DataElement child : original) {
            if (isListItemId(child.getElementId())) {
                hadKnown = true;
                int matched = useStableIds
                        ? findAvailableListValue(values, usedValues, child.getElementId())
                        : findAvailableListValueByPosition(values, usedValues, oldKnownIndex);
                oldKnownIndex++;
                if (matched >= 0) {
                    DataElement replacement = values.get(matched);
                    if (child instanceof DataElementCollection oldEntry
                            && replacement instanceof DataElementCollection newEntry) {
                        replacement = mergeKnownCollection(oldEntry, newEntry, entryOrder(id));
                    }
                    merged.add(withListItemId(replacement, child.getElementId()));
                    usedValues.add(matched);
                } else if (child instanceof DataElementCollection entry) {
                    List<String> entryFields = entryOrder(id);
                    if (entryFields != null) {
                        List<DataElement> extensions = extensionOnly(entry, entryFields);
                        if (!extensions.isEmpty()) merged.add(entry.toBuilder().dataElements(extensions).build());
                    }
                }
            } else merged.add(child);
        }
        if (!hadKnown) {
            int insertion = firstExtensionIndex(merged);
            for (int index = 0; index < values.size(); index++)
                merged.add(insertion++, withListItemId(values.get(index), listItemId(nextAvailableListItemIndex(merged))));
        }
        else {
            int lastKnown = -1;
            for (int i = 0; i < merged.size(); i++) if (isListItemId(merged.get(i).getElementId())) lastKnown = i;
            int insertion = lastKnown < 0 ? firstExtensionIndex(merged) : lastKnown + 1;
            for (int index = 0; index < values.size(); index++) {
                if (usedValues.contains(index)) continue;
                String itemId = listItemId(nextAvailableListItemIndex(merged));
                DataElement addition = withListItemId(values.get(index), itemId);
                merged.add(insertion++, addition);
            }
        }
        if (merged.isEmpty()) return patch(source, id, null, knownOrder);
        // Known entries use their canonical prefix position; extensions keep their relative order.
        MultiValuedDataElement replacement = current instanceof MultiValuedDataElement existing
                ? existing.toBuilder().elementId(id).dataElements(merged).build()
                : multi(id, merged);
        return patch(source, id, replacement, knownOrder);
    }

    private static DataElement withListItemId(DataElement value, String id) {
        if (value instanceof DataElementCollection collection) return collection.toBuilder().elementId(id).build();
        if (value instanceof SingleValuedDataElement single) return single.toBuilder().elementId(id).build();
        throw new IllegalArgumentException("Unsupported Dpp4Fun list item type: " + value.getClass().getName());
    }

    private static int nextAvailableListItemIndex(List<DataElement> elements) {
        int candidate = 0;
        boolean used;
        do {
            used = false;
            String candidateId = listItemId(candidate);
            for (DataElement element : elements) {
                if (candidateId.equals(element.getElementId())) { used = true; candidate++; break; }
            }
        } while (used);
        return candidate;
    }

    private static int firstExtensionIndex(List<DataElement> elements) {
        for (int index = 0; index < elements.size(); index++) {
            if (!isListItemId(elements.get(index).getElementId())) return index;
        }
        return elements.size();
    }

    public static void requireNoNulls(List<?> values, String name) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index) == null) throw new IllegalArgumentException(name + "[" + index + "] must not be null");
        }
    }
}
