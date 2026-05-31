<#
.SYNOPSIS
    Cuts a GraphCompose release: bumps pom versions, updates the
    showcase site links, regenerates the examples manifest, runs
    verify, commits, tags, pushes — then flips the showcase links
    back to develop so ongoing dev work stays linkable.

.DESCRIPTION
    Modes:

      Default          — full release cut. Requires clean develop in
                         sync with origin. Performs all 10 steps end
                         to end with prompts before each remote
                         action (push develop, push tag, merge to
                         main).

      -DryRun          — print every step without executing it. Use
                         to preview what the release will do.

      -SkipPush        — perform local changes (bump, tag, commit)
                         but do NOT push. Useful for staging a
                         release locally before publishing.

      -PostReleaseOnly — skip the release work entirely. Just flips
                         ShowcaseMetadata.GH_BASE back to /blob/develop,
                         re-runs ShowcaseSync, and commits +
                         pushes that change. Use after a release
                         was cut and you want ongoing develop work
                         to have linkable View Code buttons.

      -SkipVerify      — skip the mvnw verify gate. Only use when
                         you've just run verify yourself and don't
                         want to wait another minute.

.EXAMPLE
    pwsh ./scripts/cut-release.ps1 -Version 1.6.0
    # full release of v1.6.0

.EXAMPLE
    pwsh ./scripts/cut-release.ps1 -Version 1.6.0 -DryRun
    # preview what would happen

.EXAMPLE
    pwsh ./scripts/cut-release.ps1 -PostReleaseOnly
    # post-release: flip showcase links back to develop

.NOTES
    Author: Artem Demchyshyn
    Pre-conditions:
      - on develop branch
      - working tree clean
      - develop in sync with origin/develop
      - tag v$Version doesn't already exist

    Post-release reminder: after pushing the tag, merge develop into
    main so GitHub Pages picks up the new docs, then run this script
    with -PostReleaseOnly to flip the showcase links back to develop
    for ongoing v1.x.y dev work.
#>

[CmdletBinding(DefaultParameterSetName='Release')]
param(
    [Parameter(Mandatory=$true, ParameterSetName='Release')]
    [string]$Version,

    [Parameter(ParameterSetName='Release')]
    [switch]$DryRun,

    [Parameter(ParameterSetName='Release')]
    [switch]$SkipPush,

    [Parameter(ParameterSetName='Release')]
    [switch]$SkipVerify,

    [Parameter(Mandatory=$true, ParameterSetName='PostRelease')]
    [switch]$PostReleaseOnly
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path "$PSScriptRoot/..").Path
$showcaseMetadata = Join-Path $repoRoot 'examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java'
$mvnw = Join-Path $repoRoot 'mvnw.cmd'

function Step($n, $title) {
    Write-Host ""
    Write-Host "[$n] $title" -ForegroundColor Cyan
}

function Note($message) {
    Write-Host "    $message" -ForegroundColor DarkGray
}

function Run($command) {
    if ($DryRun) {
        Write-Host "    [DRY RUN] $command" -ForegroundColor Yellow
    } else {
        Write-Host "    > $command" -ForegroundColor DarkGray
        Invoke-Expression $command
        if ($LASTEXITCODE -ne 0) {
            throw "Command failed (exit $LASTEXITCODE): $command"
        }
    }
}

function Update-PomVersion($pomPath, $newVersion) {
    if (-not (Test-Path $pomPath)) {
        Note "skip (no file): $pomPath"
        return
    }
    $content = Get-Content $pomPath -Raw
    $changed = $false

    # 1. The FIRST <version> in the file. For the library root and the
    #    aggregator this is the project's own <version>; for the reactor
    #    children (examples/, benchmarks/) it is the inherited <parent>
    #    <version>, which must track the aggregator. Either way it sits
    #    before any dependency entries, so a single-shot replace is safe.
    $projectRegex = [regex]'<version>[\w\.\-]+</version>'
    $projectNew = "<version>$newVersion</version>"
    $afterProject = $projectRegex.Replace($content, $projectNew, 1)
    if ($content -ne $afterProject) {
        $content = $afterProject
        $changed = $true
        Note "bumped <version>: $pomPath -> $projectNew"
    }

    # 2. <graphcompose.version> property (if present). Subordinate POMs
    #    (examples/, benchmarks/) declare this property and depend on
    #    "io.github.demchaav:graphcompose:${graphcompose.version}". The
    #    property must track the project version so the published tag
    #    actually resolves on a fresh CI agent without a populated
    #    local m2. Bug surfaced in v1.6.0 release CI: the property
    #    stayed at 1.6.0-beta.1 while the project version flipped to
    #    1.6.0 and CI failed at "Could not find artifact ...:1.6.0-beta.1".
    $propertyRegex = [regex]'<graphcompose\.version>[\w\.\-]+</graphcompose\.version>'
    $propertyNew = "<graphcompose.version>$newVersion</graphcompose.version>"
    $afterProperty = $propertyRegex.Replace($content, $propertyNew, 1)
    if ($content -ne $afterProperty) {
        $content = $afterProperty
        $changed = $true
        Note "bumped <graphcompose.version>: $pomPath -> $propertyNew"
    }

    if (-not $changed) {
        Note "no change: $pomPath (version already $newVersion?)"
        return
    }

    if ($DryRun) {
        Write-Host "    [DRY RUN] Bump $pomPath -> $newVersion" -ForegroundColor Yellow
    } else {
        [System.IO.File]::WriteAllText($pomPath, $content)
    }
}

