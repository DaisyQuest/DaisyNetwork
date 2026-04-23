package dev.daisynetwork.proxy;

import java.util.List;

public interface WafRule {
    String ruleId();

    String name();

    String category();

    WafSeverity severity();

    int paranoiaLevel();

    List<WafRuleMatch> evaluate(WafRequest request, WafPolicy policy);
}
