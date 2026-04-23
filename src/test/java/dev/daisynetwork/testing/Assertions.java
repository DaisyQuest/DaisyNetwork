package dev.daisynetwork.testing;

import java.util.Collection;
import java.util.Objects;

public final class Assertions {
    private Assertions() {
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " Expected <" + expected + "> but was <" + actual + ">.");
        }
    }

    public static void assertContains(String haystack, String needle, String message) {
        assertTrue(haystack.contains(needle), message + " Missing text: " + needle);
    }

    public static void assertContainsAll(String haystack, Collection<String> needles, String message) {
        for (String needle : needles) {
            assertContains(haystack, needle, message);
        }
    }
}
