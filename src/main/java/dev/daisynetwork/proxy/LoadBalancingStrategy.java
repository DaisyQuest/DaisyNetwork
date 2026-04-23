package dev.daisynetwork.proxy;

public enum LoadBalancingStrategy {
    ROUND_ROBIN,
    WEIGHTED,
    LEAST_LOADED,
    LATENCY_AWARE,
    ERROR_AWARE,
    POLICY_SCRIPTED
}
