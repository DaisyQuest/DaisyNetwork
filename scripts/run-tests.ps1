$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent $PSScriptRoot
$sourceRoots = @(
    (Join-Path $repo "src\main\java"),
    (Join-Path $repo "src\test\java")
)
$buildRoot = Join-Path $repo "build\test-classes"
$sourceListFile = Join-Path $buildRoot "sources.txt"

function Assert-WithinRepo {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$Label
    )

    $pathFull = [System.IO.Path]::GetFullPath($Path)
    $rootFull = [System.IO.Path]::GetFullPath($Root).TrimEnd('\')
    if (-not ($pathFull.Equals($rootFull, [System.StringComparison]::OrdinalIgnoreCase) -or
            $pathFull.StartsWith($rootFull + '\', [System.StringComparison]::OrdinalIgnoreCase))) {
        throw "$Label must remain within $Root"
    }
}

function Remove-SafeTree {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$Label
    )

    if (Test-Path -LiteralPath $Path) {
        Assert-WithinRepo -Path $Path -Root $Root -Label $Label
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
}

function Assert-JavaToolchainPresent {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw "Required tool not found: java"
    }
    if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
        throw "Required tool not found: javac"
    }
}

Assert-JavaToolchainPresent

foreach ($sourceRoot in $sourceRoots) {
    if (-not (Test-Path -LiteralPath $sourceRoot)) {
        throw "Missing Java source root: $sourceRoot"
    }
}

Remove-SafeTree -Path $buildRoot -Root $repo -Label "Build output directory"
New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null

$sources = foreach ($sourceRoot in $sourceRoots) {
    Get-ChildItem -Path $sourceRoot -Recurse -Filter *.java | ForEach-Object { $_.FullName }
}
if (-not $sources) {
    throw "No Java sources found under configured source roots"
}

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllLines($sourceListFile, [string[]]$sources, $utf8NoBom)

try {
    & javac @(
        "-encoding",
        "UTF-8",
        "-d",
        $buildRoot,
        ('@{0}' -f $sourceListFile)
    )
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    & java @(
        "-cp",
        $buildRoot,
        "dev.daisynetwork.testing.SpecTestRunner"
    )
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Remove-SafeTree -Path $sourceListFile -Root $repo -Label "Javac source list"
}
