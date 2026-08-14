<#
.SYNOPSIS
    Cuts a GraphCompose release: bumps pom versions, updates the
    showcase site links, regenerates the examples manifest, runs
    verify, commits, tags, pushes — then flips the showcase links
    back to the release branch so ongoing dev work stays linkable.

.DESCRIPTION
    Modes:

      Default          — full release cut. Requires the release branch
                         (-Branch, default develop) clean and in sync
                         with origin. Performs all 10 steps end to end
                         with prompts before each remote action (push
                         the branch, push tag, merge to main).

      -DryRun          — print every step without executing it. Use
                         to preview what the release will do.

      -SkipPush        — perform local changes (bump, tag, commit)
                         but do NOT push. Useful for staging a
                         release locally before publishing.

      -PostReleaseOnly — skip the release work entirely; open the next
                         development line. Bumps every train pom to the next
                         patch -SNAPSHOT (leaving the README/showcase install
                         snippets on the just-published release), runs
                         `mvnw validate` + VersionConsistencyGuardTest to
                         validate the bump, flips ShowcaseMetadata.GH_BASE back
                         to /blob/<branch>, re-runs ShowcaseSync, then commits +
                         pushes. Runs the same branch / clean-tree / origin-sync
                         preflight as a real cut. Idempotent: skips the bump when
                         the poms are already a -SNAPSHOT.

      -SkipVerify      — skip the mvnw verify gate. Only use when
                         you've just run verify yourself and don't
                         want to wait another minute.

      -SkipShowcase    — skip the showcase steps (GH_BASE flip +
                         ShowcaseSync regen) and leave the showcase
                         out of the release commit. Use for a pure
                         code release where no example render changed,
                         so you don't need fresh generated PDFs. The
                         version bump still updates web/index.html.

.EXAMPLE
    pwsh ./scripts/cut-release.ps1 -Version 1.6.0
    # full release of v1.6.0

.EXAMPLE
    pwsh ./scripts/cut-release.ps1 -Version 1.6.0 -DryRun
    # preview what would happen

.EXAMPLE
    pwsh ./scripts/cut-release.ps1 -Version 2.0.0-rc.1 -Branch 2.0-dev -DryRun
    # preview the 2.0 release-candidate cut from 2.0-dev

.EXAMPLE
    pwsh ./scripts/cut-release.ps1 -PostReleaseOnly -Branch 2.0-dev
    # post-release: flip showcase links back to /blob/2.0-dev

.NOTES
    Author: Artem Demchyshyn
    Pre-conditions:
      - on the release branch (-Branch, default develop)
      - working tree clean
      - the release branch in sync with its origin
      - tag v$Version doesn't already exist

    Post-release reminder: after pushing the tag, merge the release
    branch into main so GitHub Pages picks up the new docs, then run
    this script with -PostReleaseOnly -Branch <branch> to open the next
    -SNAPSHOT development line and flip the showcase links back.
#>

[CmdletBinding(DefaultParameterSetName='Release')]
param(
    # The release branch to cut from / push to. Defaults to `develop` (the 1.9.x
    # line); pass `-Branch 2.0-dev` to cut the 2.0 line. Common to both modes.
    [string]$Branch = 'develop',

    [Parameter(Mandatory=$true, ParameterSetName='Release')]
    [string]$Version,

    # Common to both modes so -PostReleaseOnly can also be previewed.
    [switch]$DryRun,

    [Parameter(ParameterSetName='Release')]
    [switch]$SkipPush,

    [Parameter(ParameterSetName='Release')]
    [switch]$SkipVerify,

    [Parameter(ParameterSetName='Release')]
    [switch]$SkipShowcase,

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

function Assert-BranchPreflight($branch) {
    # Shared safety gate for BOTH a full release cut and -PostReleaseOnly: the current
    # branch is the target branch, the working tree is clean, and the local branch is
    # in sync with origin. In -DryRun the gate is relaxed so the flow can be previewed
    # from any branch (e.g. while iterating on this script); live runs fail loudly.
    $currentBranch = (git rev-parse --abbrev-ref HEAD).Trim()
    if ($DryRun) {
        Note "branch: $currentBranch (gate relaxed for -DryRun)"
        return
    }

    # 1. On the target branch?
    if ($currentBranch -ne $branch) {
        throw "Not on $branch branch (currently on $currentBranch). Switch to $branch first."
    }
    Note "branch: $branch OK"

    # 2. Working tree clean? `git status --porcelain` reports STAGED (column 1),
    #    unstaged (column 2), and untracked entries — any output means not clean.
    if (git status --porcelain) {
        throw "Working tree has uncommitted or staged changes. Commit or stash first."
    }
    Note "working tree: clean (incl. staged) OK"

    # 3. Local branch in sync with origin? A silently-failed fetch (network / auth)
    #    would compare against a STALE origin ref and wrongly report "in sync", so
    #    check every git exit code before trusting the comparison.
    git fetch origin $branch --quiet
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to fetch origin/$branch. Cannot verify branch synchronization."
    }
    # Capture, THEN check the exit code, THEN .Trim(): calling .Trim() on a failed
    # rev-parse's $null output would throw a confusing null-method error and skip the
    # message below.
    $local = git rev-parse $branch
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to resolve local $branch (git rev-parse)."
    }
    $remote = git rev-parse "origin/$branch"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to resolve origin/$branch (git rev-parse)."
    }
    if ($local.Trim() -ne $remote.Trim()) {
        throw "Local $branch ($local) is not in sync with origin/$branch ($remote). Pull/push first."
    }
    Note "in sync with origin/$branch OK"
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

function Update-AssetVersion($pomPath, $newVersion) {
    # Moves <graphcompose.examples.assetVersion> — the version the committed previews
    # under assets/readme were rendered at. CommittedAssetDriftTest renders at it to
    # compare like with like, and ExampleVersion accepts a released X.Y.Z and nothing
    # else, so this is deliberately NOT part of Update-PomVersion:
    #
    #   * -PostReleaseOnly bumps the train to X.Y.(Z+1)-SNAPSHOT. Carrying the property
    #     along would hand surefire a -SNAPSHOT display version, and the examples module
    #     would throw before comparing a single preview.
    #   * a pre-release cut sets X.Y.Z-rc.N, which the same check rejects — and whose
    #     qualifier-stripped form names a final version that does not exist yet, so the
    #     previews would advertise an unpublished release.
    #
    # Only a final cut moves it, in the same commit as the tag, next to the step that
    # re-renders the previews at that version.
    #
    # Independent of Update-PomVersion's early return as well: a cut interrupted after
    # the version bump leaves the poms on the new version and this property behind, and
    # re-running has to be able to finish the job rather than report nothing to do.
    $content = [System.IO.File]::ReadAllText($pomPath)
    $assetRegex = [regex]'<graphcompose\.examples\.assetVersion>[\w\.\-]+</graphcompose\.examples\.assetVersion>'
    $match = $assetRegex.Match($content)
    if (-not $match.Success) {
        Note "no <graphcompose.examples.assetVersion> in $pomPath — nothing to move"
        return
    }
    if ($match.Value -eq "<graphcompose.examples.assetVersion>$newVersion</graphcompose.examples.assetVersion>") {
        Note "asset version already $newVersion"
        return
    }
    if ($DryRun) {
        Write-Host "    [DRY RUN] asset version -> $newVersion" -ForegroundColor Yellow
        return
    }
    $content = $assetRegex.Replace($content,
        "<graphcompose.examples.assetVersion>$newVersion</graphcompose.examples.assetVersion>", 1)
    [System.IO.File]::WriteAllText($pomPath, $content)
    Note "asset version -> $newVersion"
}