function Update-ReadmeInstallVersion($readmePath, $newVersion) {
    if (-not (Test-Path $readmePath)) {
        Note "skip (no file): $readmePath"
        return
    }
    $content = Get-Content $readmePath -Raw
    $tag = "v$newVersion"
    $changed = $false

    # The README Maven Central install snippets pin the published version
    # (X.Y.Z, no `v` prefix). They must flip in the SAME commit the release
    # tag is cut from, so a new user who copy-pastes the README resolves
    # the version this release actually publishes (Phase 2.3 of the
    # release skill: README version flips at release-execution time,
    # never earlier). Two snippets carry it:
    #   Maven:  <artifactId>graphcompose</artifactId><version>X.Y.Z</version>
    #   Gradle: implementation("io.github.demchaav:graphcompose:X.Y.Z")
    # Lookbehind/lookahead so only the version token is rewritten. A
    # secondary fallback handles the legacy JitPack format
    # (<artifactId>GraphCompose</artifactId> / GraphCompose:vX.Y.Z) so
    # the script still works if a future change re-introduces a JitPack
    # snippet for documentation purposes.
    $mavenCentralRegex = [regex]'(?<=<artifactId>graphcompose</artifactId>\s*<version>)v?[\w\.\-]+(?=</version>)'
    $afterMaven = $mavenCentralRegex.Replace($content, $newVersion, 1)
    if ($content -ne $afterMaven) {
        $content = $afterMaven
        $changed = $true
        Note "bumped README Maven Central snippet -> $newVersion"
    } else {
        $mavenLegacyRegex = [regex]'(?<=<artifactId>GraphCompose</artifactId>\s*<version>)v?[\w\.\-]+(?=</version>)'
        $afterMavenLegacy = $mavenLegacyRegex.Replace($content, $tag, 1)
        if ($content -ne $afterMavenLegacy) {
            $content = $afterMavenLegacy
            $changed = $true
            Note "bumped README legacy JitPack Maven snippet -> $tag"
        }
    }

    $gradleCentralRegex = [regex]'(?<=io\.github\.demchaav:graphcompose:)v?[\w\.\-]+(?=")'
    $afterGradle = $gradleCentralRegex.Replace($content, $newVersion, 1)
    if ($content -ne $afterGradle) {
        $content = $afterGradle
        $changed = $true
        Note "bumped README Maven Central Gradle snippet -> $newVersion"
    } else {
        $gradleLegacyRegex = [regex]'(?<=:GraphCompose:)v?[\w\.\-]+(?=")'
        $afterGradleLegacy = $gradleLegacyRegex.Replace($content, $tag, 1)
        if ($content -ne $afterGradleLegacy) {
            $content = $afterGradleLegacy
            $changed = $true
            Note "bumped README legacy JitPack Gradle snippet -> $tag"
        }
    }

    if (-not $changed) {
        Note "no change: README install snippets (already $tag?)"
        return
    }

    if ($DryRun) {
        Write-Host "    [DRY RUN] README install snippets -> $tag" -ForegroundColor Yellow
    } else {
        [System.IO.File]::WriteAllText($readmePath, $content)
    }
}

