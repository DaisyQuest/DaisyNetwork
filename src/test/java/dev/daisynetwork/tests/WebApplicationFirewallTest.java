package dev.daisynetwork.tests;

import dev.daisynetwork.proxy.DefaultWebApplicationFirewall;
import dev.daisynetwork.proxy.PatternWafRule;
import dev.daisynetwork.proxy.WafAction;
import dev.daisynetwork.proxy.WafDecision;
import dev.daisynetwork.proxy.WafPolicy;
import dev.daisynetwork.proxy.WafRequest;
import dev.daisynetwork.proxy.WafSeverity;
import dev.daisynetwork.proxy.WebApplicationFirewall;
import dev.daisynetwork.testing.Assertions;
import dev.daisynetwork.testing.SpecTest;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WebApplicationFirewallTest {
    private final WebApplicationFirewall firewall = new DefaultWebApplicationFirewall();

    @SpecTest
    public void disabledPolicyAlwaysAllowsWithoutMatches() {
        WafDecision decision = firewall.inspect(request("/search"), WafPolicy.disabled());

        Assertions.assertFalse(decision.enabled(), "disabled policy must be reported as disabled");
        Assertions.assertEquals(WafAction.ALLOW, decision.action(), "disabled policy must allow");
        Assertions.assertEquals(0, decision.matches().size(), "disabled policy must skip rule evaluation");
    }

    @SpecTest
    public void customRuleComposesWithOwaspRules() {
        WafPolicy policy = new WafPolicy(
                true,
                WafAction.BLOCK,
                2,
                5,
                8192,
                true,
                Set.of(),
                List.of(new PatternWafRule(
                        "CUSTOM-001",
                        "Block internal token leakage",
                        "custom",
                        WafSeverity.CRITICAL,
                        1,
                        PatternWafRule.Target.QUERY,
                        "internal-token",
                        "internal token leaked in query")));

        WafDecision decision = firewall.inspect(
                new WafRequest("api.example.com", "/orders", "GET", Map.of(), Map.of("debug", "internal-token"), "", ""),
                policy);

        Assertions.assertEquals(WafAction.BLOCK, decision.action(), "custom critical rule must block");
        Assertions.assertContains(decision.matches().toString(), "CUSTOM-001", "custom rule match must be present");
    }

    private static WafRequest request(String target) {
        return new WafRequest("api.example.com", target, "GET", Map.of(), Map.of(), "", "");
    }
}
