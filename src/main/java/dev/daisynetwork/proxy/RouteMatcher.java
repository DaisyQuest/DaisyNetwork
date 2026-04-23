package dev.daisynetwork.proxy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RouteMatcher {
    private static final String AMBIGUOUS_EQUAL_PRIORITY_AND_SPECIFICITY =
            "matching routes share equal priority and specificity";

    private static final Comparator<RouteMatch> MATCH_ORDER = Comparator
            .comparingInt(RouteMatch::priority).reversed()
            .thenComparing(Comparator.comparingInt(RouteMatch::specificityScore).reversed())
            .thenComparing(match -> match.route().routeId().value());

    public boolean matches(RouteMatchRule rule, ProxyRequest request) {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(request, "request");
        return hostMatches(rule.hostPattern(), request.host())
                && pathMatches(rule.pathPattern(), request.path())
                && methodMatches(rule.methods(), request.method())
                && headersMatch(rule.headers(), request);
    }

    public List<RouteMatch> matchingRoutes(List<RouteSnapshot.ServiceRoute> routes, ProxyRequest request) {
        Objects.requireNonNull(routes, "routes");
        Objects.requireNonNull(request, "request");
        List<RouteMatch> matches = new ArrayList<>();
        for (RouteSnapshot.ServiceRoute route : routes) {
            if (matches(route.matchRule(), request)) {
                matches.add(new RouteMatch(
                        route,
                        route.matchRule().priority(),
                        RouteSpecificity.effectiveScore(route.matchRule())));
            }
        }
        matches.sort(MATCH_ORDER);
        return List.copyOf(matches);
    }

    public RouteSelection select(List<RouteSnapshot.ServiceRoute> routes, ProxyRequest request) {
        List<RouteMatch> candidates = matchingRoutes(routes, request);
        boolean ambiguousTopCandidates = hasAmbiguousTopCandidates(candidates);
        return new RouteSelection(
                candidates.isEmpty() || ambiguousTopCandidates
                        ? java.util.Optional.empty()
                        : java.util.Optional.of(candidates.get(0).route()),
                candidates,
                conflicts(candidates));
    }

    public List<RouteConflict> conflicts(List<RouteMatch> sortedCandidates) {
        Objects.requireNonNull(sortedCandidates, "sortedCandidates");
        List<RouteConflict> conflicts = new ArrayList<>();
        for (int firstIndex = 0; firstIndex < sortedCandidates.size(); firstIndex++) {
            RouteMatch first = sortedCandidates.get(firstIndex);
            for (int secondIndex = firstIndex + 1; secondIndex < sortedCandidates.size(); secondIndex++) {
                RouteMatch second = sortedCandidates.get(secondIndex);
                if (first.priority() != second.priority()
                        || first.specificityScore() != second.specificityScore()) {
                    continue;
                }
                conflicts.add(new RouteConflict(
                        first.route(),
                        second.route(),
                        first.priority(),
                        first.specificityScore(),
                        AMBIGUOUS_EQUAL_PRIORITY_AND_SPECIFICITY));
            }
        }
        return List.copyOf(conflicts);
    }

    private static boolean hasAmbiguousTopCandidates(List<RouteMatch> sortedCandidates) {
        if (sortedCandidates.size() < 2) {
            return false;
        }
        RouteMatch first = sortedCandidates.get(0);
        RouteMatch second = sortedCandidates.get(1);
        return first.priority() == second.priority()
                && first.specificityScore() == second.specificityScore();
    }

    private static boolean hostMatches(String pattern, String host) {
        String normalizedPattern = ProxyRequest.normalizeHost(pattern);
        if ("*".equals(normalizedPattern)) {
            return true;
        }
        if (normalizedPattern.startsWith("*.")) {
            String suffix = normalizedPattern.substring(1);
            if (!host.endsWith(suffix)) {
                return false;
            }
            String wildcardLabel = host.substring(0, host.length() - suffix.length());
            return !wildcardLabel.isBlank() && wildcardLabel.indexOf('.') < 0;
        }
        return normalizedPattern.equals(host);
    }

    private static boolean pathMatches(String pattern, String path) {
        String normalizedPattern = ProxyRequest.normalizePath(pattern);
        if ("/*".equals(normalizedPattern) || "*".equals(pattern.trim())) {
            return true;
        }
        if (normalizedPattern.endsWith("/*")) {
            String prefix = normalizedPattern.substring(0, normalizedPattern.length() - 2);
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        return normalizedPattern.equals(path);
    }

    private static boolean methodMatches(Set<String> methods, String requestMethod) {
        if (methods.isEmpty()) {
            return true;
        }
        for (String method : methods) {
            if (ProxyRequest.normalizeMethod(method).equals(requestMethod)) {
                return true;
            }
        }
        return false;
    }

    private static boolean headersMatch(Map<String, String> headers, ProxyRequest request) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            String actual = request.header(header.getKey());
            if (!Objects.equals(header.getValue(), actual)) {
                return false;
            }
        }
        return true;
    }
}
