package dev.daisynetwork.proxy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record WafRequest(
        String host,
        String path,
        String method,
        Map<String, String> headers,
        Map<String, String> query,
        String body,
        String clientIp) {
    public WafRequest {
        host = ProxyRequest.normalizeHost(host);
        path = ProxyRequest.normalizePath(path);
        method = ProxyRequest.normalizeMethod(method);
        headers = normalizeHeaders(headers, "header");
        query = normalizeFields(query, "query");
        body = body == null ? "" : body;
        clientIp = normalizeOptional(clientIp);
    }

    public static WafRequest fromProxyRequest(ProxyRequest request) {
        Objects.requireNonNull(request, "request");
        return new WafRequest(
                request.host(),
                request.path(),
                request.method(),
                request.headers(),
                Map.of(),
                "",
                "");
    }

    public String header(String name) {
        return headers.get(ProxyRequest.normalizeHeaderName(name));
    }

    public Map<String, String> inspectionTargets(int maxBodyInspectionBytes) {
        if (maxBodyInspectionBytes < 0) {
            throw new IllegalArgumentException("maxBodyInspectionBytes must not be negative");
        }
        Map<String, String> targets = new LinkedHashMap<>();
        targets.put("method", method);
        targets.put("host", host);
        targets.put("path", path);
        headers.forEach((key, value) -> targets.put("header." + key, value));
        query.forEach((key, value) -> targets.put("query." + key, value));
        if (!body.isBlank() && maxBodyInspectionBytes > 0) {
            targets.put("body", truncateByBytes(body, maxBodyInspectionBytes));
        }
        if (!clientIp.isBlank()) {
            targets.put("clientIp", clientIp);
        }
        return Collections.unmodifiableMap(targets);
    }

    private static Map<String, String> normalizeHeaders(Map<String, String> values, String label) {
        Objects.requireNonNull(values, label + "s");
        Map<String, String> normalized = new TreeMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = ProxyRequest.normalizeHeaderName(entry.getKey());
            String value = normalizeFieldValue(entry.getValue(), label + " value");
            if (normalized.containsKey(key)) {
                throw new IllegalArgumentException("duplicate " + label + " key after normalization: " + key);
            }
            normalized.put(key, value);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(normalized));
    }

    private static Map<String, String> normalizeFields(Map<String, String> values, String label) {
        Objects.requireNonNull(values, label + "s");
        Map<String, String> normalized = new TreeMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = normalizeFieldName(entry.getKey(), label + " key");
            String value = normalizeFieldValue(entry.getValue(), label + " value");
            if (normalized.containsKey(key)) {
                throw new IllegalArgumentException("duplicate " + label + " key after normalization: " + key);
            }
            normalized.put(key, value);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(normalized));
    }

    private static String normalizeFieldName(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        rejectControlCharacters(normalized, field);
        return normalized;
    }

    private static String normalizeFieldValue(String value, String field) {
        Objects.requireNonNull(value, field);
        rejectControlCharacters(value, field);
        return value;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        rejectControlCharacters(trimmed, "clientIp");
        return trimmed;
    }

    private static void rejectControlCharacters(String value, String field) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException(field + " must not contain control characters");
            }
        }
    }

    private static String truncateByBytes(String value, int maxBytes) {
        int bytes = 0;
        StringBuilder retained = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            int nextBytes = ch <= 0x7F ? 1 : String.valueOf(ch).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (bytes + nextBytes > maxBytes) {
                break;
            }
            retained.append(ch);
            bytes += nextBytes;
        }
        return retained.toString();
    }
}
