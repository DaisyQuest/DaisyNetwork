package dev.daisynetwork.testing;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class SpecTestRunner {
    private static final String TEST_PACKAGE = "dev.daisynetwork.tests";

    private SpecTestRunner() {
    }

    public static void main(String[] args) throws Exception {
        int passed = 0;
        int failed = 0;
        for (Class<?> testClass : discoverTestClasses()) {
            Object instance = testClass.getDeclaredConstructor().newInstance();
            Method[] methods = testClass.getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName));
            for (Method method : methods) {
                if (!method.isAnnotationPresent(SpecTest.class)) {
                    continue;
                }
                try {
                    method.invoke(instance);
                    passed++;
                    System.out.println("PASS " + testClass.getSimpleName() + "." + method.getName());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    failed++;
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    System.err.println("FAIL " + testClass.getSimpleName() + "." + method.getName());
                    cause.printStackTrace(System.err);
                }
            }
        }
        System.out.println("Test result: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static List<Class<?>> discoverTestClasses() throws IOException, URISyntaxException {
        String packagePath = TEST_PACKAGE.replace('.', '/');
        URL packageUrl = Thread.currentThread().getContextClassLoader().getResource(packagePath);
        if (packageUrl == null) {
            throw new IllegalStateException("Unable to find compiled test package " + TEST_PACKAGE);
        }
        Path packageDirectory = Path.of(packageUrl.toURI());
        try (Stream<Path> paths = Files.list(packageDirectory)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith("Test.class"))
                    .filter(path -> !path.getFileName().toString().contains("$"))
                    .map(path -> TEST_PACKAGE + "." + path.getFileName().toString().replace(".class", ""))
                    .sorted()
                    .map(SpecTestRunner::loadClass)
                    .toList();
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load test class " + className, e);
        }
    }
}