function Update-IndexHtmlVersion($indexHtmlPath, $newVersion) {
    if (-not (Test-Path $indexHtmlPath)) {
        Note "skip (no file): $indexHtmlPath"
        return
    }
    $content = Get-Content $indexHtmlPath -Raw
    $tag = "v$newVersion"
    $changed = $false

    # The GitHub Pages showcase (docs/index.html) hardcodes the version in
    # several spots that do NOT inherit from the pom — they previously sat at
    # v1.6.1 while the library shipped v1.6.4. VersionConsistencyGuardTest
    # fails the verify gate if any lags, so flip them all in lockstep with the
    # README + poms. The Maven Central format coordinates use bare semver
    # ($newVersion), the hero badge keeps the v-prefix ($tag), and the
    # downloadUrl points at the Central artefact page. Lookbehind/lookahead
    # so only the version token is rewritten.
    $replacements = @(
        @{ Regex = [regex]'(?<="softwareVersion": ")v?[\w\.\-]+(?=")';                                                     Value = $newVersion; Label = 'JSON-LD softwareVersion' },
        @{ Regex = [regex]'(?<=https://central\.sonatype\.com/artifact/io\.github\.demchaav/graphcompose/)v?[\w\.\-]+(?=")'; Value = $newVersion; Label = 'Central downloadUrl' },
        @{ Regex = [regex]'(?<=Java &middot; )v?[\w\.\-]+(?= &middot; MIT)';                                                Value = $tag;        Label = 'hero badge' },
        @{ Regex = [regex]'(?<=&lt;artifactId&gt;graphcompose&lt;/artifactId&gt;\s*&lt;version&gt;)v?[\w\.\-]+(?=&lt;/version&gt;)'; Value = $newVersion; Label = 'Maven Central snippet' },
        @{ Regex = [regex]"(?<=io\.github\.demchaav:graphcompose:)v?[\w\.\-]+(?=')";                                        Value = $newVersion; Label = 'Gradle Central snippet' }
    )

    foreach ($r in $replacements) {
        $after = $r.Regex.Replace($content, $r.Value, 1)
        if ($content -ne $after) {
            $content = $after
            $changed = $true
            Note "bumped index.html $($r.Label) -> $($r.Value)"
        }
    }

    if (-not $changed) {
        Note "no change: docs/index.html version (already $tag?)"
        return
    }

    if ($DryRun) {
        Write-Host "    [DRY RUN] docs/index.html version -> $tag" -ForegroundColor Yellow
    } else {
        [System.IO.File]::WriteAllText($indexHtmlPath, $content)
    }
}

function Update-ShowcaseGhBase($newRef) {
    if (-not (Test-Path $showcaseMetadata)) {
        throw "ShowcaseMetadata.java not found: $showcaseMetadata"
    }
    $content = Get-Content $showcaseMetadata -Raw
    $regex = [regex]'private static final String GH_BASE = "https://github\.com/DemchaAV/GraphCompose/blob/[^"]+";'
    $newLine = "private static final String GH_BASE = `"https://github.com/DemchaAV/GraphCompose/blob/$newRef`";"
    $newContent = $regex.Replace($content, $newLine, 1)
    if ($content -eq $newContent) {
        Note "no change: ShowcaseMetadata GH_BASE (already $newRef)"
        return $false
    }
    if ($DryRun) {
        Write-Host "    [DRY RUN] ShowcaseMetadata GH_BASE -> /blob/$newRef" -ForegroundColor Yellow
    } else {
        [System.IO.File]::WriteAllText($showcaseMetadata, $newContent)
        Note "GH_BASE -> /blob/$newRef"
    }
    return $true
}

function Run-ShowcaseSync {
    # Quote the -D argument: PowerShell's call operator drops the leading
    # '-D' on the way to mvnw.cmd, so Maven sees ".mainClass=..." as a
    # lifecycle phase. Wrapping the whole token in quotes preserves it
    # as a single literal argument.
    $execProp = '"-Dexec.mainClass=com.demcha.examples.support.ShowcaseSync"'
    if ($DryRun) {
        Write-Host "    [DRY RUN] $mvnw -B -ntp -DskipTests install -pl ." -ForegroundColor Yellow
        Write-Host "    [DRY RUN] $mvnw -f examples/pom.xml exec:java $execProp" -ForegroundColor Yellow
        return
    }
    Push-Location $repoRoot
    try {
        # ShowcaseSync runs from the examples module, which depends on
        # io.github.demchaav:graphcompose:${project.version}. After Step 1
        # bumps the four pom.xml files to the new release version, that
        # artifact is not yet in the local m2 cache — only the previous
        # release is — so exec:java fails dependency resolution with
        # "Could not find artifact ...:graphcompose:jar:<new-version>".
        # Install the root artifact first so the examples module can
        # resolve it. Bug surfaced during v1.6.5 cut: Step 4 aborted with
        # exit 1; we had to install by hand and resume manually.
        Write-Host "    > $mvnw -B -ntp -DskipTests install -pl ." -ForegroundColor DarkGray
        & $mvnw -B -ntp -DskipTests install -pl . 2>&1 | ForEach-Object {
            if ($_ -match 'BUILD SUCCESS|BUILD FAILURE|ERROR') {
                Write-Host "    $_" -ForegroundColor DarkGray
            }
        }
        if ($LASTEXITCODE -ne 0) {
            throw "Install root artifact failed (exit $LASTEXITCODE)"
        }
        & $mvnw -f examples/pom.xml exec:java $execProp 2>&1 | ForEach-Object {
            if ($_ -match 'Synced|Wrote manifest|BUILD SUCCESS|BUILD FAILURE|ERROR') {
                Write-Host "    $_" -ForegroundColor DarkGray
            }
        }
        if ($LASTEXITCODE -ne 0) {
            throw "ShowcaseSync failed (exit $LASTEXITCODE)"
        }
    } finally {
        Pop-Location
    }
}

