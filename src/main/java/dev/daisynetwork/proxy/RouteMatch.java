package dev.daisynetwork.proxy;

import java.util.Objects;

public record RouteMatch(
        RouteSnapshot.ServiceRoute route,
        int priority,
        int specificityScore) {
    public RouteMatch {
        Objects.requireNonNull(route, "route");
        if (specificityScore < 0) {
            throw new IllegalArgumentException("specificityScore must not be negative");
        }
    }
}
