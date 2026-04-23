package dev.daisynetwork.tests;

import dev.daisynetwork.proxy.BackendEndpoint;
import dev.daisynetwork.proxy.Identity;
import dev.daisynetwork.proxy.LoadBalancingStrategy;
import dev.daisynetwork.proxy.RouteAction;
import dev.daisynetwork.proxy.RouteMatchRule;
import dev.daisynetwork.proxy.RouteSnapshot;
import dev.daisynetwork.proxy.RouteSnapshotValidationResult;
import dev.daisynetwork.proxy.RouteSnapshotValidator;
import dev.daisynetwork.proxy.TrafficPolicy;
import dev.daisynetwork.proxy.ValidationIssue;
import dev.daisynetwork.testing.Assertions;
import dev.daisynetwork.testing.SpecTest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RouteSnapshotValidatorTest {
    private final RouteSnapshotValidator validator = new RouteSnapshotValidator();

    @SpecTest
    public void validSnapshotIsActivatable() {
        RouteSnapshot snapshot = snapshot(
                List.of(route("orders", "orders.example.com", "/api/*", "orders", false)),
                List.of(backend("orders-a", "orders", BackendEndpoint.Health.HEALTHY)));

        RouteSnapshotValidationResult result = validator.validate(snapshot);

        Assertions.assertTrue(result.valid(), "valid snapshot must pass validation");
        Assertions.assertTrue(result.activatableSnapshot().isPresent(), "valid snapshot must be activatable");
        Assertions.assertEquals(0, result.issues().size(), "valid snapshot must not report issues");
    }

    @SpecTest
    public void publicAdminRouteAndMissingBackendAreErrors() {
        RouteSnapshot snapshot = snapshot(
                List.of(route("admin", "*.example.com", "/console/*", "admin", false)),
                List.of());

        RouteSnapshotValidationResult result = validator.validate(snapshot);

        Assertions.assertFalse(result.valid(), "unsafe admin route without backend must fail validation");
        Assertions.assertEquals(ValidationIssue.Severity.ERROR, result.issues().get(0).severity(),
                "validation issue must be an error");
        Assertions.assertContains(result.issues().toString(), "admin or management paths",
                "admin boundary issue must be visible");
    }

    private static RouteSnapshot snapshot(List<RouteSnapshot.ServiceRoute> routes, List<BackendEndpoint> backends) {
        return new RouteSnapshot("generation-1", new Identity("cluster-a"), Instant.EPOCH, routes, backends);
    }

    private static RouteSnapshot.ServiceRoute route(
            String id,
            String host,
            String path,
            String pool,
            boolean maintenance) {
        return new RouteSnapshot.ServiceRoute(
                new Identity(id),
                new RouteMatchRule(host, path, Set.of("GET"), Map.of(), 10, 0),
                new TrafficPolicy(LoadBalancingStrategy.ROUND_ROBIN, Duration.ofSeconds(1), Duration.ofSeconds(5), 0, false),
                new RouteAction(pool, "", "", Map.of(), maintenance));
    }

    private static BackendEndpoint backend(String id, String slot, BackendEndpoint.Health health) {
        return new BackendEndpoint(new Identity(id), URI.create("http://" + id + ".internal:8080"), slot, health, 100, false);
    }
}
