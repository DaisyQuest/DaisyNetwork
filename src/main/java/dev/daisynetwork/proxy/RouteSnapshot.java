package dev.daisynetwork.proxy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RouteSnapshot(
        String generation,
        Identity clusterId,
        Instant createdAt,
        List<ServiceRoute> routes,
        List<BackendEndpoint> backends) {
    public RouteSnapshot {
        generation = requireText(generation, "generation");
        Objects.requireNonNull(clusterId, "clusterId");
        Objects.requireNonNull(createdAt, "createdAt");
        routes = List.copyOf(Objects.requireNonNull(routes, "routes"));
        backends = List.copyOf(Objects.requireNonNull(backends, "backends"));
    }

    public record ServiceRoute(
            Identity routeId,
            RouteMatchRule matchRule,
            TrafficPolicy trafficPolicy,
            RouteAction action) {
        public ServiceRoute {
            Objects.requireNonNull(routeId, "routeId");
            Objects.requireNonNull(matchRule, "matchRule");
            Objects.requireNonNull(trafficPolicy, "trafficPolicy");
            Objects.requireNonNull(action, "action");
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
