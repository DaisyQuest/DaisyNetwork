package dev.daisynetwork.proxy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultWebApplicationFirewall implements WebApplicationFirewall {
    private final List<WafRule> managedRules;

    public DefaultWebApplicationFirewall() {
        this(OwaspManagedWafRules.baseline());
    }

    public DefaultWebApplicationFirewall(List<WafRule> managedRules) {
        this.managedRules = Objects.requireNonNull(managedRules, "managedRules").stream()
                .sorted(Comparator.comparing(WafRule::ruleId))
                .toList();
    }

    @Override
    public WafDecision inspect(WafRequest request, WafPolicy policy) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(policy, "policy");
        if (!policy.enabled()) {
            return WafDecision.disabled(policy.blockThreshold());
        }

        List<WafRuleMatch> matches = new ArrayList<>();
        for (WafRule rule : activeRules(policy)) {
            if (policy.ruleEnabled(rule)) {
                matches.addAll(rule.evaluate(request, policy));
            }
        }
        matches.sort(Comparator
                .comparing(WafRuleMatch::ruleId)
                .thenComparing(WafRuleMatch::location)
                .thenComparing(WafRuleMatch::evidence));

        int anomalyScore = matches.stream().mapToInt(WafRuleMatch::anomalyScore).sum();
        Map<String, Integer> categoryScores = new LinkedHashMap<>();
        for (WafRuleMatch match : matches) {
            categoryScores.merge(match.category(), match.anomalyScore(), Integer::sum);
        }
        WafAction action = actionFor(policy, anomalyScore);
        return new WafDecision(
                true,
                action,
                anomalyScore,
                policy.blockThreshold(),
                matches,
                categoryScores);
    }

    private List<WafRule> activeRules(WafPolicy policy) {
        List<WafRule> rules = new ArrayList<>();
        if (policy.includeOwaspManagedRules()) {
            rules.addAll(managedRules);
        }
        rules.addAll(policy.customRules());
        rules.sort(Comparator.comparing(WafRule::ruleId));
        return List.copyOf(rules);
    }

    private static WafAction actionFor(WafPolicy policy, int anomalyScore) {
        if (anomalyScore <= 0) {
            return WafAction.ALLOW;
        }
        if (policy.action() == WafAction.LOG) {
            return WafAction.LOG;
        }
        if (policy.action() == WafAction.BLOCK && anomalyScore >= policy.blockThreshold()) {
            return WafAction.BLOCK;
        }
        return WafAction.LOG;
    }
}
