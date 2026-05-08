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

    # 1. Project's own <version> tag (the FIRST <version> in the file,
    #    before <parent> or any dependency entries).
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
    if ($DryRun) {
        Write-Host "    [DRY RUN] $mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.support.ShowcaseSync" -ForegroundColor Yellow
        return
    }
    Push-Location $repoRoot
    try {
        & $mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.support.ShowcaseSync 2>&1 | ForEach-Object {
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

    # 1. On develop branch?
    $branch = (git rev-parse --abbrev-ref HEAD).Trim()
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

    Step 1 "Bump pom versions to $Version"
    Update-PomVersion (Join-Path $repoRoot 'pom.xml') $Version
    Update-PomVersion (Join-Path $repoRoot 'examples/pom.xml') $Version
    Update-PomVersion (Join-Path $repoRoot 'benchmarks/pom.xml') $Version

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
        Write-Host "    [DRY RUN] git add pom.xml examples/pom.xml benchmarks/pom.xml CHANGELOG.md examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java docs/examples.json" -ForegroundColor Yellow
        Write-Host "    [DRY RUN] git commit -m `"$commitMsg`"" -ForegroundColor Yellow
    } else {
        git add `
            pom.xml `
            examples/pom.xml `
            benchmarks/pom.xml `
            CHANGELOG.md `
            examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java `
            docs/examples.json
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
