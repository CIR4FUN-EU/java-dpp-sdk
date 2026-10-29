package dppsdk.core.transport.json;

import dppsdk.core.model.DataElement;
import java.util.List;

/** Best-effort reconstruction of one operational DataElement and its semantic uncertainties. */
public record CompressedElementExpansionResult(
        DataElement element, List<CompressedExpansionResult.ExpansionIssue> issues) {
    public CompressedElementExpansionResult {
        issues = List.copyOf(issues);
    }
}
