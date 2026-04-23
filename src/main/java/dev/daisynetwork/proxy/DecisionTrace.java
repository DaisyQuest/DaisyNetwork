package dev.daisynetwork.proxy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DecisionTrace(
        String routeGeneration,
        Identity routeId,
        Identity selectedBackend,
        List<Identity> rejectedBackends,
        String policyVersion,
        Map<String, String> inputSignals,
        Instant capturedAt) {
    public DecisionTrace {
        routeGeneration = requireText(routeGeneration, "routeGeneration");
        Objects.requireNonNull(routeId, "routeId");
        Objects.requireNonNull(selectedBackend, "selectedBackend");
        rejectedBackends = List.copyOf(Objects.requireNonNull(rejectedBackends, "rejectedBackends"));
        policyVersion = requireText(policyVersion, "policyVersion");
        inputSignals = Map.copyOf(Objects.requireNonNull(inputSignals, "inputSignals"));
        Objects.requireNonNull(capturedAt, "capturedAt");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
