package dev.daisynetwork.proxy;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record ProxyRequest(
        String host,
        String path,
        String method,
        Map<String, String> headers) {
    public ProxyRequest {
        host = normalizeHost(host);
        path = normalizePath(path);
        method = normalizeMethod(method);
        headers = normalizeHeaders(headers);
    }

    public String header(String name) {
        return headers.get(normalizeHeaderName(name));
    }

    static String normalizeHost(String host) {
        String normalized = requireText(host, "host").toLowerCase(Locale.ROOT);
        rejectHostConfusionCharacters(normalized);
        if (normalized.startsWith("[")) {
            return normalizeBracketedIpv6(normalized);
        }

        int firstColon = normalized.indexOf(':');
        if (firstColon < 0) {
            return stripTrailingDots(normalized);
        }
        if (normalized.indexOf(':', firstColon + 1) >= 0) {
            throw new IllegalArgumentException("host must use brackets for IPv6 literals");
        }

        String hostPart = normalized.substring(0, firstColon);
        String portPart = normalized.substring(firstColon + 1);
        if (hostPart.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        parsePort(portPart);
        return stripTrailingDots(hostPart);
    }

    static String normalizePath(String path) {
        String normalized = requireText(path, "path");
        if (normalized.contains("?") || normalized.contains("#") || normalized.contains("\\")) {
            throw new IllegalArgumentException("path must not contain query, fragment, or backslash characters");
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (lowered.contains("%2f") || lowered.contains("%5c")) {
            throw new IllegalArgumentException("path must not contain encoded path separators");
        }
        if (lowered.contains("%2e") || lowered.contains("%0d") || lowered.contains("%0a")) {
            throw new IllegalArgumentException("path must not contain encoded dot or control characters");
        }
        String absolute = normalized.startsWith("/") ? normalized : "/" + normalized;
        if (absolute.equals("/..") || absolute.endsWith("/..") || absolute.contains("/../")
                || absolute.equals("/.") || absolute.endsWith("/.") || absolute.contains("/./")) {
            throw new IllegalArgumentException("path must not contain dot segments");
        }
        return absolute;
    }

    static String normalizeMethod(String method) {
        String normalized = requireText(method, "method").toUpperCase(Locale.ROOT);
        for (int index = 0; index < normalized.length(); index++) {
            char ch = normalized.charAt(index);
            if (ch < 'A' || ch > 'Z') {
                throw new IllegalArgumentException("method must contain only uppercase HTTP token letters");
            }
        }
        return normalized;
    }

    static String normalizeHeaderName(String name) {
        String normalized = requireText(name, "header name").toLowerCase(Locale.ROOT);
        for (int index = 0; index < normalized.length(); index++) {
            char ch = normalized.charAt(index);
            if (!((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '-')) {
                throw new IllegalArgumentException("header name must contain only letters, digits, and hyphen");
            }
        }
        return normalized;
    }

    private static Map<String, String> normalizeHeaders(Map<String, String> headers) {
        Objects.requireNonNull(headers, "headers");
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String name = normalizeHeaderName(entry.getKey());
            if (normalized.containsKey(name)) {
                throw new IllegalArgumentException("duplicate request header after normalization: " + name);
            }
            String value = Objects.requireNonNull(entry.getValue(), "header value");
            rejectControlCharacters(value, "header value");
            normalized.put(name, value);
        }
        return Map.copyOf(normalized);
    }

    private static String normalizeBracketedIpv6(String host) {
        int closingBracket = host.indexOf(']');
        if (closingBracket < 0) {
            throw new IllegalArgumentException("host must contain a closing bracket for IPv6 literals");
        }

        String address = host.substring(1, closingBracket);
        if (address.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }

        if (closingBracket == host.length() - 1) {
            return stripTrailingDots(address);
        }

        if (host.charAt(closingBracket + 1) != ':') {
            throw new IllegalArgumentException("host must use a colon after the IPv6 closing bracket");
        }

        String portPart = host.substring(closingBracket + 2);
        parsePort(portPart);
        return stripTrailingDots(address);
    }

    private static String stripTrailingDots(String value) {
        String normalized = value;
        while (normalized.endsWith(".") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static int parsePort(String portPart) {
        if (portPart.isBlank()) {
            throw new IllegalArgumentException("port must not be blank");
        }
        try {
            int port = Integer.parseInt(portPart);
            if (port < 1 || port > 65_535) {
                throw new IllegalArgumentException("port must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("port must be a valid integer between 1 and 65535", e);
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

    private static void rejectControlCharacters(String value, String field) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException(field + " must not contain control characters");
            }
        }
    }

    private static void rejectHostConfusionCharacters(String host) {
        for (int index = 0; index < host.length(); index++) {
            char ch = host.charAt(index);
            if (Character.isWhitespace(ch) || ch == '/' || ch == '\\' || ch == '@') {
                throw new IllegalArgumentException("host must not contain whitespace or authority confusion characters");
            }
        }
    }
}