function Get-NextSnapshotVersion($version) {
    # A final release X.Y.Z opens the next patch development line X.Y.(Z+1)-SNAPSHOT.
    # Pre-release versions (rc / beta / alpha) stay on their own cycle, so return
    # $null and let the caller skip the post-release SNAPSHOT bump for them.
    if ($version -match '^(\d+)\.(\d+)\.(\d+)$') {
        return "$($Matches[1]).$($Matches[2]).$([int]$Matches[3] + 1)-SNAPSHOT"
    }
    return $null
}

function Test-ReadmeLatestStable($version) {
    # True when the README 'Latest stable' prose block names $version. Used to verify
    # the block AFTER Step 1 rewrote it — see Update-ReadmeReleaseStatus.
    $readme = Get-Content (Join-Path $repoRoot 'README.md') -Raw
    return $readme -match "\*\*Latest stable\*\*:\s*\[v$([regex]::Escape($version))\]"
}

function Test-RoadmapCurrentStable($version) {
    # True when ROADMAP's 'Current stable' section names $version in bold. Used to
    # verify the section AFTER Step 1 rewrote it — see Update-RoadmapCurrentStable.
    $roadmap = Get-Content (Join-Path $repoRoot 'ROADMAP.md') -Raw
    $section = Get-RoadmapCurrentStableSection $roadmap
    if (-not $section) { return $false }
    return $section -match "\*\*$([regex]::Escape($version))\*\*"
}

function Get-RoadmapCurrentStableSection($content) {
    # The '## Current stable' section body, up to the next '## ' heading. Everything
    # below that heading is per-release history and must not be touched.
    $start = $content.IndexOf('## Current stable')
    if ($start -lt 0) { return $null }
    $next = $content.IndexOf("`n## ", $start + 1)
    if ($next -lt 0) { return $content.Substring($start) }
    return $content.Substring($start, $next - $start)
}

function Update-RoadmapCurrentStable($roadmapPath, $newVersion) {
    # ROADMAP's 'Current stable' section names the live release, and
    # VersionConsistencyGuardTest holds it to a published version. Step 5 runs
    # `mvnw clean verify` AFTER the poms are already at the final version, so a cut that
    # left this section behind would fail its own gate: the guard would read the pom's
    # X.Y.Z and find the previous release still named here. The README block is rewritten
    # for exactly this reason (Update-ReadmeReleaseStatus) — this is the same contract for
    # the other page that makes a claim about what is live.
    #
    # FINAL releases only, like the README block: a pre-release never reaches Maven
    # Central, so the section keeps naming the last stable version a reader can resolve.
    #
    # Only the bolded version token in that section is rewritten; the maintainer's prose,
    # and every per-release section below it, are left alone.
    if (-not (Test-Path $roadmapPath)) {
        Note "skip (no file): $roadmapPath"
        return
    }
    $content = Get-Content $roadmapPath -Raw
    $section = Get-RoadmapCurrentStableSection $content
    if (-not $section) {
        throw "ROADMAP.md has no '## Current stable' section to update — VersionConsistencyGuardTest requires one, so Step 5 would fail."
    }
    $updated = [regex]::Replace($section, '\*\*\d+\.\d+\.\d+\*\*', "**$newVersion**", 1)
    if ($updated -eq $section) {
        throw "ROADMAP.md '## Current stable' section names no bolded X.Y.Z version to update — VersionConsistencyGuardTest would fail at Step 5."
    }
    if ($DryRun) {
        Write-Host "    [DRY RUN] ROADMAP.md 'Current stable' -> $newVersion" -ForegroundColor Yellow
        return
    }
    Set-Content -Path $roadmapPath -Value $content.Replace($section, $updated) -NoNewline
    Note "ROADMAP.md 'Current stable' -> $newVersion"
}

function Update-ReadmeReleaseStatus($readmePath, $newVersion) {
    # The README 'Release status' blockquote carries two halves:
    #
    #   > 🟢 **Latest stable**: [vX.Y.Z](…/releases/tag/vX.Y.Z) — …
    #   >  · 🟡 **In development**: vX.Y.(Z+1) on `develop` — …
    #
    # It used to be a maintainer pre-cut hand-edit, gated in Step 0. That forced a
    # false state: to leave `main` correct after the tag, `develop` had to advertise
    # an unpublished version as "latest stable" for the whole cycle, with a release
    # link that 404s. The script owns the block now — Step 1 promotes the in-development
    # half to latest-stable and opens the next patch line — so `develop` stays truthful
    # between releases and the release commit still carries the right text to `main`.
    # Only the version tokens are rewritten; the maintainer's prose is left alone.
    if (-not (Test-Path $readmePath)) {
        Note "skip (no file): $readmePath"
        return
    }
    $content = Get-Content $readmePath -Raw
    $next = Get-NextSnapshotVersion $newVersion
    $nextVersion = if ($next) { $next -replace '-SNAPSHOT$', '' } else { $null }

    $stable = [regex]'(?<=\*\*Latest stable\*\*:\s*\[v)[\w\.\-]+(?=\])'
    $stableUrl = [regex]'(?<=/releases/tag/v)[\w\.\-]+(?=\))'
    $inDev = [regex]'(?<=\*\*In development\*\*:\s*v)[\w\.\-]+'

    $updated = $stable.Replace($content, $newVersion, 1)
    $updated = $stableUrl.Replace($updated, $newVersion, 1)
    if ($nextVersion) {
        $updated = $inDev.Replace($updated, $nextVersion, 1)
    }

    if ($updated -eq $content) {
        Note "README release-status block already names v$newVersion"
        return
    }
    if ($DryRun) {
        Note "[DRY RUN] README release status -> latest stable v$newVersion, in development v$nextVersion"
        return
    }
    [System.IO.File]::WriteAllText($readmePath, $updated)
    Note "README release status -> latest stable v$newVersion, in development v$nextVersion"
}

