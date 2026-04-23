package dev.daisynetwork.proxy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProxyReadModel(
        String routeGeneration,
        Instant capturedAt,
        List<RouteSnapshot.ServiceRoute> routes,
        List<BackendEndpoint> backends,
        boolean partial,
        String freshnessState) {
    public ProxyReadModel {
        routeGeneration = requireText(routeGeneration, "routeGeneration");
        Objects.requireNonNull(capturedAt, "capturedAt");
        routes = List.copyOf(Objects.requireNonNull(routes, "routes"));
        backends = List.copyOf(Objects.requireNonNull(backends, "backends"));
        freshnessState = requireText(freshnessState, "freshnessState");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
