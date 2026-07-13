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

      -PostReleaseOnly — skip the release work entirely. Just flips
                         ShowcaseMetadata.GH_BASE back to /blob/<branch>,
                         re-runs ShowcaseSync, and commits +
                         pushes that change. Use after a release
                         was cut and you want ongoing branch work
                         to have linkable View Code buttons.

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
    this script with -PostReleaseOnly -Branch <branch> to flip the
    showcase links back for ongoing dev work.
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
    # there is no legacy-format fallback, and each file only ever references its
    # own coordinate, so a blanket replace within the file is safe. Called for
    # the train modules only; fonts/emoji READMEs pin their own independent
    # versions and must never be touched by an engine cut.
    if (-not (Test-Path $readmePath)) {
        Note "skip (no file): $readmePath"
        return
    }
    $content = Get-Content $readmePath -Raw
    $changed = $false

    $mavenRegex = [regex]'(?<=<artifactId>graph-compose[\w\-]*</artifactId>\s*<version>)v?[\w\.\-]+(?=</version>)'
    $afterMaven = $mavenRegex.Replace($content, $newVersion)
    if ($content -ne $afterMaven) {
        $content = $afterMaven
        $changed = $true
        Note "bumped module README Maven snippet: $readmePath -> $newVersion"
    }

    $gradleRegex = [regex]'(?<=io\.github\.demchaav:graph-compose[\w\-]*:)v?[\w\.\-]+(?=")'
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

