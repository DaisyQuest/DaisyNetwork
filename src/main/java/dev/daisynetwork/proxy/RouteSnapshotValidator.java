package dev.daisynetwork.proxy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class RouteSnapshotValidator {
    private static final Set<String> ADMIN_PATH_PREFIXES = Set.of(
            "/admin",
            "/console",
            "/actuator",
            "/management");

    private static final Comparator<ValidationIssue> ISSUE_ORDER = Comparator
            .comparing(ValidationIssue::path)
            .thenComparing(issue -> issue.severity().name())
            .thenComparing(ValidationIssue::message);

    public RouteSnapshotValidationResult validate(RouteSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<ValidationIssue> issues = new ArrayList<>();

        validateRouteIdentities(snapshot, issues);
        validateBackendIdentities(snapshot, issues);
        validateRouteGrammarAndSafety(snapshot, issues);
        validateBackendPools(snapshot, issues);
        validateRouteConflicts(snapshot, issues);

        issues.sort(ISSUE_ORDER);
        return new RouteSnapshotValidationResult(snapshot, issues);
    }

    public RouteSnapshot validateOrThrow(RouteSnapshot snapshot) {
        RouteSnapshotValidationResult result = validate(snapshot);
        return result.activatableSnapshot()
                .orElseThrow(() -> new IllegalStateException("route snapshot is not activatable: " + result.issues()));
    }

    private static void validateRouteIdentities(RouteSnapshot snapshot, List<ValidationIssue> issues) {
        Map<String, Integer> seen = new HashMap<>();
        for (RouteSnapshot.ServiceRoute route : snapshot.routes()) {
            seen.merge(route.routeId().value(), 1, Integer::sum);
        }
        seen.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> issues.add(error(
                        "routes." + entry.getKey(),
                        "Route snapshot must not contain duplicate route identity " + entry.getKey() + ".")));
    }

    private static void validateBackendIdentities(RouteSnapshot snapshot, List<ValidationIssue> issues) {
        Map<String, Integer> seen = new HashMap<>();
        for (BackendEndpoint backend : snapshot.backends()) {
            seen.merge(backend.backendId().value(), 1, Integer::sum);
        }
        seen.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> issues.add(error(
                        "backends." + entry.getKey(),
                        "Route snapshot must not contain duplicate backend identity " + entry.getKey() + ".")));
    }

    private static void validateRouteGrammarAndSafety(RouteSnapshot snapshot, List<ValidationIssue> issues) {
        for (RouteSnapshot.ServiceRoute route : snapshot.routes()) {
            RouteMatchRule rule = route.matchRule();
            String routePath = "routes." + route.routeId().value();
            String normalizedHost = ProxyRequest.normalizeHost(rule.hostPattern());
            String normalizedPath = ProxyRequest.normalizePath(rule.pathPattern());

            if (normalizedHost.startsWith("**.")) {
                issues.add(error(
                        routePath + ".hostPattern",
                        "Multi-label wildcard hosts require an explicit approval contract before activation."));
            }
            if (usesUnsupportedPathGlob(rule.pathPattern())) {
                issues.add(error(
                        routePath + ".pathPattern",
                        "Path wildcard grammar must be exact, catch-all, or trailing segment prefix only."));
            }
            if (isGlobalFallback(normalizedHost, normalizedPath) && !route.action().maintenanceResponse()) {
                issues.add(error(
                        routePath + ".matchRule",
                        "Global catch-all routes must use a fixed maintenance response until fallback policy is explicit."));
            }
            if (isAdminPath(normalizedPath) && isPublicHostPattern(normalizedHost) && !route.action().maintenanceResponse()) {
                issues.add(error(
                        routePath + ".matchRule",
                        "Public routes must not expose admin or management paths without a fixed maintenance response."));
            }
        }
    }

    private static void validateBackendPools(RouteSnapshot snapshot, List<ValidationIssue> issues) {
        for (RouteSnapshot.ServiceRoute route : snapshot.routes()) {
            if (route.action().maintenanceResponse()) {
                continue;
            }
            String poolId = route.action().backendPoolId();
            List<BackendEndpoint> poolBackends = snapshot.backends().stream()
                    .filter(backend -> backendMatchesPool(backend, poolId))
                    .toList();
            if (poolBackends.isEmpty()) {
                issues.add(error(
                        "routes." + route.routeId().value() + ".action.backendPoolId",
                        "Route backend pool " + poolId + " has no matching backend endpoints."));
                continue;
            }
            boolean anyEligible = poolBackends.stream().anyMatch(RouteSnapshotValidator::eligible);
            if (!anyEligible) {
                issues.add(warning(
                        "routes." + route.routeId().value() + ".action.backendPoolId",
                        "Route backend pool " + poolId + " has no currently eligible backend endpoints."));
            }
        }
    }

    private static void validateRouteConflicts(RouteSnapshot snapshot, List<ValidationIssue> issues) {
        Map<String, List<RouteSnapshot.ServiceRoute>> bySignature = snapshot.routes().stream()
                .collect(Collectors.groupingBy(
                        RouteSnapshotValidator::matchSignature,
                        TreeMap::new,
                        Collectors.toList()));

        for (List<RouteSnapshot.ServiceRoute> routes : bySignature.values()) {
            if (routes.size() < 2) {
                continue;
            }
            routes.sort(Comparator.comparing(route -> route.routeId().value()));
            for (int firstIndex = 0; firstIndex < routes.size(); firstIndex++) {
                RouteSnapshot.ServiceRoute first = routes.get(firstIndex);
                for (int secondIndex = firstIndex + 1; secondIndex < routes.size(); secondIndex++) {
                    RouteSnapshot.ServiceRoute second = routes.get(secondIndex);
                    int firstScore = RouteSpecificity.effectiveScore(first.matchRule());
                    int secondScore = RouteSpecificity.effectiveScore(second.matchRule());
                    if (first.matchRule().priority() == second.matchRule().priority() && firstScore == secondScore) {
                        issues.add(error(
                                "routes." + first.routeId().value() + ".conflicts." + second.routeId().value(),
                                "Routes share an identical match signature, priority, and specificity; activation must fail closed."));
                    } else {
                        issues.add(warning(
                                "routes." + first.routeId().value() + ".shadows." + second.routeId().value(),
                                "Routes share an identical match signature; lower-ranked mappings are shadowed."));
                    }
                }
            }
        }
    }

    private static String matchSignature(RouteSnapshot.ServiceRoute route) {
        RouteMatchRule rule = route.matchRule();
        String headers = rule.headers().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
        String methods = rule.methods().stream().sorted().collect(Collectors.joining(","));
        return ProxyRequest.normalizeHost(rule.hostPattern())
                + "|"
                + ProxyRequest.normalizePath(rule.pathPattern())
                + "|"
                + methods
                + "|"
                + headers;
    }

    private static boolean usesUnsupportedPathGlob(String pathPattern) {
        String trimmed = pathPattern.trim();
        if ("*".equals(trimmed) || "/*".equals(trimmed)) {
            return false;
        }
        int star = trimmed.indexOf('*');
        return star >= 0 && !trimmed.endsWith("/*");
    }

    private static boolean isGlobalFallback(String normalizedHost, String normalizedPath) {
        return "*".equals(normalizedHost) && ("*".equals(normalizedPath) || "/*".equals(normalizedPath));
    }

    private static boolean isAdminPath(String normalizedPath) {
        String lowered = normalizedPath.toLowerCase(Locale.ROOT);
        for (String prefix : ADMIN_PATH_PREFIXES) {
            if (lowered.equals(prefix) || lowered.startsWith(prefix + "/") || lowered.startsWith(prefix + "/*")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPublicHostPattern(String normalizedHost) {
        return "*".equals(normalizedHost)
                || normalizedHost.startsWith("*.")
                || normalizedHost.startsWith("**.")
                || !normalizedHost.endsWith(".internal");
    }

    private static boolean backendMatchesPool(BackendEndpoint backend, String backendPoolId) {
        Identity backendId = backend.backendId();
        return backend.slot().equals(backendPoolId)
                || backendId.value().equals(backendPoolId)
                || backendId.value().startsWith(backendPoolId + ".")
                || backendId.value().startsWith(backendPoolId + "-");
    }

    private static boolean eligible(BackendEndpoint backend) {
        return !backend.draining()
                && backend.health() != BackendEndpoint.Health.UNHEALTHY
                && backend.health() != BackendEndpoint.Health.QUARANTINED;
    }

    private static ValidationIssue error(String path, String message) {
        return new ValidationIssue(ValidationIssue.Severity.ERROR, path, message);
    }

    private static ValidationIssue warning(String path, String message) {
        return new ValidationIssue(ValidationIssue.Severity.WARNING, path, message);
    }
}
