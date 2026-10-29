package dppsdk.core.transport.json;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.validation.ValidationIssue;
import java.util.List;
import java.util.Objects;

/** Canonical passport reconstructed from operational JSON, with information lost by compression. */
public record CompressedExpansionResult(DigitalProductPassport passport, List<ExpansionIssue> issues) {
    public CompressedExpansionResult {
        Objects.requireNonNull(passport, "passport");
        issues = List.copyOf(issues);
    }

    /** Location and reason for semantic information unavailable in the operational form. */
    public record ExpansionIssue(String path, String reason) {
        public ExpansionIssue {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(reason, "reason");
        }

        /** Compressed JSON does not carry enough EN metadata to resolve every semantic. */
        public String rule() { return "EN18223_COMPRESSED_SEMANTICS_UNRESOLVED"; }

        /** Best-effort expansion is informative and does not make the recovered tree invalid. */
        public ValidationIssue.Severity severity() { return ValidationIssue.Severity.WARNING; }
    }
}
