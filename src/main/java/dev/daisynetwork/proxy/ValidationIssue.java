package dev.daisynetwork.proxy;

import java.util.Objects;

public record ValidationIssue(Severity severity, String path, String message) {
    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }

    public ValidationIssue {
        Objects.requireNonNull(severity, "severity");
        path = requireText(path, "path");
        message = requireText(message, "message");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
