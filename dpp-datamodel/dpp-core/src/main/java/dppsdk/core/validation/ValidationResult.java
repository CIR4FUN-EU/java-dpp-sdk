package dppsdk.core.validation;

import java.util.List;

/** Immutable result of one complete validation pass. */
public final class ValidationResult {
    public enum Status {
        VALID,
        INVALID,
        UNSUPPORTED
    }

    private final List<ValidationIssue> issues;
    private final Status status;

    ValidationResult(List<ValidationIssue> issues) {
        this.issues = List.copyOf(issues);
        this.status = determineStatus(this.issues);
    }

    /** Create a result for a validator outside this package using shared issue contracts. */
    public static ValidationResult of(List<ValidationIssue> issues) {
        return new ValidationResult(issues);
    }

    public Status status() {
        return status;
    }

    public boolean isValid() {
        return status == Status.VALID;
    }

    /**
     * Findings in deterministic header-first, depth-first model traversal order.
     */
    public List<ValidationIssue> issues() {
        return issues;
    }

    private static Status determineStatus(List<ValidationIssue> issues) {
        List<ValidationIssue> errors = issues.stream()
                .filter(issue -> issue.severity() == ValidationIssue.Severity.ERROR).toList();
        if (errors.isEmpty()) {
            return Status.VALID;
        }
        if (errors.stream().allMatch(issue -> "IDENTIFIER_METHOD_UNSUPPORTED".equals(issue.rule())
                || "IDENTIFICATION_LINK_DI_UNSUPPORTED".equals(issue.rule())
                || "GS1_DIGITAL_LINK_KEY_UNSUPPORTED".equals(issue.rule())
                || "GS1_DIGITAL_LINK_QUERY_UNSUPPORTED".equals(issue.rule()))) {
            return Status.UNSUPPORTED;
        }
        return Status.INVALID;
    }
}
