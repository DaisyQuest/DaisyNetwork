package dev.daisynetwork.proxy;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record WafPolicy(
        boolean enabled,
        WafAction action,
        int paranoiaLevel,
        int blockThreshold,
        int maxBodyInspectionBytes,
        boolean includeOwaspManagedRules,
        Set<String> disabledRuleIds,
        List<WafRule> customRules) {
    public WafPolicy {
        Objects.requireNonNull(action, "action");
        if (paranoiaLevel < 1 || paranoiaLevel > 4) {
            throw new IllegalArgumentException("paranoiaLevel must be between 1 and 4");
        }
        if (blockThreshold < 1) {
            throw new IllegalArgumentException("blockThreshold must be at least 1");
        }
        if (maxBodyInspectionBytes < 0) {
            throw new IllegalArgumentException("maxBodyInspectionBytes must not be negative");
        }
        disabledRuleIds = immutableSortedSet(disabledRuleIds);
        customRules = Objects.requireNonNull(customRules, "customRules").stream()
                .sorted(Comparator.comparing(WafRule::ruleId))
                .toList();
        Set<String> ruleIds = new HashSet<>();
        for (WafRule rule : customRules) {
            if (!ruleIds.add(rule.ruleId())) {
                throw new IllegalArgumentException("customRules must not contain duplicate rule id " + rule.ruleId());
            }
        }
    }

    public static WafPolicy disabled() {
        return new WafPolicy(false, WafAction.ALLOW, 1, 5, 8192, true, Set.of(), List.of());
    }

    public static WafPolicy owaspEnabled(int paranoiaLevel, int blockThreshold) {
        return new WafPolicy(true, WafAction.BLOCK, paranoiaLevel, blockThreshold, 8192, true, Set.of(), List.of());
    }

    public WafPolicy withCustomRules(List<WafRule> nextCustomRules) {
        return new WafPolicy(
                enabled,
                action,
                paranoiaLevel,
                blockThreshold,
                maxBodyInspectionBytes,
                includeOwaspManagedRules,
                disabledRuleIds,
                nextCustomRules);
    }

    public boolean ruleEnabled(WafRule rule) {
        Objects.requireNonNull(rule, "rule");
        return rule.paranoiaLevel() <= paranoiaLevel && !disabledRuleIds.contains(rule.ruleId());
    }

    private static Set<String> immutableSortedSet(Set<String> values) {
        Objects.requireNonNull(values, "disabledRuleIds");
        TreeSet<String> sorted = new TreeSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("disabledRuleIds must not contain blank rule ids");
            }
            sorted.add(value.trim());
        }
        return Set.copyOf(sorted);
    }
}
