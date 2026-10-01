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
      pwsh ./scripts/release-smoke/run.ps1                  # isolated, tests 2.4.1
      pwsh ./scripts/release-smoke/run.ps1 -Version 2.0.1   # test a different published version
      pwsh ./scripts/release-smoke/run.ps1 -Warm            # keep everything cached (fast dev iteration)
      pwsh ./scripts/release-smoke/run.ps1 -StagedRepo <dir>
          # before upload: resolve the GraphCompose coordinates from <dir>, a Maven
          # repository-layout directory such as the unzipped central-bundle.zip, and
          # everything else from Central. The version defaults to the single version
          # staged there. After each scenario every GraphCompose artifact of that
          # version must have come from <dir> — one resolved from anywhere else (a stale
          # cache, Central) fails the scenario, so a pass proves the staged bytes.
#>
param(
    [switch]$Warm,
    # Default version under test: the currently published release. Release smoke
    # must test PUBLISHED artifacts — never a -SNAPSHOT.
    [string]$Version = '2.4.1',
    [string]$StagedRepo = ''
)

$ErrorActionPreference = 'Continue'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $here '..\..')).Path
$mvnw = Join-Path $repoRoot 'mvnw.cmd'
$settings = Join-Path $here 'settings.xml'

$scenarios = @('s1-graph-compose', 's2-core-only', 's3-core-render-pdf', 's4-templates', 's5-testing', 's6-bundle', 's7-core-render-pptx', 's8-core-render-docx', 's9-cv-templates')
$repo = Join-Path $repoRoot 'target\release-smoke-m2\repo'
New-Item -ItemType Directory -Force -Path $repo | Out-Null

# An empty value (an unset variable) must not quietly turn staged mode off and smoke
# the published default from Central instead.
if ($PSBoundParameters.ContainsKey('StagedRepo') -and -not $StagedRepo) {
    Write-Error '-StagedRepo needs a directory (got an empty value)'
    exit 2
}

if ($StagedRepo) {
    if ($Warm) {
        # A warm cache can satisfy a coordinate without consulting the staged repo,
        # which is the one thing staged mode exists to rule out.
        Write-Error '-StagedRepo cannot be combined with -Warm'
        exit 2
    }
    $stagedGc = Join-Path $StagedRepo 'io\github\demchaav'
    if (-not (Test-Path $stagedGc -PathType Container)) {
        Write-Error "FATAL: $StagedRepo is not a Maven repository layout holding io/github/demchaav"
        exit 2
    }
    # The path goes into XML; escape the characters that would break it.
    $stagedAbs = [System.Security.SecurityElement]::Escape(((Resolve-Path $StagedRepo).Path -replace '\\', '/'))
    if (-not $PSBoundParameters.ContainsKey('Version')) {
        $staged = @(Get-ChildItem -Directory (Join-Path $stagedGc 'graph-compose-core') -ErrorAction SilentlyContinue)
        if ($staged.Count -ne 1) {
            Write-Error "FATAL: expected exactly one staged graph-compose-core version, found: $($staged.Name -join ', ')"
            exit 2
        }
        $Version = $staged[0].Name
    }
    # The isolated settings, with one exception carved out of the Central-only mirror:
    # the staged repository, active for every scenario.
    $settings = Join-Path $repoRoot 'target\release-smoke-m2\settings-staged.xml'
    $stagedSettings = @"
<settings>
  <mirrors>
    <mirror>
      <id>central-only</id>
      <url>https://repo.maven.apache.org/maven2</url>
      <mirrorOf>*,!staged</mirrorOf>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>staged</id>
      <repositories>
        <repository>
          <id>staged</id>
          <url>file:///$($stagedAbs.TrimStart('/'))</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>staged</activeProfile>
  </activeProfiles>
</settings>
"@
    # Without a BOM: Windows PowerShell's `-Encoding utf8` writes one.
    [System.IO.File]::WriteAllText($settings, $stagedSettings, (New-Object System.Text.UTF8Encoding($false)))
}

# Staged mode only. Every file of every train artifact the scenario resolved must
# record the staged repository as its source, and every train artifact must be at
# the version under test — a train module resolved at another version is lockstep
# drift in a staged POM, and it would have come from Central unchecked. fonts and
# emoji are exempt: they version independently and always come from Central. Fails
# closed — a scenario that resolved no train artifact proves nothing.
function Test-StagedProvenance {
    $seen = 0
    $ok = $true
    $artifacts = @(Get-ChildItem -Directory (Join-Path $repo 'io\github\demchaav') -ErrorAction SilentlyContinue)
    foreach ($artifact in $artifacts) {
        if ($artifact.Name -in @('graph-compose-fonts', 'graph-compose-emoji')) { continue }
        foreach ($versionDir in @(Get-ChildItem -Directory $artifact.FullName)) {
            if ($versionDir.Name -ne $Version) {
                Write-Host "PROVENANCE: $($artifact.Name) resolved at $($versionDir.Name), not the staged $Version"
                $ok = $false
                continue
            }
            $seen++
            $marker = Join-Path $versionDir.FullName '_remote.repositories'
            if (-not (Test-Path $marker)) {
                Write-Host "PROVENANCE: $($versionDir.FullName) records no source repository"
                $ok = $false
                continue
            }
            foreach ($line in Get-Content $marker) {
                if (-not $line -or $line.StartsWith('#')) { continue }
                if (-not $line.EndsWith('>staged=')) {
                    Write-Host "PROVENANCE: $($artifact.Name) ${Version}: '$line' was not resolved from the staged repository"
                    $ok = $false
                }
            }
        }
    }
    if ($seen -eq 0) {
        Write-Host "PROVENANCE: no GraphCompose $Version artifact was resolved at all"
        return $false
    }
    return $ok
}

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
    Write-Host "=== SMOKE $s   (version=$Version, repo=$repo, evicted=$(if ($Warm) { 'no' } else { 'yes' })$(if ($StagedRepo) { ", staged=$StagedRepo" }))"
    Write-Host "=================================================================="
    & $mvnw -B -ntp -s $settings "-Dgc.version=$Version" -f (Join-Path $here "$s\pom.xml") "-Dmaven.repo.local=$repo" clean verify
    $passed = $LASTEXITCODE -eq 0
    if ($passed -and $StagedRepo -and -not (Test-StagedProvenance)) { $passed = $false }
    if ($passed) {
        $results += "$s PASS"; $pass++
    } else {
        $results += "$s FAIL"; $fail++
    }
}

Write-Host ""
Write-Host "===================== RELEASE SMOKE SUMMARY ====================="
Write-Host "version-under-test: $Version$(if ($StagedRepo) { " (staged: $StagedRepo)" })"
foreach ($r in $results) { Write-Host "RESULT $r" }
Write-Host ("SUMMARY {""version"":""$Version"",""passed"":$pass,""failed"":$fail,""total"":$($pass + $fail)}")

if ($fail -ne 0) { exit 1 } else { exit 0 }
