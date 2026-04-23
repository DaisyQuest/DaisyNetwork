package dev.daisynetwork.proxy;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record BackendEndpoint(
        Identity backendId,
        URI address,
        String slot,
        Health health,
        int weight,
        boolean draining) {
    public enum Health {
        UNKNOWN,
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        QUARANTINED
    }

    public BackendEndpoint {
        Objects.requireNonNull(backendId, "backendId");
        Objects.requireNonNull(address, "address");
        validateBackendAddress(address);
        slot = requireText(slot, "slot");
        Objects.requireNonNull(health, "health");
        if (weight < 0) {
            throw new IllegalArgumentException("weight must not be negative");
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static void validateBackendAddress(URI address) {
        String scheme = address.getScheme();
        if (scheme == null || !Set.of("http", "https").contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("backend address must use http or https");
        }
        if (address.getHost() == null || address.getHost().isBlank()) {
            throw new IllegalArgumentException("backend address must include a host");
        }
        if (address.getUserInfo() != null) {
            throw new IllegalArgumentException("backend address must not include user info");
        }
        if (address.getPort() == 0 || address.getPort() < -1) {
            throw new IllegalArgumentException("backend address port must be omitted or between 1 and 65535");
        }
        if (address.getRawQuery() != null || address.getRawFragment() != null) {
            throw new IllegalArgumentException("backend address must not include query or fragment components");
        }
    }
}
