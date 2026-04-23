package dev.daisynetwork.proxy;

import java.time.Duration;
import java.util.Objects;

public record TrafficPolicy(
        LoadBalancingStrategy strategy,
        Duration connectTimeout,
        Duration responseTimeout,
        int retryBudget,
        boolean stickyAffinity) {
    public TrafficPolicy {
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(connectTimeout, "connectTimeout");
        if (connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        Objects.requireNonNull(responseTimeout, "responseTimeout");
        if (responseTimeout.isNegative() || responseTimeout.isZero()) {
            throw new IllegalArgumentException("responseTimeout must be positive");
        }
        if (retryBudget < 0) {
            throw new IllegalArgumentException("retryBudget must not be negative");
        }
    }
}
