package dev.daisynetwork.proxy;

public interface WebApplicationFirewall {
    WafDecision inspect(WafRequest request, WafPolicy policy);
}
