[CmdletBinding()]
<#
.SYNOPSIS
Runs the GraphCompose benchmarks-module JUnit tests (the diff / median tooling tests).

.DESCRIPTION
The benchmarks live in a sibling Maven module (benchmarks/pom.xml) that depends
on the published graph-compose jar plus its tests-classifier jar, so they are
not part of the main reactor and are not run by `./mvnw test -pl .`. This
wrapper runs the module's JUnit tests through the Maven wrapper.

Use -Test to run a single test class or method using Surefire's -Dtest syntax.
Use -Install when the benchmarks module cannot resolve graph-compose (or its
tests-classifier jar) from the local Maven repository yet; it installs the main
module first, exactly like scripts/run-benchmarks.ps1 does.

.EXAMPLE
PS> ./scripts/run-benchmark-tests.ps1
Runs every test in the benchmarks module.

.EXAMPLE
PS> ./scripts/run-benchmark-tests.ps1 -Test BenchmarkDiffToolTest
Runs only the comparison-engine tests.

.EXAMPLE
PS> ./scripts/run-benchmark-tests.ps1 -Install -Test 'BenchmarkDiffToolTest#rejectsUnknownReportSchema'
Installs the main artifact first, then runs a single test method.
#>
param(
    [string]$Test = "",
    [switch]$Install
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$mavenWrapper = Join-Path $repoRoot "mvnw.cmd"
$benchmarksPom = Join-Path $repoRoot "benchmarks\pom.xml"

Push-Location $repoRoot
try {
    if ($Install) {
        Write-Host "Installing main graph-compose artifact into the local Maven repo..." -ForegroundColor Cyan
        & $mavenWrapper "-B" "-ntp" "-DskipTests" "install" "-pl" "."
        if ($LASTEXITCODE -ne 0) {
            throw "Install of the main module failed with exit code $LASTEXITCODE"
        }
    }

    $mvnArgs = @("-B", "-ntp", "-f", $benchmarksPom, "test")
    if (-not [string]::IsNullOrWhiteSpace($Test)) {
        $mvnArgs += "-Dtest=$Test"
    }

    Write-Host "Running benchmark-module tests: mvnw $($mvnArgs -join ' ')" -ForegroundColor Cyan
    & $mavenWrapper @mvnArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Benchmark-module tests failed with exit code $LASTEXITCODE"
    }

    Write-Host "Benchmark-module tests passed." -ForegroundColor Green
} finally {
    Pop-Location
}
