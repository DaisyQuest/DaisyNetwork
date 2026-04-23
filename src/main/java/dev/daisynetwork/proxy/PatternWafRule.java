package dev.daisynetwork.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class PatternWafRule implements WafRule {
    public enum Target {
        HOST,
        PATH,
        HEADER,
        QUERY,
        BODY,
        ANY
    }

    private final String ruleId;
    private final String name;
    private final String category;
    private final WafSeverity severity;
    private final int paranoiaLevel;
    private final Target target;
    private final Pattern pattern;
    private final String message;

    public PatternWafRule(
            String ruleId,
            String name,
            String category,
            WafSeverity severity,
            int paranoiaLevel,
            Target target,
            String regex,
            String message) {
        this.ruleId = requireText(ruleId, "ruleId");
        this.name = requireText(name, "name");
        this.category = requireText(category, "category");
        this.severity = Objects.requireNonNull(severity, "severity");
        if (paranoiaLevel < 1 || paranoiaLevel > 4) {
            throw new IllegalArgumentException("paranoiaLevel must be between 1 and 4");
        }
        this.paranoiaLevel = paranoiaLevel;
        this.target = Objects.requireNonNull(target, "target");
        this.pattern = compileRegex(regex);
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
        List<WafRuleMatch> matches = new ArrayList<>();
        for (Map.Entry<String, String> entry : targets(request, policy).entrySet()) {
            java.util.regex.Matcher matcher = pattern.matcher(entry.getValue());
            if (matcher.find()) {
                matches.add(new WafRuleMatch(
                        ruleId,
                        name,
                        category,
                        severity,
                        paranoiaLevel,
                        entry.getKey(),
                        matcher.group(),
                        message));
            }
        }
        return List.copyOf(matches);
    }

    private Map<String, String> targets(WafRequest request, WafPolicy policy) {
        Map<String, String> allTargets = request.inspectionTargets(policy.maxBodyInspectionBytes());
        if (target == Target.ANY) {
            return allTargets;
        }

        Set<String> prefixes = switch (target) {
            case HOST -> Set.of("host");
            case PATH -> Set.of("path");
            case HEADER -> Set.of("header.");
            case QUERY -> Set.of("query.");
            case BODY -> Set.of("body");
            case ANY -> Set.of();
        };
        java.util.LinkedHashMap<String, String> filtered = new java.util.LinkedHashMap<>();
        allTargets.forEach((key, value) -> {
            for (String prefix : prefixes) {
                if (key.equals(prefix) || key.startsWith(prefix)) {
                    filtered.put(key, value);
                    break;
                }
            }
        });
        return Map.copyOf(filtered);
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
