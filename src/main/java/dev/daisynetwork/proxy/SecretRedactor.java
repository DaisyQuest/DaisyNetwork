package dev.daisynetwork.proxy;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class SecretRedactor {
    public static final String REDACTED = "[REDACTED]";

    private static final Set<String> SECRET_TOKENS = Set.of(
            "password",
            "secret",
            "token",
            "credential",
            "privatekey",
            "apikey",
            "authorization",
            "bearer",
            "cookie",
            "session",
            "passphrase",
            "certificate",
            "tlskey",
            "oauth",
            "clientsecret",
            "awssecretaccesskey",
            "azureclientsecret",
            "gcpserviceaccountkey",
            "openaiapikey",
            "githubtoken",
            "stripeapikey");

    public boolean isSecretKey(String key) {
        Objects.requireNonNull(key, "key");
        String normalized = normalize(key);
        for (String token : SECRET_TOKENS) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder(lower.length());
        for (int index = 0; index < lower.length(); index++) {
            char current = lower.charAt(index);
            if (Character.isLetterOrDigit(current)) {
                normalized.append(current);
            }
        }
        return normalized.toString();
    }
}
