<#
    Release smoke harness (PowerShell) — runs every external consumer project
    against the published GraphCompose coordinates on Maven Central. See README.md.

    Isolation model: a dedicated local repository under target/ that never receives
    an `mvn install` of GraphCompose, plus an isolated settings.xml whose mirror
    forces ALL resolution through Maven Central. Before each scenario the GraphCompose
    artifacts (io\github\demchaav\**) are EVICTED — and the harness hard-fails if the
    eviction does not take — so every scenario must re-resolve graph-compose-* from
    Central. Maven's own plugins and third-party libraries stay cached: re-downloading
    Maven's core plugins on an empty repo is heavy, flaky, and tests Maven, not this
    release.

    Usage:
      pwsh ./scripts/release-smoke/run.ps1                  # isolated, tests 2.2.2
      pwsh ./scripts/release-smoke/run.ps1 -Version 2.0.1   # test a different published version
      pwsh ./scripts/release-smoke/run.ps1 -Warm            # keep everything cached (fast dev iteration)
#>
param(
    [switch]$Warm,
    # Default version under test: the currently published release. Release smoke
    # must test PUBLISHED artifacts — never a -SNAPSHOT.
    [string]$Version = '2.2.2'
)

$ErrorActionPreference = 'Continue'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $here '..\..')).Path
$mvnw = Join-Path $repoRoot 'mvnw.cmd'
$settings = Join-Path $here 'settings.xml'

$scenarios = @('s1-graph-compose', 's2-core-only', 's3-core-render-pdf', 's4-templates', 's5-testing', 's6-bundle', 's7-core-render-pptx', 's8-core-render-docx')
$repo = Join-Path $repoRoot 'target\release-smoke-m2\repo'
New-Item -ItemType Directory -Force -Path $repo | Out-Null

$pass = 0
$fail = 0
$results = @()

foreach ($s in $scenarios) {
    if (-not $Warm) {
        # Evict only the GraphCompose coordinates, then hard-fail if it did not take.
        $gc = Join-Path $repo 'io\github\demchaav'
        if (Test-Path $gc) { Remove-Item -Recurse -Force $gc }
        if (Test-Path $gc) {
            Write-Error "FATAL: could not remove the GraphCompose cache directory: $gc"
            exit 3
        }
    }
    Write-Host ""
    Write-Host "=================================================================="
    Write-Host "=== SMOKE $s   (version=$Version, repo=$repo, evicted=$(if ($Warm) { 'no' } else { 'yes' }))"
    Write-Host "=================================================================="
    & $mvnw -B -ntp -s $settings "-Dgc.version=$Version" -f (Join-Path $here "$s\pom.xml") "-Dmaven.repo.local=$repo" clean verify
    if ($LASTEXITCODE -eq 0) {
        $results += "$s PASS"; $pass++
    } else {
        $results += "$s FAIL"; $fail++
    }
}

Write-Host ""
Write-Host "===================== RELEASE SMOKE SUMMARY ====================="
Write-Host "version-under-test: $Version"
foreach ($r in $results) { Write-Host "RESULT $r" }
Write-Host ("SUMMARY {""version"":""$Version"",""passed"":$pass,""failed"":$fail,""total"":$($pass + $fail)}")

if ($fail -ne 0) { exit 1 } else { exit 0 }