function Assert-ReleaseMetadata($version, $isFinalRelease) {
    # Fast, build-free validation of the metadata cut-release itself rewrote in Steps 1-2.
    # Runs at Step 2b — after those mutations, before commit/tag; VersionConsistencyGuardTest
    # re-checks it in the verify gate. The maintainer-authored README 'Latest stable' block
    # is validated earlier, in Step 0 (Test-ReadmeLatestStable). The CHANGELOG-date and
    # install-snippet checks are FINAL-release only: a pre-release carries no dated CHANGELOG
    # entry (it is cut against the upcoming final's notes) and deliberately keeps the Central
    # snippets on the last stable version. The pom-version check applies to both — the poms
    # move to the (pre-)release version either way.
    $problems = @()

    if ($isFinalRelease) {
        # 1. CHANGELOG carries a DATED entry for this version (Step 2 flips Planned -> dated).
        $changelog = Get-Content (Join-Path $repoRoot 'CHANGELOG.md') -Raw
        if ($changelog -notmatch "(?m)^## v$([regex]::Escape($version))\s+[—-]\s+\d{4}-\d{2}-\d{2}\b") {
            $problems += "CHANGELOG.md has no dated '## v$version - YYYY-MM-DD' entry (still 'Planned', or missing)."
        }

        # 2. README Maven install snippet advertises this version (Step 1 rewrites it; the
        #    full per-module + Gradle + showcase check is VersionConsistencyGuardTest in the
        #    verify gate — this is the fast representative check).
        $readme = Get-Content (Join-Path $repoRoot 'README.md') -Raw
        $snippet = [regex]::Match($readme, '<artifactId>graph-compose</artifactId>\s*<version>v?([\w\.\-]+)</version>')
        if (-not $snippet.Success) {
            $problems += "README.md has no graph-compose Maven install snippet (<artifactId>graph-compose</artifactId> ... <version>...</version>)."
        } elseif ($snippet.Groups[1].Value -ne $version) {
            $problems += "README.md install snippet is $($snippet.Groups[1].Value), expected $version."
        }

        # 2b. README release-status block names this version as latest stable (Step 1
        #     rewrites it; this is the post-mutation verification that replaced the old
        #     pre-cut hand-edit gate).
        if (-not (Test-ReadmeLatestStable $version)) {
            $problems += "README.md release-status block does not name v$version as 'Latest stable'."
        }

        # 2c. ROADMAP's 'Current stable' section names this version (Step 1 rewrites it).
        #     Held here for the same reason as 2b: VersionConsistencyGuardTest reads it
        #     during the Step 5 verify gate, so a section left behind fails the cut after
        #     the poms have already moved.
        if (-not (Test-RoadmapCurrentStable $version)) {
            $problems += "ROADMAP.md 'Current stable' section does not name $version."
        }
    }

    # 3. Every train pom's own <version> is this version (belt-and-suspenders ahead
    #    of the verify-gate guard; fonts/emoji version independently and are skipped).
    foreach ($pom in @('core/pom.xml', 'pom.xml', 'render-pdf/pom.xml', 'render-docx/pom.xml',
            'render-pptx/pom.xml', 'templates/pom.xml', 'testing/pom.xml', 'wrapper/pom.xml', 'bundle/pom.xml')) {
        $p = Join-Path $repoRoot $pom
        if (Test-Path $p) {
            $m = [regex]::Match((Get-Content $p -Raw), '<version>([\w\.\-]+)</version>')
            if ($m.Success -and $m.Groups[1].Value -ne $version) {
                $problems += "$pom <version> is $($m.Groups[1].Value), expected $version."
            }
        }
    }

    if ($problems.Count -gt 0) {
        throw ("Release metadata validation failed:`n  - " + ($problems -join "`n  - "))
    }
    if ($isFinalRelease) {
        Note "release metadata: CHANGELOG dated, install snippet + train poms consistent OK"
    } else {
        Note "release metadata (pre-release): train poms consistent OK (snippets/Latest-stable left on last stable)"
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
    $mavenCentralRegex = [regex]'(?<=<artifactId>graph-compose</artifactId>\s*<version>)v?[\w\.\-]+(?=</version>)'
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

    $gradleCentralRegex = [regex]'(?<=io\.github\.demchaav:graph-compose:)v?[\w\.\-]+(?=")'
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

function Update-ModuleReadmeInstallVersion($readmePath, $newVersion) {
    # Per-module READMEs carry copy-paste install snippets for THEIR OWN train
    # artifact (Maven + Gradle). Bump every occurrence — unlike the root README
    # there is no legacy-format fallback. Also used for documentation pages that
    # carry a train install snippet; a page names more than one coordinate, so the
    # match cannot lean on "one file, one coordinate" the way a module README can.
    # graph-compose-fonts and graph-compose-emoji ship on their own fonts-v* /
    # emoji-v* tags, so they are excluded explicitly here rather than by the
    # caller's choice of file — an engine cut must never rewrite their version,
    # wherever their snippet turns up.
    if (-not (Test-Path $readmePath)) {
        Note "skip (no file): $readmePath"
        return
    }
    $content = Get-Content $readmePath -Raw
    $changed = $false

    $mavenRegex = [regex]'(?<=<artifactId>graph-compose(?!-fonts|-emoji)[\w\-]*</artifactId>\s*<version>)v?[\w\.\-]+(?=</version>)'
    $afterMaven = $mavenRegex.Replace($content, $newVersion)
    if ($content -ne $afterMaven) {
        $content = $afterMaven
        $changed = $true
        Note "bumped module README Maven snippet: $readmePath -> $newVersion"
    }

    $gradleRegex = [regex]'(?<=io\.github\.demchaav:graph-compose(?!-fonts|-emoji)[\w\-]*:)v?[\w\.\-]+(?=")'
    $afterGradle = $gradleRegex.Replace($content, $newVersion)
    if ($content -ne $afterGradle) {
        $content = $afterGradle
        $changed = $true
        Note "bumped module README Gradle snippet: $readmePath -> $newVersion"
    }

    if (-not $changed) {
        Note "no change: $readmePath (version already $newVersion?)"
        return
    }

    if ($DryRun) {
        Write-Host "    [DRY RUN] Bump $readmePath install snippets -> $newVersion" -ForegroundColor Yellow
    } else {
        [System.IO.File]::WriteAllText($readmePath, $content)
    }
}

function Update-ReleaseSmokeDefaultVersion($repoRoot, $newVersion) {
    # The consumer-smoke harness hard-codes the version it tests when none is
    # passed. Left behind, a post-release run that accepts the prefilled default
    # re-verifies the PREVIOUS release and reports green — the one failure mode a
    # release gate must not have. Each pattern is anchored to its own construct so
    # a bump cannot smear across unrelated version strings in the same file.
    $targets = @(
        @{ Path = 'scripts/release-smoke/run.sh';        Pattern = '(?<=^GC_VERSION=")[\w\.\-]+(?=")';               Label = 'run.sh default' },
        @{ Path = 'scripts/release-smoke/run.sh';        Pattern = '(?<=tests gc\.version=)[\w\.\-]+';                Label = 'run.sh usage' },
        @{ Path = 'scripts/release-smoke/run.ps1';       Pattern = "(?<=\[string\]\`$Version = ')[\w\.\-]+(?=')";     Label = 'run.ps1 default' },
        @{ Path = 'scripts/release-smoke/run.ps1';       Pattern = '(?<=# isolated, tests )[\w\.\-]+';                Label = 'run.ps1 usage' },
        @{ Path = '.github/workflows/release-smoke.yml'; Pattern = "(?<=^\s{8}default: ')[\w\.\-]+(?=')";             Label = 'workflow input default' },
        @{ Path = 'scripts/release-smoke/README.md';     Pattern = '(?<=defaults to the current published release \(`)[\w\.\-]+(?=`\))'; Label = 'README prose' }
    )

    foreach ($target in $targets) {
        $path = Join-Path $repoRoot $target.Path
        if (-not (Test-Path $path)) {
            Note "skip (no file): $($target.Path)"
            continue
        }
        $content = Get-Content $path -Raw
        $updated = [regex]::Replace($content, $target.Pattern, $newVersion, 'Multiline')
        if ($content -eq $updated) {
            Note "no change: $($target.Path) [$($target.Label)] (already $newVersion?)"
            continue
        }
        if ($DryRun) {
            Write-Host "    [DRY RUN] Bump $($target.Path) [$($target.Label)] -> $newVersion" -ForegroundColor Yellow
        } else {
            [System.IO.File]::WriteAllText($path, $updated)
            Note "bumped release-smoke $($target.Label): $($target.Path) -> $newVersion"
        }
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

    # The GitHub Pages showcase (web/index.html) hardcodes the version in
    # several spots that do NOT inherit from the pom — they previously sat at
    # v1.6.1 while the library shipped v1.6.4. VersionConsistencyGuardTest
    # fails the verify gate if any lags, so flip them all in lockstep with the
    # README + poms. The Maven Central format coordinates use bare semver
    # ($newVersion), the hero badge keeps the v-prefix ($tag), and the
    # downloadUrl points at the Central artefact page. Lookbehind/lookahead
    # so only the version token is rewritten.
    $replacements = @(
        @{ Regex = [regex]'(?<="softwareVersion": ")v?[\w\.\-]+(?=")';                                                     Value = $newVersion; Label = 'JSON-LD softwareVersion' },
        @{ Regex = [regex]'(?<=https://central\.sonatype\.com/artifact/io\.github\.demchaav/graph-compose/)v?[\w\.\-]+(?=")'; Value = $newVersion; Label = 'Central downloadUrl' },
        @{ Regex = [regex]'(?<=Java &middot; )v?[\w\.\-]+(?= &middot; MIT)';                                                Value = $tag;        Label = 'hero badge' },
        @{ Regex = [regex]'(?<=&lt;artifactId&gt;graph-compose&lt;/artifactId&gt;\s*&lt;version&gt;)v?[\w\.\-]+(?=&lt;/version&gt;)'; Value = $newVersion; Label = 'Maven Central snippet' },
        @{ Regex = [regex]"(?<=io\.github\.demchaav:graph-compose:)v?[\w\.\-]+(?=')";                                        Value = $newVersion; Label = 'Gradle Central snippet' }
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
        Note "no change: web/index.html version (already $tag?)"
        return
    }

    if ($DryRun) {
        Write-Host "    [DRY RUN] web/index.html version -> $tag" -ForegroundColor Yellow
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

function Build-ExampleCatalogue {
    # Renders the whole example catalogue into examples/target/generated-pdfs at the
    # version the poms now carry. Both the published site and the committed previews
    # are copied out of that tree, so it is built once and read twice.
    # Quote the -D argument: PowerShell's call operator drops the leading
    # '-D' on the way to mvnw.cmd, so Maven sees ".mainClass=..." as a
    # lifecycle phase. Wrapping the whole token in quotes preserves it
    # as a single literal argument.
    $execProp = '"-Dexec.mainClass=com.demcha.examples.support.ShowcaseSync"'
    $generateProp = '"-Dexec.mainClass=com.demcha.examples.GenerateAllExamples"'
    # The examples module depends on these bumped SNAPSHOT siblings (not on
    # Central); each must be installed before exec:java can resolve them. Keep
    # this list in lockstep with examples/pom.xml — a module the examples depend
    # on but that is missing here fails Step 4 with "Could not find artifact
    # …:<new version>", because the just-bumped version exists nowhere yet.
    # ReleaseScriptInstallListGuardTest fails the build if the two drift apart.
    #
    # ORDER IS PART OF THE CONTRACT, not presentation. Each module is installed on
    # its own, so anything it needs must already be in the local repository at the
    # just-bumped version — and that version exists nowhere else, not in the
    # reactor and not on Central. Two edges matter:
    #   render-pdf  before render-pptx and wrapper  (compile scope)
    #   testing     before render-pptx              (test scope, since #407)
    # The second was wrong from the moment render-pptx took that dependency, and
    # stayed invisible: a cut only fails on it when the local repository does not
    # already hold graph-compose-testing at the new version, which is the normal
    # state of a clean machine. The 2.1.1 cut hit it and stopped at Step 4 —
    # before any commit, tag or push, which is the one thing that went right.
    $exampleSnapshotSiblings = @('render-pdf/pom.xml', 'testing/pom.xml', 'wrapper/pom.xml',
                                 'render-docx/pom.xml', 'render-pptx/pom.xml', 'templates/pom.xml')
    if ($DryRun) {
        Write-Host "    [DRY RUN] $mvnw -B -ntp -DskipTests install -pl :graph-compose-core" -ForegroundColor Yellow
        foreach ($modulePom in $exampleSnapshotSiblings) {
            Write-Host "    [DRY RUN] $mvnw -B -ntp -DskipTests install -f $modulePom" -ForegroundColor Yellow
        }
        Write-Host "    [DRY RUN] $mvnw -B -ntp -f examples/pom.xml -DskipTests clean compile exec:java $generateProp" -ForegroundColor Yellow
        return
    }
    Push-Location $repoRoot
    try {
        # ShowcaseSync runs from the examples module, which depends on the
        # engine plus the bumped SNAPSHOT siblings render-docx and
        # graph-compose-testing. After Step 1 bumps the poms to the new release
        # version, those artifacts are not yet in the local m2 cache — only the
        # previous release is — so exec:java fails dependency resolution with
        # "Could not find artifact ...:jar:<new-version>". Install the engine and
        # each sibling first so the examples module can resolve them. Bug
        # surfaced during v1.6.5 cut: Step 4 aborted with exit 1; we had to
        # install by hand and resume manually.
        Write-Host "    > $mvnw -B -ntp -DskipTests install -pl :graph-compose-core" -ForegroundColor DarkGray
        & $mvnw -B -ntp -DskipTests install -pl :graph-compose-core 2>&1 | ForEach-Object {
            if ($_ -match 'BUILD SUCCESS|BUILD FAILURE|ERROR') {
                Write-Host "    $_" -ForegroundColor DarkGray
            }
        }
        if ($LASTEXITCODE -ne 0) {
            throw "Install root artifact failed (exit $LASTEXITCODE)"
        }
        foreach ($modulePom in $exampleSnapshotSiblings) {
            Write-Host "    > $mvnw -B -ntp -DskipTests install -f $modulePom" -ForegroundColor DarkGray
            & $mvnw -B -ntp -DskipTests install -f $modulePom 2>&1 | ForEach-Object {
                if ($_ -match 'BUILD SUCCESS|BUILD FAILURE|ERROR') {
                    Write-Host "    $_" -ForegroundColor DarkGray
                }
            }
            if ($LASTEXITCODE -ne 0) {
                throw "Install $modulePom failed (exit $LASTEXITCODE)"
            }
        }
        # Regenerate the catalogue before syncing it. ShowcaseSync only COPIES what is
        # already in examples/target/generated-pdfs, so without this the published site
        # is whatever happens to be sitting in the maintainer's target directory from
        # earlier commands — and on a clean checkout there is nothing there at all.
        # `clean` is the point: it drops artifacts left by tests or by examples that
        # have since been renamed or deleted, so the published tree is a function of
        # the tag rather than of shell history. This runs after Step 1 bumped the poms
        # and Step 3 rewrote GH_BASE, so version-stamped renders carry the new version.
        Write-Host "    > $mvnw -B -ntp -f examples/pom.xml -DskipTests clean compile exec:java $generateProp" -ForegroundColor DarkGray
        & $mvnw -B -ntp -f examples/pom.xml -DskipTests clean compile exec:java $generateProp 2>&1 | ForEach-Object {
            if ($_ -match 'BUILD SUCCESS|BUILD FAILURE|ERROR') {
                Write-Host "    $_" -ForegroundColor DarkGray
            }
        }
        if ($LASTEXITCODE -ne 0) {
            throw "GenerateAllExamples failed (exit $LASTEXITCODE)"
        }
    } finally {
        Pop-Location
    }
}

function Sync-ShowcaseSite {
    # Copies the generated catalogue into web/showcase and writes web/examples.json.
    # Reads the tree Build-ExampleCatalogue leaves behind — call it first.
    $execProp = '"-Dexec.mainClass=com.demcha.examples.support.ShowcaseSync"'
    if ($DryRun) {
        Write-Host "    [DRY RUN] $mvnw -B -ntp -f examples/pom.xml -DskipTests compile exec:java $execProp" -ForegroundColor Yellow
        return
    }
    Push-Location $repoRoot
    try {
        # `compile` before exec:java is REQUIRED: Step 3 rewrote ShowcaseMetadata.GH_BASE
        # to /blob/<tag>, and exec:java runs the COMPILED class. Without recompiling it here,
        # ShowcaseSync would emit examples.json with the previous release's "View Code" links
        # (the drift that shipped a 2.0 site still linking /blob/v1.9.0). Mirrors
        # Render-ReadmeBanner, which recompiles for the same filtered-source reason.
        & $mvnw -B -ntp -f examples/pom.xml -DskipTests compile exec:java $execProp 2>&1 | ForEach-Object {
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

function Run-ShowcaseSync {
    # The pair, for the post-release pass: it regenerates the site with branch links
    # and deliberately leaves the committed previews alone — those belong to the tag.
    Build-ExampleCatalogue
    Sync-ShowcaseSite
}

function Refresh-CommittedPreviews {
    # Copies the freshly generated catalogue over the previews the repository
    # commits under assets/readme/examples. README and the showcase site read
    # those files rather than rendering anything, and until CommittedAssetDriftTest
    # arrived nothing held them to the code: they drifted release by release, and
    # a deck went two of them without the bold weights its styles asked for.
    #
    # Which previews are published is the folder itself — every file already there
    # gets its counterpart, and nothing new is added. That is the same set the
    # drift gate compares, so the refresh and the check cannot disagree about what
    # is published.
    #
    # Runs on every cut, -SkipShowcase or not: that flag is about the published
    # site under web/, while these files ship in the repository. It must also run
    # BEFORE Step 5, since `mvnw verify` is where the drift gate would otherwise
    # fail the release on previews this step exists to refresh.
    $previews = Join-Path $repoRoot 'assets/readme/examples'
    $generated = Join-Path $repoRoot 'examples/target/generated-pdfs'
    if (-not (Test-Path $previews)) {
        Note "no committed previews at $previews — nothing to refresh"
        return
    }
    if (-not $DryRun -and -not (Test-Path $generated)) {
        throw "Refresh-CommittedPreviews: $generated is missing — the catalogue must be generated first."
    }

    $committed = Get-ChildItem -Path $previews -File
    if ($DryRun) {
        Write-Host "    [DRY RUN] refresh $($committed.Count) committed previews from $generated" -ForegroundColor Yellow
        return
    }

    # The committed folder is flat, so a name is the whole address. Two rendered
    # documents sharing one would leave whichever the walk reached first deciding what
    # a preview gets refreshed from — the same silence CommittedAssetDriftTest refuses,
    # and the reason to refuse it here too: with -SkipVerify that gate never runs, and
    # the wrong document would be committed under the right name.
    $rendered = @{}
    foreach ($file in Get-ChildItem -Path $generated -File -Recurse) {
        if ($rendered.ContainsKey($file.Name)) {
            throw ("Refresh-CommittedPreviews: two rendered documents share the name " +
                   "$($file.Name) ($($rendered[$file.Name]) and $($file.FullName)).")
        }
        $rendered[$file.Name] = $file.FullName
    }

    $missing = @()
    foreach ($file in $committed) {
        if ($rendered.ContainsKey($file.Name)) {
            Copy-Item -Path $rendered[$file.Name] -Destination $file.FullName -Force
        } else {
            $missing += $file.Name
        }
    }
    if ($missing.Count -gt 0) {
        # A committed preview no example renders cannot be refreshed, and the drift
        # gate fails on it a step later. Say so here, where the name is still known.
        throw "Refresh-CommittedPreviews: nothing renders $($missing -join ', ')."
    }
    Note "previews: $($committed.Count) refreshed from the catalogue"
}

function Render-ReadmeBanner {
    # Re-renders assets/readme/repository_showcase_render.png — the 2.0 module-first
    # hero (EngineDeckV2Example.renderBannerImage) — so the hero's version pill
    # carries the just-bumped ${project.version} (read from the filtered
    # banner.properties). The `compile` is REQUIRED: banner.properties is filtered
    # at examples-compile time, so the examples module must be recompiled AFTER the
    # Step-1 version bump — otherwise the banner would carry the previous release
    # version. Runs after Build-ExampleCatalogue, which already installed the bumped root
    # artifact into the local m2 cache so the examples module resolves it.
    Write-Host "  > Re-render the version-stamped README hero banner" -ForegroundColor Cyan
    $banner = Join-Path $repoRoot 'assets/readme/repository_showcase_render.png'
    $execProp = '"-Dexec.mainClass=com.demcha.examples.support.ReadmeBannerV2Renderer"'
    $execArgs = "`"-Dexec.args=$banner`""
    if ($DryRun) {
        Write-Host "    [DRY RUN] $mvnw -f examples/pom.xml -DskipTests compile exec:java $execProp $execArgs" -ForegroundColor Yellow
        return
    }
    Push-Location $repoRoot
    try {
        & $mvnw -B -ntp -f examples/pom.xml -DskipTests compile exec:java $execProp $execArgs 2>&1 | ForEach-Object {
            if ($_ -match 'Rendered README banner|BUILD SUCCESS|BUILD FAILURE|ERROR') {
                Write-Host "    $_" -ForegroundColor DarkGray
            }
        }
        if ($LASTEXITCODE -ne 0) {
            throw "README banner render failed (exit $LASTEXITCODE)"
        }
    } finally {
        Pop-Location
    }
    Note "banner: assets/readme/repository_showcase_render.png re-rendered"
}

# ============================================================
# Mode: -PostReleaseOnly
# ============================================================
if ($PostReleaseOnly) {
    Push-Location $repoRoot
    try {
        Step 0 "Pre-flight checks"
        # Same branch / clean-tree / origin-sync gate as a real cut: -PostReleaseOnly
        # also commits and pushes to $Branch, so it must not run from the wrong branch,
        # over a dirty tree, or out of sync with origin.
        Assert-BranchPreflight $Branch

        # The released version is whatever the train poms currently carry (a cut leaves
        # them at the release version). Open the next patch development line from it. The
        # 2.0 module layout keeps the engine version in core/pom.xml; the legacy 1.x tree
        # has no core/ and retired post-release bumping, so read defensively and skip the
        # bump when it is absent (the showcase flip below still runs).
        $nextSnapshot = $null
        $corePom = Join-Path $repoRoot 'core/pom.xml'
        if (Test-Path $corePom) {
            $currentVersion = [regex]::Match((Get-Content $corePom -Raw), '<version>([\w\.\-]+)</version>').Groups[1].Value
            $nextSnapshot = Get-NextSnapshotVersion $currentVersion
        }

        Step 1 "Switch ShowcaseMetadata GH_BASE back to /blob/$Branch"
        $showcaseChanged = Update-ShowcaseGhBase $Branch
        if ($showcaseChanged -or $DryRun) {
            Step 2 "Regenerate web/examples.json with $Branch links"
            Run-ShowcaseSync
        } else {
            Note "GH_BASE already points to $Branch."
        }

        # Post-release version bump: move the train off the release version onto the
        # next patch -SNAPSHOT so develop builds are distinguishable from the release
        # AND the japicmp gate (baseline = the release) actually compares (it short-
        # circuits when the working version equals the baseline). The README / showcase
        # INSTALL SNIPPETS are deliberately NOT touched here: they keep advertising the
        # version actually on Maven Central, which VersionConsistencyGuardTest requires
        # during a -SNAPSHOT cycle. cut-release rewrites the snippets to the new version
        # at the NEXT release commit. Idempotent: if the poms are already on a -SNAPSHOT,
        # Get-NextSnapshotVersion returns $null and the bump is skipped.
        $bumpedPoms = @()
        if ($nextSnapshot) {
            Step 3 "Open the next development line: bump train poms to $nextSnapshot"
            foreach ($pom in @('core/pom.xml', 'pom.xml', 'examples/pom.xml', 'benchmarks/pom.xml',
                    'qa/pom.xml', 'coverage/pom.xml', 'render-pdf/pom.xml', 'render-docx/pom.xml',
                    'render-pptx/pom.xml', 'templates/pom.xml', 'testing/pom.xml', 'wrapper/pom.xml', 'bundle/pom.xml')) {
                if (Test-Path (Join-Path $repoRoot $pom)) {
                    Update-PomVersion (Join-Path $repoRoot $pom) $nextSnapshot
                    $bumpedPoms += $pom
                }
            }
        } else {
            Step 3 "Skipped SNAPSHOT bump (no core/pom.xml, or the current version is not a final X.Y.Z release)"
        }

        # Validate the bump BEFORE committing or pushing: a reactor `validate` resolves
        # every module at the new SNAPSHOT (catches an inconsistent bump / unresolvable
        # parent), and VersionConsistencyGuardTest confirms the poms stay in lockstep and
        # the install snippets still advertise the published release (the -SNAPSHOT-cycle
        # rule). Only runs when poms were actually bumped.
        if ($bumpedPoms.Count -gt 0) {
            Step "3b" "Validate the bump (mvnw validate + VersionConsistencyGuardTest)"
            # Pass args as SPLATTED arrays, never inline: the call operator `&` re-tokenizes
            # an inline unquoted arg with a dot (e.g. -Djacoco.skip=true splits into
            # `-Djacoco` and `.skip=true`), which Maven then reads as a bogus phase. Array
            # elements are passed literally (same pattern the japicmp gate uses).
            $validateArgs = @('-B', '-ntp', 'validate')
            $guardArgs = @('-B', '-ntp', 'test', '-Dtest=VersionConsistencyGuardTest', '-pl', ':graph-compose-core', '-Djacoco.skip=true')
            if ($DryRun) {
                Write-Host "    [DRY RUN] $mvnw $($validateArgs -join ' ')" -ForegroundColor Yellow
                Write-Host "    [DRY RUN] $mvnw $($guardArgs -join ' ')" -ForegroundColor Yellow
            } else {
                & $mvnw @validateArgs 2>&1 | ForEach-Object {
                    if ($_ -match 'BUILD SUCCESS|BUILD FAILURE|ERROR') { Write-Host "    $_" -ForegroundColor DarkGray }
                }
                if ($LASTEXITCODE -ne 0) { throw "mvnw validate failed after the SNAPSHOT bump." }
                & $mvnw @guardArgs 2>&1 | ForEach-Object {
                    if ($_ -match 'Tests run:|BUILD SUCCESS|BUILD FAILURE|ERROR') { Write-Host "    $_" -ForegroundColor DarkGray }
                }
                if ($LASTEXITCODE -ne 0) { throw "VersionConsistencyGuardTest failed after the SNAPSHOT bump." }
                Note "bump validated: reactor resolves + version guard green OK"
            }
        }

        # Commit whatever changed: the bumped poms and/or the restored showcase files.
        $filesToCommit = @()
        if ($showcaseChanged -or $DryRun) { $filesToCommit += @($showcaseMetadata, 'web/examples.json') }
        $filesToCommit += $bumpedPoms
        if ($filesToCommit.Count -gt 0) {
            $parts = @()
            if ($bumpedPoms.Count -gt 0) { $parts += "open $nextSnapshot" }
            if ($showcaseChanged -or $DryRun) { $parts += "restore /blob/$Branch showcase links" }
            $msg = "chore(release): " + ($parts -join ' + ')
            Step 4 "Commit"
            if ($DryRun) {
                Write-Host "    [DRY RUN] git add $($filesToCommit -join ' ')" -ForegroundColor Yellow
                Write-Host "    [DRY RUN] git commit -m `"$msg`"" -ForegroundColor Yellow
            } else {
                git add @filesToCommit
                git commit -m $msg
                Note "commit: $msg"
            }
            Step 5 "Push $Branch"
            if ($DryRun) {
                Write-Host "    [DRY RUN] git push origin $Branch" -ForegroundColor Yellow
            } else {
                git push origin $Branch
            }
        } else {
            Note "Nothing to do (showcase already on /blob/$Branch and version already a SNAPSHOT)."
        }
    } finally {
        Pop-Location
    }
    Write-Host ""
    Write-Host "Done. $Branch is on the next development line with linkable View Code buttons." -ForegroundColor Green
    return
}

# ============================================================
# Mode: full release cut
# ============================================================
Push-Location $repoRoot
try {
    $tag = "v$Version"
    # Only X.Y.Z (final) and X.Y.Z-{rc|alpha|beta}.N (pre-release) are supported. Reject
    # anything else up front, so an unrecognised suffix (e.g. 2.1.0-preview.1) cannot
    # silently fall into the pre-release path.
    if ($Version -notmatch '^\d+\.\d+\.\d+(-(rc|alpha|beta)\.\d+)?$') {
        throw "Unsupported version '$Version'. Use X.Y.Z (final) or X.Y.Z-{rc|alpha|beta}.N (pre-release)."
    }
    # A FINAL release is X.Y.Z with no suffix. Pre-releases (X.Y.Z-rc.N / -alpha / -beta)
    # ship only to the GitHub Release pre-release surface — publish.yml skips them for
    # Maven Central — so a pre-release cut must NOT touch the README 'Latest stable' block
    # or the Central install snippets: they keep advertising the last stable, on-Central
    # version. The poms still move to the pre-release version (the tag builds at it).
    $isFinalRelease = $Version -match '^\d+\.\d+\.\d+$'
    if (-not $isFinalRelease) {
        Note "pre-release ($Version): README 'Latest stable' + Central install snippets stay on the last stable version"
    }

    Step 0 "Pre-flight checks"

    # Branch / clean-tree / origin-sync gate (shared with -PostReleaseOnly).
    Assert-BranchPreflight $Branch

    # Tag doesn't already exist — locally or on origin?
    $existingTag = git tag -l $tag
    if ($existingTag) {
        throw "Tag $tag already exists locally. Bump version or delete the tag."
    }
    # Query origin with ls-remote, NOT `git fetch refs/tags/...`: fetch exits 128 for a
    # legitimately-absent tag (the normal pre-cut case), so its exit code cannot tell
    # "tag free" from "network broke". ls-remote returns empty for an absent tag and
    # fails only on a real network/auth error, so a broken connection can no longer be
    # mistaken for "tag is free".
    $remoteTag = git ls-remote --tags origin "refs/tags/$tag"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to query origin for tag $tag (git ls-remote). Cannot verify the tag is free."
    }
    if ($remoteTag) {
        throw "Tag $tag exists on origin. Bump version or delete the remote tag."
    }
    Note ("tag {0}: available OK" -f $tag)

    # The README release-status block is rewritten by Step 1 (Update-ReadmeReleaseStatus)
    # and verified after the mutation by Assert-ReleaseMetadata, so there is nothing to
    # demand of it here. What Step 0 still checks is that the block EXISTS in the shape
    # the rewrite expects — a renamed or reflowed blockquote would otherwise be skipped
    # in silence and ship `main` advertising the previous release, the v1.6.9 failure.
    if ($isFinalRelease) {
        $readmeRaw = Get-Content (Join-Path $repoRoot 'README.md') -Raw
        if ($readmeRaw -notmatch '\*\*Latest stable\*\*:\s*\[v[\w\.\-]+\]') {
            throw "README has no '**Latest stable**: [vX.Y.Z](...)' release-status line for the cut to rewrite."
        }
        Note "README release-status block present OK"
    }

    if ($isFinalRelease) {
        $step1Title = "Bump versions to $Version (poms + README install snippets)"
    } else {
        $step1Title = "Bump versions to $Version (poms only; snippets stay on last stable)"
    }
    Step 1 $step1Title
    # All ENGINE-LINE version sites must move together or
    # VersionConsistencyGuardTest fails the verify gate below: the standalone
    # the engine core/pom.xml (the published artifact), the root reactor
    # aggregator (pom.xml), the examples/benchmarks children whose inherited
    # <parent> version tracks the aggregator, and the standalone bundle
    # aggregate (graph-compose-bundle).
    # NOTE: graph-compose-fonts is deliberately absent — it carries an
    # INDEPENDENT version line (currently 1.1.0) and ships on its own fonts-v*
    # tag, so an engine release must never rewrite it. Its first <version> is
    # 1.1.0, not the engine version, so even a stray reactor bump would skip it.
    Update-PomVersion (Join-Path $repoRoot 'core/pom.xml') $Version
    Update-PomVersion (Join-Path $repoRoot 'pom.xml') $Version
    Update-PomVersion (Join-Path $repoRoot 'examples/pom.xml') $Version
    Update-PomVersion (Join-Path $repoRoot 'benchmarks/pom.xml') $Version
    # graph-compose-qa / graph-compose-coverage are aggregator children too. They
    # inherit their version, but the inherited <parent><version> is a literal that
    # must track the root: skipping them leaves graph-compose-build unresolvable in a
    # clean reactor build (no local m2 cache), which breaks the release.yml /
    # publish.yml verify on the tag even though a warm local build passes.
    Update-PomVersion (Join-Path $repoRoot 'qa/pom.xml') $Version
    Update-PomVersion (Join-Path $repoRoot 'coverage/pom.xml') $Version
    # render-pdf / render-docx / render-pptx track the engine line (lockstep): each
    # <version> bumps here and its graph-compose-core dep is ${project.version}
    # (follows automatically).
    Update-PomVersion (Join-Path $repoRoot 'render-pdf/pom.xml') $Version
    Update-PomVersion (Join-Path $repoRoot 'render-docx/pom.xml') $Version
    Update-PomVersion (Join-Path $repoRoot 'render-pptx/pom.xml') $Version
    # graph-compose-templates tracks the engine line (lockstep): its <version> bumps
    # here and its graph-compose-core dep is ${project.version} (follows automatically).
    Update-PomVersion (Join-Path $repoRoot 'templates/pom.xml') $Version
    # graph-compose-testing tracks the engine line (lockstep): its <version>
    # bumps here and its graph-compose dep is ${project.version} (follows
    # automatically).
    Update-PomVersion (Join-Path $repoRoot 'testing/pom.xml') $Version
    # graph-compose (the graph-compose compat wrapper (wrapper/)) tracks the engine line lockstep;
    # its graph-compose-core dep is ${project.version} (follows automatically).
    Update-PomVersion (Join-Path $repoRoot 'wrapper/pom.xml') $Version
    # Bundle tracks the engine line: its project <version> bumps here; its
    # graph-compose dep is ${project.version} (follows automatically) and its
    # graph-compose-fonts dep is ${graphcompose.fonts.version} (stays pinned —
    # the bump regex does not touch the $-prefixed property reference).
    Update-PomVersion (Join-Path $repoRoot 'bundle/pom.xml') $Version
    # Maven Central install snippets — FINAL releases only. A pre-release never lands on
    # Central (publish.yml skips hyphenated tags), so rewriting these to the pre-release
    # version would advertise a coordinate that 404s for any user who copies it; leave
    # them on the last stable, published version. VersionConsistencyGuardTest accepts the
    # latest published release for a non-final (SNAPSHOT or pre-release) working version.
    if ($isFinalRelease) {
        Update-ReadmeInstallVersion (Join-Path $repoRoot 'README.md') $Version
        Update-ReadmeReleaseStatus (Join-Path $repoRoot 'README.md') $Version
        Update-RoadmapCurrentStable (Join-Path $repoRoot 'ROADMAP.md') $Version
        # Per-module README install snippets (train modules only — fonts/emoji pin
        # their own independent versions).
        foreach ($moduleReadme in @('core/README.md', 'render-pdf/README.md', 'render-docx/README.md',
                'render-pptx/README.md', 'templates/README.md', 'testing/README.md',
                'wrapper/README.md', 'bundle/README.md')) {
            Update-ModuleReadmeInstallVersion (Join-Path $repoRoot $moduleReadme) $Version
        }
        # Documentation pages that carry a copy-paste install snippet for a train
        # artifact. Same bumper as the module READMEs — the snippet names a train
        # coordinate, so it moves with the release or a reader who opened the page
        # because something already broke is handed the previous minor. Historical
        # pages (docs/migration, docs/roadmaps, docs/archive) pin an old version on
        # purpose and are never listed here; VersionConsistencyGuardTest excludes
        # them by the same rule.
        foreach ($docPage in @('docs/troubleshooting.md')) {
            Update-ModuleReadmeInstallVersion (Join-Path $repoRoot $docPage) $Version
        }
        Update-IndexHtmlVersion (Join-Path $repoRoot 'web/index.html') $Version
        # The smoke harness's default version must follow the release, or the
        # post-release run silently re-verifies the previous one.
        Update-ReleaseSmokeDefaultVersion $repoRoot $Version
    } else {
        Note "pre-release: skipped README / module-README / web install-snippet bumps (stay on last stable)"
    }
    # The Next.js site/ and the docs->site/public mirror were retired when the static
    # showcase moved to web/ (deployed directly via .github/workflows/deploy-web.yml).
    # Only web/ is version-bumped now.

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

    Step "2b" "Validate release metadata (fast pre-tag gate)"
    # Runs in milliseconds, before the showcase regen / verify / commit / tag, so a
    # misconfigured release fails immediately. Validates the metadata cut-release just
    # mutated in Steps 1-2 (CHANGELOG date, install snippet, train pom versions). In
    # -DryRun those mutations were only previewed (files still at the pre-cut state), so
    # the assertion is deferred to the real cut. (The README 'Latest stable' block was
    # already validated in Step 0, before any mutation.)
    if ($DryRun) {
        Note "[DRY RUN] train pom versions (+ CHANGELOG date & install snippet for a final release) are validated on the real cut"
    } else {
        Assert-ReleaseMetadata $Version $isFinalRelease
    }

    if (-not $SkipShowcase) {
        Step 3 "Switch ShowcaseMetadata GH_BASE to /blob/$tag"
        Update-ShowcaseGhBase $tag | Out-Null
    } else {
        Step 3 "Skipped showcase GH_BASE flip (-SkipShowcase)"
    }

    # The catalogue is built either way: the site is copied out of it, and so are the
    # previews. -SkipShowcase is about the published tree under web/, not about this.
    Step 4 "Build the example catalogue at $Version"
    Build-ExampleCatalogue

    # The README assets follow the tag, so only a final cut moves them. On a
    # pre-release the version they would carry is the qualifier-stripped one — a
    # release that does not exist yet — so they stay on the last published version
    # along with the property that records it.
    #
    # -SkipShowcase does not skip this: these files ship in the repository, and a
    # preview left at the previous release is what CommittedAssetDriftTest fails the
    # next build on. It has to happen before Step 5, which is where that gate runs.
    if ($isFinalRelease) {
        Step "4b" "Re-render the README assets at $Version"
        Update-AssetVersion (Join-Path $repoRoot 'examples/pom.xml') $Version
        Refresh-CommittedPreviews
        Render-ReadmeBanner
    } else {
        Step "4b" "Skipped the README assets (pre-release cut: they stay on the last published version)"
    }

    if (-not $SkipShowcase) {
        Step "4c" "Regenerate web/examples.json with $tag links"
        Sync-ShowcaseSite
    } else {
        Step "4c" "Skipped web/showcase sync (-SkipShowcase)"
    }

    if (-not $SkipVerify) {
        Step 5 "Run mvnw clean verify (sanity check)"
        # The whole train ships on the tag, so verify the whole reactor. The 2.0
        # core/ layout builds every module from the root; the older 1.x layout
        # scopes to the engine at the root (-pl .). Detect by core/pom.xml.
        $verifyArgs = if (Test-Path (Join-Path $repoRoot 'core/pom.xml')) {
            @('-B','-ntp','clean','verify')
        } else {
            @('-B','-ntp','clean','verify','-pl','.')
        }
        if ($DryRun) {
            Write-Host "    [DRY RUN] $mvnw $($verifyArgs -join ' ')" -ForegroundColor Yellow
        } else {
            & $mvnw @verifyArgs 2>&1 | ForEach-Object {
                if ($_ -match 'Tests run:|BUILD SUCCESS|BUILD FAILURE|ERROR') {
                    Write-Host "    $_" -ForegroundColor DarkGray
                }
            }
            if ($LASTEXITCODE -ne 0) {
                throw "mvnw verify failed."
            }
            Note "mvnw clean verify: green"
        }
    } else {
        Step 5 "Skipped mvnw verify (-SkipVerify)"
    }

    if (-not $SkipVerify) {
        Step "5b" "Binary-compatibility gate (japicmp vs the published baseline)"
        # Confirm the graph-compose-core public API stays binary-compatible with the
        # japicmp baseline BEFORE the tag is cut — independent of the PR-time CI japicmp
        # job, which a direct-to-branch push could bypass. 2.0 module layout only (core/
        # present); the legacy 1.x single-artifact tree has no such gate. Precondition:
        # the baseline (japicmp.baseline in core/pom.xml) must already be on Central — so
        # this gate is meaningful from 2.0.1 onward (vs the published 2.0.0), not on the
        # first-of-a-major cut that publishes the baseline itself.
        if (Test-Path (Join-Path $repoRoot 'core/pom.xml')) {
            $japicmpArgs = @('-B', '-ntp', '-P', 'japicmp', '-Dmaven.test.skip=true', '-Djacoco.skip=true', 'verify', '-pl', ':graph-compose-core')
            if ($DryRun) {
                Write-Host "    [DRY RUN] $mvnw $($japicmpArgs -join ' ')" -ForegroundColor Yellow
            } else {
                & $mvnw @japicmpArgs 2>&1 | ForEach-Object {
                    if ($_ -match 'BUILD SUCCESS|BUILD FAILURE|ERROR|incompatib') {
                        Write-Host "    $_" -ForegroundColor DarkGray
                    }
                }
                if ($LASTEXITCODE -ne 0) {
                    throw "japicmp gate failed: the tagged code breaks binary compatibility with the published baseline."
                }
                Note "japicmp: binary-compatible with the baseline OK"
            }
        } else {
            Note "japicmp gate skipped (1.x single-artifact layout)"
        }
    } else {
        Step "5b" "Skipped japicmp gate (-SkipVerify)"
    }

    Step 6 "Commit release"
    $commitMsg = "Release v$Version"
    # Version/doc files always ship; the showcase files only when it was regenerated.
    $commitFiles = @(
        'core/pom.xml',
        'pom.xml',
        'bundle/pom.xml',
        'render-pdf/pom.xml',
        'render-docx/pom.xml',
        'render-pptx/pom.xml',
        'templates/pom.xml',
        'testing/pom.xml',
        'wrapper/pom.xml',
        'examples/pom.xml',
        'benchmarks/pom.xml',
        'README.md',
        'ROADMAP.md',
        'CHANGELOG.md',
        'web/index.html',
        # Bumped by Update-ReleaseSmokeDefaultVersion so the post-release smoke run
        # defaults to the version just published, not the previous one.
        'scripts/release-smoke/run.sh',
        'scripts/release-smoke/run.ps1',
        'scripts/release-smoke/README.md',
        '.github/workflows/release-smoke.yml'
    )
    # qa + coverage exist only in the 2.0 aggregator layout; add them to the commit
    # only when present so the script stays layout-agnostic (the 1.x single-artifact
    # tree has neither) — mirroring Update-PomVersion's skip-if-absent guard. On a 2.0
    # checkout both are present; Step 5's version guard fails first if either is missing.
    foreach ($modulePom in @('qa/pom.xml', 'coverage/pom.xml')) {
        if (Test-Path (Join-Path $repoRoot $modulePom)) {
            $commitFiles += $modulePom
        }
    }
    # Per-module READMEs carry version-bumped install snippets (2.0 layout only).
    foreach ($moduleReadme in @('core/README.md', 'render-pdf/README.md', 'render-docx/README.md',
            'render-pptx/README.md', 'templates/README.md', 'testing/README.md',
            'wrapper/README.md', 'bundle/README.md')) {
        if (Test-Path (Join-Path $repoRoot $moduleReadme)) {
            $commitFiles += $moduleReadme
        }
    }
    # Documentation pages carrying a train install snippet, bumped alongside the
    # module READMEs in Step 1.
    foreach ($docPage in @('docs/troubleshooting.md')) {
        if (Test-Path (Join-Path $repoRoot $docPage)) {
            $commitFiles += $docPage
        }
    }
    # The README assets ride along whenever they were re-rendered — every final cut,
    # -SkipShowcase or not, since that flag is about the published site and these ship
    # in the repository. A pre-release leaves them alone, so it stages nothing here.
    if ($isFinalRelease) {
        $commitFiles += @(
            'assets/readme/examples',
            'assets/readme/repository_showcase_render.png'
        )
    }
    if (-not $SkipShowcase) {
        $commitFiles += @(
            'examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java',
            'web/examples.json',
            'web/showcase'
        )
    }
    if ($DryRun) {
        Write-Host "    [DRY RUN] git add $($commitFiles -join ' ')" -ForegroundColor Yellow
        Write-Host "    [DRY RUN] git commit -m `"$commitMsg`"" -ForegroundColor Yellow
    } else {
        git add @commitFiles
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
        Write-Host "      git push origin $Branch" -ForegroundColor Cyan
        Write-Host "      git push origin $tag" -ForegroundColor Cyan
    } else {
        Step 8 "Push $Branch and tag"
        if ($DryRun) {
            Write-Host "    [DRY RUN] git push origin $Branch" -ForegroundColor Yellow
            Write-Host "    [DRY RUN] git push origin $tag" -ForegroundColor Yellow
        } else {
            git push origin $Branch
            git push origin $tag
            Note "pushed: $Branch + $tag"
        }
    }

    Write-Host ""
    Write-Host "Release $tag committed locally." -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps (manual):" -ForegroundColor Cyan
    Write-Host "  - Merge $Branch into main on GitHub (fast-forward) so GitHub Pages picks up $tag." -ForegroundColor Cyan
    Write-Host "  - Create a GitHub Release for $tag with the CHANGELOG section as body." -ForegroundColor Cyan
    if ($isFinalRelease) {
        Write-Host "  - Verify the Maven Central publish resolved: mvn dependency:get -DgroupId=io.github.demchaav -DartifactId=graph-compose -Dversion=$Version" -ForegroundColor Cyan
        Write-Host "  - Smoke-test the PUBLISHED train once Central has indexed it: dispatch the Release Smoke" -ForegroundColor Cyan
        Write-Host "    workflow with version=$Version, or run: bash scripts/release-smoke/run.sh --version $Version" -ForegroundColor Cyan
    } else {
        Write-Host "  - Pre-release: NOT published to Maven Central (publish.yml skips hyphenated tags), so there" -ForegroundColor Cyan
        Write-Host "    is no Central verify or release-smoke; the GitHub Release ships as a pre-release only." -ForegroundColor Cyan
    }
    Write-Host "  - Open the next development line + restore showcase links: pwsh ./scripts/cut-release.ps1 -PostReleaseOnly -Branch $Branch" -ForegroundColor Cyan
} finally {
    Pop-Location
}
