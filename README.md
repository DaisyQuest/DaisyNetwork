# DaisyNetwork

DaisyNetwork owns the reverse-proxy and network policy contracts extracted from OpenAppServiceContainer.

The initial library surface includes:

- route snapshots, route match rules, route selection, and activation validation
- backend endpoint health and pool eligibility contracts
- traffic policy and load-balancing strategy types
- a composable WAF interface with OWASP managed rules and custom rule support
- read-model contracts used by control-plane and dashboard integrations

OpenAppServiceContainer consumes this project at build time from the sibling `DaisyNetwork/src/main/java` source root. DaisyCloud consumes it as a composite Gradle build.

Run local validation with:

```powershell
.\scripts\check.ps1
```
