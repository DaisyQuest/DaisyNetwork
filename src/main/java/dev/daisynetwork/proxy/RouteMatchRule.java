package dev.daisynetwork.proxy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record RouteMatchRule(
        String hostPattern,
        String pathPattern,
        Set<String> methods,
        Map<String, String> headers,
        int priority,
        int specificityScore) {
    public RouteMatchRule {
        hostPattern = requireText(hostPattern, "hostPattern");
        pathPattern = requireText(pathPattern, "pathPattern");
        methods = normalizeMethods(methods);
        headers = normalizeHeaders(headers);
        if (specificityScore < 0) {
            throw new IllegalArgumentException("specificityScore must not be negative");
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        rejectControlCharacters(trimmed, field);
        return trimmed;
    }

    private static Set<String> normalizeMethods(Set<String> methods) {
        Objects.requireNonNull(methods, "methods");
        TreeSet<String> normalized = new TreeSet<>();
        for (String method : methods) {
            String value = requireText(method, "method");
            if ("*".equals(value)) {
                normalized.add(value);
            } else {
                normalized.add(ProxyRequest.normalizeMethod(value));
            }
        }
        if (normalized.contains("*") && normalized.size() > 1) {
            throw new IllegalArgumentException("method wildcard must not be combined with explicit methods");
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static Map<String, String> normalizeHeaders(Map<String, String> headers) {
        Objects.requireNonNull(headers, "headers");
        Map<String, String> normalized = new LinkedHashMap<>();
        headers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String name = ProxyRequest.normalizeHeaderName(entry.getKey());
                    String value = Objects.requireNonNull(entry.getValue(), "header value for " + name);
                    rejectControlCharacters(value, "header value");
                    if (normalized.containsKey(name)) {
                        throw new IllegalArgumentException("duplicate header matcher after normalization: " + name);
                    }
                    normalized.put(name, value);
                });
        return Map.copyOf(normalized);
    }

    private static void rejectControlCharacters(String value, String field) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException(field + " must not contain control characters");
            }
        }
    }
}
