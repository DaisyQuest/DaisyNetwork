package dev.daisynetwork.proxy;

import java.util.Objects;

public record WafRuleMatch(
        String ruleId,
        String ruleName,
        String category,
        WafSeverity severity,
        int paranoiaLevel,
        String location,
        String evidence,
        String message) {
    public WafRuleMatch {
        ruleId = requireText(ruleId, "ruleId");
        ruleName = requireText(ruleName, "ruleName");
        category = requireText(category, "category");
        Objects.requireNonNull(severity, "severity");
        if (paranoiaLevel < 1 || paranoiaLevel > 4) {
            throw new IllegalArgumentException("paranoiaLevel must be between 1 and 4");
        }
        location = requireText(location, "location");
        evidence = redactEvidence(location, truncate(requireText(evidence, "evidence"), 160));
        message = requireText(message, "message");
    }

    public int anomalyScore() {
        return severity.anomalyScore();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String redactEvidence(String location, String evidence) {
        SecretRedactor redactor = new SecretRedactor();
        if (redactor.isSecretKey(location) || redactor.isSecretKey(evidence)) {
            return SecretRedactor.REDACTED;
        }
        return evidence;
    }
}
