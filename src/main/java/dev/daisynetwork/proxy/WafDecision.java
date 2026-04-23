package dev.daisynetwork.proxy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record WafDecision(
        boolean enabled,
        WafAction action,
        int anomalyScore,
        int blockThreshold,
        List<WafRuleMatch> matches,
        Map<String, Integer> categoryScores) {
    public WafDecision {
        Objects.requireNonNull(action, "action");
        if (anomalyScore < 0) {
            throw new IllegalArgumentException("anomalyScore must not be negative");
        }
        if (blockThreshold < 1) {
            throw new IllegalArgumentException("blockThreshold must be at least 1");
        }
        matches = Objects.requireNonNull(matches, "matches").stream()
                .sorted(Comparator
                        .comparing(WafRuleMatch::ruleId)
                        .thenComparing(WafRuleMatch::location)
                        .thenComparing(WafRuleMatch::evidence))
                .toList();
        categoryScores = Map.copyOf(new TreeMap<>(Objects.requireNonNull(categoryScores, "categoryScores")));
    }

    public static WafDecision disabled(int blockThreshold) {
        return new WafDecision(false, WafAction.ALLOW, 0, blockThreshold, List.of(), Map.of());
    }

    public boolean blocked() {
        return enabled && action == WafAction.BLOCK;
    }
}
