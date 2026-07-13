<#
    Release smoke harness (PowerShell) — runs every external consumer project
    against the published GraphCompose coordinates on Maven Central. See README.md.

    Isolation model: a dedicated local repository under target/ that never receives
    an `mvn install` of GraphCompose. Before each scenario the GraphCompose
    artifacts (io\github\demchaav\**) are EVICTED, so every scenario must re-resolve
    graph-compose-* from Maven Central — proving the release resolves cleanly with
    no local reactor build behind it. Maven's own plugins and third-party libraries
    (PDFBox, JUnit, ...) are kept cached: re-downloading Maven's core plugins on an
    empty repo is heavy and flaky and tests Maven, not this release.

    Usage:
      pwsh ./scripts/release-smoke/run.ps1          # evict GraphCompose per scenario (Central-only, isolated)
      pwsh ./scripts/release-smoke/run.ps1 -Warm    # keep everything cached (fast dev iteration)
#>
param(
    [switch]$Warm
)

$ErrorActionPreference = 'Continue'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $here '..\..')).Path
$mvnw = Join-Path $repoRoot 'mvnw.cmd'

$scenarios = @('s1-graph-compose', 's2-core-only', 's3-core-render-pdf', 's4-templates', 's5-testing', 's6-bundle')
$repo = Join-Path $repoRoot 'target\release-smoke-m2\repo'
New-Item -ItemType Directory -Force -Path $repo | Out-Null

$pass = 0
$fail = 0
$results = @()

foreach ($s in $scenarios) {
    if (-not $Warm) {
        $gc = Join-Path $repo 'io\github\demchaav'
        if (Test-Path $gc) { Remove-Item -Recurse -Force $gc }
    }
    Write-Host ""
    Write-Host "=================================================================="
    Write-Host "=== SMOKE $s   (maven.repo.local=$repo, GraphCompose evicted=$(if ($Warm) { 'no' } else { 'yes' }))"
    Write-Host "=================================================================="
    & $mvnw -B -ntp -f (Join-Path $here "$s\pom.xml") "-Dmaven.repo.local=$repo" clean verify
    if ($LASTEXITCODE -eq 0) {
        $results += "$s PASS"; $pass++
    } else {
        $results += "$s FAIL"; $fail++
    }
}

Write-Host ""
Write-Host "===================== RELEASE SMOKE SUMMARY ====================="
foreach ($r in $results) { Write-Host "RESULT $r" }
Write-Host ("SUMMARY {""passed"":$pass,""failed"":$fail,""total"":$($pass + $fail)}")

if ($fail -ne 0) { exit 1 } else { exit 0 }
