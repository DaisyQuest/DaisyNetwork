package dev.daisynetwork.proxy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RouteSnapshotValidationResult(
        RouteSnapshot snapshot,
        List<ValidationIssue> issues) {
    public RouteSnapshotValidationResult {
        Objects.requireNonNull(snapshot, "snapshot");
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public boolean valid() {
        return issues.stream().noneMatch(issue -> issue.severity() == ValidationIssue.Severity.ERROR);
    }

    public Optional<RouteSnapshot> activatableSnapshot() {
        return valid() ? Optional.of(snapshot) : Optional.empty();
    }
}
