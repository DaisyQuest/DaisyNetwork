package dev.daisynetwork.proxy;

import java.util.Objects;

public record RouteConflict(
        RouteSnapshot.ServiceRoute first,
        RouteSnapshot.ServiceRoute second,
        int priority,
        int specificityScore,
        String reason) {
    public RouteConflict {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        reason = requireText(reason, "reason");
        if (specificityScore < 0) {
            throw new IllegalArgumentException("specificityScore must not be negative");
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
