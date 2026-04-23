package dev.daisynetwork.proxy;

import java.util.Objects;

public record Identity(String value) {
    public Identity {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("identity value must not be blank");
        }
    }
}