# ============================================================
# Mode: -PostReleaseOnly
# ============================================================
if ($PostReleaseOnly) {
    Push-Location $repoRoot
    try {
        Step 1 "Switch ShowcaseMetadata GH_BASE back to /blob/develop"
        $changed = Update-ShowcaseGhBase 'develop'

        if ($changed -or $DryRun) {
            Step 2 "Regenerate docs/examples.json with develop links"
            Run-ShowcaseSync

            Step 3 "Commit"
            $msg = "post-release: flip showcase links back to /blob/develop"
            if ($DryRun) {
                Write-Host "    [DRY RUN] git commit -m `"$msg`"" -ForegroundColor Yellow
            } else {
                git add $showcaseMetadata 'docs/examples.json'
                git commit -m $msg
            }

            Step 4 "Push develop"
            if ($DryRun) {
                Write-Host "    [DRY RUN] git push origin develop" -ForegroundColor Yellow
            } else {
                git push origin develop
            }
        } else {
            Note "GH_BASE already points to develop. Nothing to do."
        }
    } finally {
        Pop-Location
    }
    Write-Host ""
    Write-Host "Done. Ongoing develop work has linkable View Code buttons again." -ForegroundColor Green
    return
}

# ============================================================
# Mode: full release cut
# ============================================================
Push-Location $repoRoot
try {
    $tag = "v$Version"

    Step 0 "Pre-flight checks"

    # In -DryRun mode the script never mutates anything, so the branch /
    # working-tree / origin-sync gates are relaxed: a maintainer can preview
    # what a release cut would do from a feature branch (e.g. while iterating
    # on the script itself) without having to switch to develop and back.
    # Live cuts still fail these gates loudly.
    $branch = (git rev-parse --abbrev-ref HEAD).Trim()
    if ($DryRun) {
        Note "branch: $branch (gate relaxed for -DryRun)"
    } else {
        # 1. On develop branch?
        if ($branch -ne 'develop') {
            throw "Not on develop branch (currently on $branch). Switch to develop first."
        }
        Note "branch: develop OK"

        # 2. Working tree clean?
        $status = git status --porcelain
        if ($status) {
            throw "Working tree has uncommitted changes. Commit or stash first."
        }
        Note "working tree: clean OK"

        # 3. In sync with origin?
        git fetch origin develop --quiet
        $local = (git rev-parse develop).Trim()
        $remote = (git rev-parse origin/develop).Trim()
        if ($local -ne $remote) {
            throw "Local develop ($local) is not in sync with origin/develop ($remote). Pull/push first."
        }
        Note "in sync with origin/develop OK"
    }

    # 4. Tag doesn't already exist?
    $existingTag = git tag -l $tag
    if ($existingTag) {
        throw "Tag $tag already exists. Bump version or delete the tag."
    }
    git fetch origin "refs/tags/$tag`:refs/tags/$tag" 2>&1 | Out-Null
    $existingTag = git tag -l $tag
    if ($existingTag) {
        throw "Tag $tag exists on origin. Bump version or delete the remote tag."
    }
    Note ("tag {0}: available OK" -f $tag)

    Step 1 "Bump versions to $Version (poms + README install snippets)"
    # All four version sites must move together or VersionConsistencyGuardTest
    # fails the verify gate below: the standalone library pom.xml (the published
    # JitPack artifact), the reactor aggregator, and the examples/benchmarks
    # children whose inherited <parent> version tracks the aggregator.
    Update-PomVersion (Join-Path $repoRoot 'pom.xml') $Version
    Update-PomVersion (Join-Path $repoRoot 'aggregator/pom.xml') $Version
    Update-PomVersion (Join-Path $repoRoot 'examples/pom.xml') $Version
    Update-PomVersion (Join-Path $repoRoot 'benchmarks/pom.xml') $Version
    Update-ReadmeInstallVersion (Join-Path $repoRoot 'README.md') $Version
    Update-IndexHtmlVersion (Join-Path $repoRoot 'docs/index.html') $Version

    Step 2 "Update CHANGELOG date for v$Version"
    $changelog = Join-Path $repoRoot 'CHANGELOG.md'
    if (Test-Path $changelog) {
        $today = (Get-Date -Format 'yyyy-MM-dd')
        $content = Get-Content $changelog -Raw
        $regex = [regex]"## v$([regex]::Escape($Version)) — Planned"
        $newHeader = "## v$Version — $today"
        $newContent = $regex.Replace($content, $newHeader, 1)
        if ($content -ne $newContent) {
            if ($DryRun) {
                Write-Host "    [DRY RUN] CHANGELOG.md: 'v$Version — Planned' -> 'v$Version — $today'" -ForegroundColor Yellow
            } else {
                [System.IO.File]::WriteAllText($changelog, $newContent)
                Note "CHANGELOG: v$Version — $today"
            }
        } else {
            Note "CHANGELOG: no '## v$Version — Planned' header found. Skipping (already dated?)."
        }
    }

    Step 3 "Switch ShowcaseMetadata GH_BASE to /blob/$tag"
    Update-ShowcaseGhBase $tag | Out-Null

    Step 4 "Regenerate docs/examples.json with $tag links"
    Run-ShowcaseSync

    if (-not $SkipVerify) {
        Step 5 "Run mvnw verify (sanity check)"
        if ($DryRun) {
            Write-Host "    [DRY RUN] $mvnw verify -pl ." -ForegroundColor Yellow
        } else {
            & $mvnw verify -pl . 2>&1 | ForEach-Object {
                if ($_ -match 'Tests run:|BUILD SUCCESS|BUILD FAILURE|ERROR') {
                    Write-Host "    $_" -ForegroundColor DarkGray
                }
            }
            if ($LASTEXITCODE -ne 0) {
                throw "mvnw verify failed."
            }
            Note "mvnw verify: green"
        }
    } else {
        Step 5 "Skipped mvnw verify (-SkipVerify)"
    }

    Step 6 "Commit release"
    $commitMsg = "Release v$Version"
    if ($DryRun) {
        Write-Host "    [DRY RUN] git add pom.xml aggregator/pom.xml examples/pom.xml benchmarks/pom.xml README.md CHANGELOG.md examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java docs/examples.json docs/index.html" -ForegroundColor Yellow
        Write-Host "    [DRY RUN] git commit -m `"$commitMsg`"" -ForegroundColor Yellow
    } else {
        git add `
            pom.xml `
            aggregator/pom.xml `
            examples/pom.xml `
            benchmarks/pom.xml `
            README.md `
            CHANGELOG.md `
            examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java `
            docs/examples.json `
            docs/index.html
        git commit -m $commitMsg
        Note "commit: $commitMsg"
    }

    Step 7 "Tag $tag"
    if ($DryRun) {
        Write-Host "    [DRY RUN] git tag -a $tag -m `"Release $tag`"" -ForegroundColor Yellow
    } else {
        git tag -a $tag -m "Release $tag"
        Note "tag: $tag"
    }

    if ($SkipPush) {
        Step 8 "Skipped push (-SkipPush). Run manually:"
        Write-Host "      git push origin develop" -ForegroundColor Cyan
        Write-Host "      git push origin $tag" -ForegroundColor Cyan
    } else {
        Step 8 "Push develop and tag"
        if ($DryRun) {
            Write-Host "    [DRY RUN] git push origin develop" -ForegroundColor Yellow
            Write-Host "    [DRY RUN] git push origin $tag" -ForegroundColor Yellow
        } else {
            git push origin develop
            git push origin $tag
            Note "pushed: develop + $tag"
        }
    }

    Write-Host ""
    Write-Host "Release $tag committed locally." -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps (manual):" -ForegroundColor Cyan
    Write-Host "  1. Merge develop into main on GitHub (PR or fast-forward)." -ForegroundColor Cyan
    Write-Host "     This makes the deployed GitHub Pages site pick up $tag." -ForegroundColor Cyan
    Write-Host "  2. Create a GitHub Release for $tag with the CHANGELOG section as body." -ForegroundColor Cyan
    Write-Host "  3. Verify JitPack: https://jitpack.io/com/github/DemchaAV/GraphCompose/$tag/build.log" -ForegroundColor Cyan
    Write-Host "  4. Flip showcase links back to develop:" -ForegroundColor Cyan
    Write-Host "       pwsh ./scripts/cut-release.ps1 -PostReleaseOnly" -ForegroundColor Cyan
} finally {
    Pop-Location
}
