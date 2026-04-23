package dev.daisynetwork.proxy;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class HeaderValueWafRule implements WafRule {
    private final String ruleId;
    private final String name;
    private final String category;
    private final WafSeverity severity;
    private final int paranoiaLevel;
    private final String headerName;
    private final Pattern pattern;
    private final boolean matchWhenMissing;
    private final String message;

    public HeaderValueWafRule(
            String ruleId,
            String name,
            String category,
            WafSeverity severity,
            int paranoiaLevel,
            String headerName,
            String regex,
            boolean matchWhenMissing,
            String message) {
        this.ruleId = requireText(ruleId, "ruleId");
        this.name = requireText(name, "name");
        this.category = requireText(category, "category");
        this.severity = Objects.requireNonNull(severity, "severity");
        if (paranoiaLevel < 1 || paranoiaLevel > 4) {
            throw new IllegalArgumentException("paranoiaLevel must be between 1 and 4");
        }
        this.paranoiaLevel = paranoiaLevel;
        this.headerName = ProxyRequest.normalizeHeaderName(headerName);
        this.pattern = compileRegex(regex);
        this.matchWhenMissing = matchWhenMissing;
        this.message = requireText(message, "message");
    }

    @Override
    public String ruleId() {
        return ruleId;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String category() {
        return category;
    }

    @Override
    public WafSeverity severity() {
        return severity;
    }

    @Override
    public int paranoiaLevel() {
        return paranoiaLevel;
    }

    @Override
    public List<WafRuleMatch> evaluate(WafRequest request, WafPolicy policy) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(policy, "policy");
        String value = request.header(headerName);
        if (value == null) {
            return matchWhenMissing
                    ? List.of(match("header." + headerName, "<missing>"))
                    : List.of();
        }
        return pattern.matcher(value).find()
                ? List.of(match("header." + headerName, value))
                : List.of();
    }

    private WafRuleMatch match(String location, String evidence) {
        return new WafRuleMatch(
                ruleId,
                name,
                category,
                severity,
                paranoiaLevel,
                location,
                evidence,
                message);
    }

    private static Pattern compileRegex(String regex) {
        String value = requireText(regex, "regex");
        if (value.length() > 512) {
            throw new IllegalArgumentException("regex must not exceed 512 characters");
        }
        rejectBacktrackingProneRegex(value);
        try {
            return Pattern.compile(value, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("regex must compile", e);
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }

    private static void rejectBacktrackingProneRegex(String regex) {
        if (regex.contains(".*.*")
                || regex.matches(".*\\([^)]*[+*][^)]*\\)[+*].*")
                || regex.contains("\\1")
                || regex.contains("\\2")
                || regex.contains("(?<=")
                || regex.contains("(?<!")) {
            throw new IllegalArgumentException("regex must avoid backtracking-prone constructs");
        }
    }
}
