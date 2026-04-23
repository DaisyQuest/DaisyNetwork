plugins {
    `java-library`
}

group = "dev.daisynetwork"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
