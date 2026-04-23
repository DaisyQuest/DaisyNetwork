# DaisyNetwork

DaisyNetwork owns the reverse-proxy and network policy contracts extracted from OpenAppServiceContainer.

The initial library surface includes:

- route snapshots, route match rules, route selection, and activation validation
- backend endpoint health and pool eligibility contracts
- traffic policy and load-balancing strategy types
- a composable WAF interface with OWASP managed rules and custom rule support
- read-model contracts used by control-plane and dashboard integrations

OpenAppServiceContainer consumes this project at build time from the sibling `DaisyNetwork/src/main/java` source root. DaisyCloud consumes it as a composite Gradle build during local development and can consume the published GitHub Packages artifact in CI or downstream builds.

Run local validation with:

```powershell
.\scripts\check.ps1
```

Run the Gradle build and Maven-local publication check with:

```powershell
.\gradlew.bat clean build publishToMavenLocal
```

The GitHub Actions publishing workflow publishes the build-time package:

```kotlin
implementation("dev.daisynetwork:daisynetwork:0.1.0-SNAPSHOT")
```

Consumers should add `https://maven.pkg.github.com/DaisyQuest/DaisyNetwork` as a Maven repository and provide `GITHUB_ACTOR` plus a token with `read:packages` access.
