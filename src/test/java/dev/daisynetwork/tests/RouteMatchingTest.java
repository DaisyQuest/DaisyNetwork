package dev.daisynetwork.tests;

import dev.daisynetwork.proxy.LoadBalancingStrategy;
import dev.daisynetwork.proxy.ProxyRequest;
import dev.daisynetwork.proxy.RouteAction;
import dev.daisynetwork.proxy.RouteMatchRule;
import dev.daisynetwork.proxy.RouteMatcher;
import dev.daisynetwork.proxy.RouteSelection;
import dev.daisynetwork.proxy.RouteSnapshot;
import dev.daisynetwork.proxy.TrafficPolicy;
import dev.daisynetwork.proxy.Identity;
import dev.daisynetwork.testing.Assertions;
import dev.daisynetwork.testing.SpecTest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RouteMatchingTest {
    private final RouteMatcher matcher = new RouteMatcher();

    @SpecTest
    public void exactRouteOutranksWildcardAtEqualPriority() {
        RouteSnapshot.ServiceRoute wildcard = route("wildcard", "*.example.com", "/api/*", 50);
        RouteSnapshot.ServiceRoute exact = route("exact", "api.example.com", "/api/orders", 50);

        RouteSelection selection = matcher.select(
                List.of(wildcard, exact),
                new ProxyRequest("api.example.com", "/api/orders", "GET", Map.of()));

        Assertions.assertEquals("exact", selection.selectedRoute().orElseThrow().routeId().value(),
                "exact route must outrank wildcard route");
        Assertions.assertFalse(selection.ambiguous(), "different specificity must not be ambiguous");
    }

    @SpecTest
    public void equalPriorityAndSpecificityFailsClosed() {
        RouteSnapshot.ServiceRoute first = route("first", "*.example.com", "/api/*", 50);
        RouteSnapshot.ServiceRoute second = route("second", "*.example.com", "/api/*", 50);

        RouteSelection selection = matcher.select(
                List.of(first, second),
                new ProxyRequest("orders.example.com", "/api/items", "GET", Map.of()));

        Assertions.assertTrue(selection.ambiguous(), "equal top candidates must be ambiguous");
        Assertions.assertFalse(selection.selectedRoute().isPresent(), "ambiguous route set must not select a route");
    }

    private static RouteSnapshot.ServiceRoute route(String id, String host, String path, int priority) {
        return new RouteSnapshot.ServiceRoute(
                new Identity(id),
                new RouteMatchRule(host, path, Set.of("GET"), Map.of(), priority, 0),
                new TrafficPolicy(LoadBalancingStrategy.ROUND_ROBIN, Duration.ofSeconds(1), Duration.ofSeconds(5), 0, false),
                new RouteAction("default", "", "", Map.of(), false));
    }
}
