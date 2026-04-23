package dev.daisynetwork.proxy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record RouteAction(
        String backendPoolId,
        String rewritePrefix,
        String redirectTo,
        Map<String, String> headerTransforms,
        boolean maintenanceResponse) {
    private static final Set<String> BLOCKED_TRANSFORM_HEADERS = Set.of(
            "authorization",
            "cookie",
            "set-cookie",
            "host",
            "forwarded",
            "x-real-ip",
            "x-forwarded-for",
            "x-forwarded-host",
            "x-forwarded-proto",
            "x-forwarded-port",
            "connection",
            "keep-alive",
            "proxy-connection",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "content-length");

    public RouteAction {
        backendPoolId = requireText(backendPoolId, "backendPoolId");
        rewritePrefix = normalizePathPrefix(rewritePrefix, "rewritePrefix");
        redirectTo = normalizeLocalRedirect(redirectTo);
        headerTransforms = normalizeHeaderTransforms(headerTransforms);
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

    private static String normalizePathPrefix(String value, String field) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = ProxyRequest.normalizePath(value);
        if (normalized.contains("?") || normalized.contains("#") || normalized.contains("\\")
                || normalized.contains("%2f") || normalized.contains("%2F")
                || normalized.contains("%5c") || normalized.contains("%5C")
                || normalized.contains("%2e") || normalized.contains("%2E")
                || normalized.contains("%0d") || normalized.contains("%0D")
                || normalized.contains("%0a") || normalized.contains("%0A")
                || normalized.contains("/../") || normalized.endsWith("/..") || normalized.equals("/..")) {
            throw new IllegalArgumentException(field + " must be a safe path prefix");
        }
        return normalized;
    }

    private static String normalizeLocalRedirect(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String redirect = requireText(value, "redirectTo");
        if (!redirect.startsWith("/") || redirect.startsWith("//")) {
            throw new IllegalArgumentException("redirectTo must be a local absolute path");
        }
        if (redirect.contains("\\") || redirect.contains("%2f") || redirect.contains("%2F")
                || redirect.contains("%5c") || redirect.contains("%5C")
                || redirect.contains("%2e") || redirect.contains("%2E")
                || redirect.contains("%0d") || redirect.contains("%0D")
                || redirect.contains("%0a") || redirect.contains("%0A")) {
            throw new IllegalArgumentException("redirectTo must not contain encoded path separators, dots, or controls");
        }
        try {
            URI uri = new URI(redirect);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("redirectTo must stay within the local origin and omit fragments");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("redirectTo must be a valid local URI path", e);
        }
        return redirect;
    }

    private static Map<String, String> normalizeHeaderTransforms(Map<String, String> transforms) {
        Objects.requireNonNull(transforms, "headerTransforms");
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : transforms.entrySet()) {
            String name = ProxyRequest.normalizeHeaderName(entry.getKey());
            if (BLOCKED_TRANSFORM_HEADERS.contains(name) || name.startsWith("x-forwarded-")) {
                throw new IllegalArgumentException("headerTransforms must not mutate security-sensitive header " + name);
            }
            String value = Objects.requireNonNull(entry.getValue(), "header transform value for " + name);
            rejectControlCharacters(value, "header transform value");
            if (normalized.containsKey(name)) {
                throw new IllegalArgumentException("duplicate header transform after normalization: " + name);
            }
            normalized.put(name, value);
        }
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
