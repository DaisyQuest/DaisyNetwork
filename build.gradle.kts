plugins {
    `java-library`
    `maven-publish`
}

group = "dev.daisynetwork"
version = providers.gradleProperty("version").orElse("0.1.0-SNAPSHOT").get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
}

val specTest by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs DaisyNetwork's contract-level proxy and WAF tests."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("dev.daisynetwork.testing.SpecTestRunner")
}

tasks.named<Test>("test") {
    enabled = false
}

tasks.named("check") {
    dependsOn(specTest)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("DaisyNetwork")
                description.set("Network policy, reverse-proxy, and WAF contracts for DaisyCloud app services.")
                url.set("https://github.com/DaisyQuest/DaisyNetwork")
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/DaisyQuest/DaisyNetwork")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orElse(providers.environmentVariable("GITHUB_USERNAME"))
                    .orElse("not-set")
                    .get()
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orElse(providers.environmentVariable("GITHUB_PACKAGES_TOKEN"))
                    .orElse("not-set")
                    .get()
            }
        }
    }
}
