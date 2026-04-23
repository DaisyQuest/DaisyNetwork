package dev.daisynetwork.proxy;

import java.util.Objects;

public final class RouteSpecificity {
    private static final int EXACT_HOST_WEIGHT = 10_000;
    private static final int WILDCARD_HOST_WEIGHT = 5_000;
    private static final int EXACT_PATH_WEIGHT = 10_000;
    private static final int PREFIX_PATH_WEIGHT = 5_000;
    private static final int METHOD_WEIGHT = 500;
    private static final int HEADER_WEIGHT = 250;

    private RouteSpecificity() {
    }

    public static int effectiveScore(RouteMatchRule rule) {
        Objects.requireNonNull(rule, "rule");
        return rule.specificityScore()
                + hostScore(rule.hostPattern())
                + pathScore(rule.pathPattern())
                + methodScore(rule)
                + headerScore(rule);
    }

    private static int hostScore(String hostPattern) {
        String normalized = ProxyRequest.normalizeHost(hostPattern);
        if ("*".equals(normalized)) {
            return 0;
        }
        if (normalized.startsWith("*.")) {
            return WILDCARD_HOST_WEIGHT + normalized.length();
        }
        return EXACT_HOST_WEIGHT + normalized.length();
    }

    private static int pathScore(String pathPattern) {
        String normalized = ProxyRequest.normalizePath(pathPattern);
        if ("/*".equals(normalized) || "*".equals(pathPattern.trim())) {
            return 0;
        }
        if (normalized.endsWith("/*")) {
            return PREFIX_PATH_WEIGHT + normalized.length();
        }
        return EXACT_PATH_WEIGHT + normalized.length();
    }

    private static int methodScore(RouteMatchRule rule) {
        return rule.methods().isEmpty() ? 0 : METHOD_WEIGHT + rule.methods().size();
    }

    private static int headerScore(RouteMatchRule rule) {
        return rule.headers().size() * HEADER_WEIGHT;
    }
}