function Run-ShowcaseSync {
    # Quote the -D argument: PowerShell's call operator drops the leading
    # '-D' on the way to mvnw.cmd, so Maven sees ".mainClass=..." as a
    # lifecycle phase. Wrapping the whole token in quotes preserves it
    # as a single literal argument.
    $execProp = '"-Dexec.mainClass=com.demcha.examples.support.ShowcaseSync"'
    # The examples module depends on these bumped SNAPSHOT siblings (not on
    # Central); each must be installed before exec:java can resolve them.
    $exampleSnapshotSiblings = @('render-pdf/pom.xml', 'wrapper/pom.xml', 'render-docx/pom.xml', 'templates/pom.xml', 'testing/pom.xml')
    if ($DryRun) {
        Write-Host "    [DRY RUN] $mvnw -B -ntp -DskipTests install -pl :graph-compose-core" -ForegroundColor Yellow
        foreach ($modulePom in $exampleSnapshotSiblings) {
            Write-Host "    [DRY RUN] $mvnw -B -ntp -DskipTests install -f $modulePom" -ForegroundColor Yellow
        }
        Write-Host "    [DRY RUN] $mvnw -B -ntp -f examples/pom.xml -DskipTests compile exec:java $execProp" -ForegroundColor Yellow
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

function Render-ReadmeBanner {
    # Re-renders assets/readme/repository_showcase_render.png — the 2.0 module-first
    # hero (EngineDeckV2Example.renderBannerImage) — so the hero's version pill
    # carries the just-bumped ${project.version} (read from the filtered
    # banner.properties). The `compile` is REQUIRED: banner.properties is filtered
    # at examples-compile time, so the examples module must be recompiled AFTER the
    # Step-1 version bump — otherwise the banner would carry the previous release
    # version. Runs after Run-ShowcaseSync, which already installed the bumped root
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
        Step 1 "Switch ShowcaseMetadata GH_BASE back to /blob/$Branch"
        $changed = Update-ShowcaseGhBase $Branch

        if ($changed -or $DryRun) {
            Step 2 "Regenerate web/examples.json with $Branch links"
            Run-ShowcaseSync

            Step 3 "Commit"
            $msg = "post-release: flip showcase links back to /blob/$Branch"
            if ($DryRun) {
                Write-Host "    [DRY RUN] git commit -m `"$msg`"" -ForegroundColor Yellow
            } else {
                git add $showcaseMetadata 'web/examples.json'
                git commit -m $msg
            }

            Step 4 "Push $Branch"
            if ($DryRun) {
                Write-Host "    [DRY RUN] git push origin $Branch" -ForegroundColor Yellow
            } else {
                git push origin $Branch
            }
        } else {
            Note "GH_BASE already points to $Branch. Nothing to do."
        }
    } finally {
        Pop-Location
    }
    Write-Host ""
    Write-Host "Done. Ongoing $Branch work has linkable View Code buttons again." -ForegroundColor Green
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
    # on the script itself) without having to switch to the release branch and back.
    # Live cuts still fail these gates loudly.
    $currentBranch = (git rev-parse --abbrev-ref HEAD).Trim()
    if ($DryRun) {
        Note "branch: $currentBranch (gate relaxed for -DryRun)"
    } else {
        # 1. On the release branch (-Branch)?
        if ($currentBranch -ne $Branch) {
            throw "Not on $Branch branch (currently on $currentBranch). Switch to $Branch first."
        }
        Note "branch: $Branch OK"

        # 2. Working tree clean?
        $status = git status --porcelain
        if ($status) {
            throw "Working tree has uncommitted changes. Commit or stash first."
        }
        Note "working tree: clean OK"

        # 3. In sync with origin?
        git fetch origin $Branch --quiet
        $local = (git rev-parse $Branch).Trim()
        $remote = (git rev-parse origin/$Branch).Trim()
        if ($local -ne $remote) {
            throw "Local $Branch ($local) is not in sync with origin/$Branch ($remote). Pull/push first."
        }
        Note "in sync with origin/$Branch OK"
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
    # All ENGINE-LINE version sites must move together or
    # VersionConsistencyGuardTest fails the verify gate below: the standalone
    # the engine core/pom.xml (the published artifact), the root reactor
    # aggregator (pom.xml), the examples/benchmarks children whose inherited
    # <parent> version tracks the aggregator, and the standalone bundle
    # aggregate (graph-compose-bundle).
    # NOTE: graph-compose-fonts is deliberately absent — it carries an
    # INDEPENDENT version line (currently 1.0.0) and ships on its own fonts-v*
    # tag, so an engine release must never rewrite it. Its first <version> is
    # 1.0.0, not the engine version, so even a stray reactor bump would skip it.
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
    Update-ReadmeInstallVersion (Join-Path $repoRoot 'README.md') $Version
    # Per-module README install snippets (train modules only — fonts/emoji pin
    # their own independent versions). VersionConsistencyGuardTest fails the
    # Step-5 verify if any of these lag the pom version.
    foreach ($moduleReadme in @('core/README.md', 'render-pdf/README.md', 'render-docx/README.md',
            'render-pptx/README.md', 'templates/README.md', 'testing/README.md',
            'wrapper/README.md', 'bundle/README.md')) {
        Update-ModuleReadmeInstallVersion (Join-Path $repoRoot $moduleReadme) $Version
    }
    Update-IndexHtmlVersion (Join-Path $repoRoot 'web/index.html') $Version
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

    if (-not $SkipShowcase) {
        Step 3 "Switch ShowcaseMetadata GH_BASE to /blob/$tag"
        Update-ShowcaseGhBase $tag | Out-Null

        Step 4 "Regenerate web/examples.json with $tag links"
        Run-ShowcaseSync
        Render-ReadmeBanner
    } else {
        Step 3 "Skipped showcase GH_BASE flip + regen + banner (-SkipShowcase)"
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
        'CHANGELOG.md',
        'web/index.html'
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
    if (-not $SkipShowcase) {
        $commitFiles += @(
            'examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java',
            'web/examples.json',
            'web/showcase',
            'assets/readme/repository_showcase_render.png'
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
    Write-Host "  1. Merge $Branch into main on GitHub (PR or fast-forward)." -ForegroundColor Cyan
    Write-Host "     This makes the deployed GitHub Pages site pick up $tag." -ForegroundColor Cyan
    Write-Host "  2. Create a GitHub Release for $tag with the CHANGELOG section as body." -ForegroundColor Cyan
    Write-Host "  3. Verify the Maven Central publish (publish.yml) resolved: mvn dependency:get -DgroupId=io.github.demchaav -DartifactId=graph-compose -Dversion=$Version" -ForegroundColor Cyan
    Write-Host "  4. Flip showcase links back to ${Branch}:" -ForegroundColor Cyan
    Write-Host "       pwsh ./scripts/cut-release.ps1 -PostReleaseOnly -Branch $Branch" -ForegroundColor Cyan
} finally {
    Pop-Location
}
