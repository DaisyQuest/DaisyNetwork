$ErrorActionPreference = "Stop"

& (Join-Path $PSScriptRoot "run-tests.ps1")
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "DaisyNetwork checks passed."
