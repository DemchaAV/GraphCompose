# GraphCompose release process

This is the canonical release runbook for GraphCompose 2.x, cut from `develop`.
For a 1.9.x critical-fix release the same flow applies from the `1.x` maintenance
branch, on the older single-jar layout where `-pl .` is the engine.

- Maven Central — `io.github.demchaav:graph-compose:<version>` (canonical, from v1.6.6)
- JitPack — `com.github.DemchaAV:GraphCompose:v<version>` (legacy; resolves for callers pinned to v1.6.5 and earlier, no longer the documented install channel)

The release workflow is automated by [`scripts/cut-release.ps1`](../../scripts/cut-release.ps1). The script must run from the `develop` branch with a clean working tree. The agent (Claude / Codex) **must complete every audit gate below before a release tag is cut**, and **must wait for explicit human approval** ("yes, cut the tag" / "делаем тег") before invoking the script.

> **Agent contract**: audit and pre-release fixes are local-only by default. The script is the only step that mutates remotes (push develop, push tag). Never tag, push tags, or merge to `main` without an explicit go-signal in the chat.

---

## 0. Pre-release agent checklist

Run this every time, in order. Stop on the first red gate and fix it before continuing.

The shell setup and exact PowerShell commands live in the `graphcompose-release-engineer` skill (loaded via `Skill` tool). On Git Bash use `./mvnw` instead of `.\mvnw.cmd`; the gates are identical.

> **Backporting to 1.9.x?** This checklist is written for the 2.x reactor on `develop`. On the `1.x` maintenance branch substitute throughout: the full gate `./mvnw -B -ntp clean verify` → `clean verify -pl .`; engine-only commands `-pl :graph-compose-core` → `-pl .`; the root `pom.xml` reactor → `aggregator/pom.xml`, with the engine version in the standalone `pom.xml`. The cut itself uses `cut-release.ps1 -Branch 1.x`.

### A. Branch + working tree

- [ ] On `develop` branch (or in a `develop` worktree). Never tag from `main`.
- [ ] `git status --short` is clean. No `??` zero-byte stragglers (`{,`, `0)`, `[Help`, etc.). Verify any leftover with `wc -c <file>` before deleting.
- [ ] `git log origin/develop..origin/main --oneline` is empty. If not, merge `origin/main` into `develop` and resolve conflicts before proceeding (a hotfix on `main` blocks the fast-forward at script Step 8).
- [ ] `git rev-parse develop origin/develop` returns identical SHAs (script enforces this in pre-flight).

### B. Build + test gates

- [ ] `./mvnw -B -ntp clean verify` exits 0 over the whole reactor. Every test must pass — no skips, no flake retries. Confirm `Tests run: <N>, Failures: 0, Errors: 0, Skipped: 0` per module from `*/target/surefire-reports/*.txt`. **Read Maven's own exit code** — never end the command in a pipe, or the shell reports the last stage's status and a `BUILD FAILURE` slips through as `0`.
- [ ] Examples module compiles cleanly: `./mvnw -B -ntp -q -f examples/pom.xml clean compile` exits 0. Catches `double → float` lossy narrowing and similar bugs that don't surface in the engine module.
- [ ] All examples regenerate: `./mvnw -B -ntp -q -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples` exits 0, prints one `Generated:` line per example, and emits no `Fixed column ... is smaller than required natural width` or `Spanned cell ... requires extra width` errors. (Requires `./mvnw install -DskipTests` once first so the local `~/.m2` resolves the current SNAPSHOT version — any standalone goal that resolves train modules from `~/.m2`, including the `qa` suite and `javadoc:javadoc`, needs this after a version bump.)
- [ ] Architecture-guard suite explicitly green: `./mvnw -B -ntp test -pl :graph-compose-core -Dtest='CanonicalSurfaceGuardTest,DocumentationCoverageTest,InternalAnnotationCoverageTest,PublicApiNoEngineLeakTest,PackageMapGuardTest,VersionConsistencyGuardTest,CiGuardListGuardTest'` exits 0. These guard against legacy-API leakage in docs and engine internals leaking into the public surface, and — via `VersionConsistencyGuardTest` — against version drift between the train poms and the README install snippets. Both markdown guards skip the gitignored `docs/private/`, so local planning notes cannot fail a run that CI is unable to reproduce.
- [ ] Javadoc gate green on the published modules: `./mvnw -B -ntp javadoc:javadoc -pl :graph-compose-core,:graph-compose-render-pdf,:graph-compose-render-docx,:graph-compose-render-pptx,:graph-compose-templates,:graph-compose-testing` exits 0. The release profile publishes a javadoc jar per module with `failOnError=false`, so a broken link only surfaces here.
- [ ] `graph-compose` publishes a javadoc jar with content. The wrapper has no sources of its own and builds the jar from the engine's, so the engine must first be installed **with the release profile** — a plain `install` attaches no `-sources.jar`, and on a cold `~/.m2` the aggregation then has nothing to read:
  ```bash
  ./mvnw -B -ntp -f core/pom.xml -P release -DskipTests -Dgpg.skip=true install
  ./mvnw -B -ntp -f wrapper/pom.xml -P release -DskipTests -Dgpg.skip=true package
  ```
  Expect `wrapper/target/graph-compose-<version>-javadoc.jar` to exist and to contain `index.html`, `com/demcha/compose/GraphCompose.html` and `com/demcha/compose/document/api/DocumentSession.html`. An unresolvable dependency source set attaches nothing, and the release then ships the coordinate without an API reference — which is what left javadoc.io serving 1.9.1 through the whole 2.0 / 2.1.0 line.

### C. Documentation freeze (matches target version)

- [ ] `CHANGELOG.md` has a `## v<target> — Planned` header at the top. The script flips `Planned` → today's date during release execution; if the header is missing or already dated, the script silently skips and the release ships with the wrong header.
- [ ] CHANGELOG `v<target>` section: every linked file resolves on disk. Common offenders: new `docs/adr/00XX-*.md`, `docs/migration/*.md`, recipe pages.
- [ ] `README.md` carries no hand-maintained count (test totals, example totals) that the release would falsify. Numbers in prose have no owner and no guard — prefer prose that stays true.
- [ ] `README.md` install snippets match the **current** `pom.xml` version (on `develop` between releases that is the last published version). `VersionConsistencyGuardTest` enforces README == pom, so the two move together: `cut-release.ps1` rewrites the README Maven + Gradle install snippets to the new version in the *same* release commit it bumps the POMs (section 1, Step 2/6). The README therefore flips to the new version at release-execution time, never on `develop` ahead of the tag — a snippet pointing at a version that has not been published yet would 404 for any user who copies it. Do **not** hand-flip the README ahead of the script: that desyncs README from the still-unbumped pom and fails the guard at the verify gate.
- [ ] `ROADMAP.md` names the version being released as the current stable line, and `## Now` points at what comes after it. Not guarded by a test.
- [ ] **`README.md` "Release status" prose block** (the `> 🟢 Latest stable: vX … 🟡 In development: vY` blockquote near the top) is **script-owned since v2.1.1**: `cut-release.ps1` Step 1 promotes the in-development half to latest stable and opens the next patch line, and `Assert-ReleaseMetadata` verifies it after the mutation. What you own is the **prose** — the one-line highlight after the version. Update it on develop before the cut so the release commit carries the right description to `main`; the version tokens take care of themselves. Step 0 only checks the block still exists in the shape the rewrite matches, so a reflowed blockquote aborts loudly instead of being skipped in silence.
- [ ] `README.md` and `examples/README.md` link audits resolve: every `(./...)` and `(../...)` link must exist on disk. Use `grep -oE '\(\.?\.?/[^)]+\.(md|java|png|pdf|jpg)\)' README.md examples/README.md | sed 's/^(//;s/)$//' | sort -u | xargs -I{} test -e {} || echo MISSING: {}`.
- [ ] Every example the runner produces has a `ShowcaseMetadata` entry, so the published site carries no placeholder cards: compare `examples/target/generated-pdfs` against the entries in `examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java`.
- [ ] For minor releases (`vX.Y.0`): a migration note exists under `docs/migration/`. Patch releases skip this.

### D. Version artifacts (script-handled, agent verifies state)

The script's Step 1–4 mutates these. The agent only confirms the *current state is one the script can transition from*:

- [ ] The train version lives in **13** poms that must stay in lockstep. `cut-release.ps1` Step 1 moves them together, so this list is the script's own array — keep the two in sync rather than re-deriving it by hand:
  `pom.xml` (root reactor), `core/pom.xml`, `render-pdf/pom.xml`, `render-docx/pom.xml`, `render-pptx/pom.xml`, `templates/pom.xml`, `testing/pom.xml`, `wrapper/pom.xml`, `bundle/pom.xml`, plus the four that inherit `<parent>`: `examples/pom.xml`, `benchmarks/pom.xml`, `qa/pom.xml`, `coverage/pom.xml`.
  All read the same value: either the in-flight develop value or already the target. `VersionConsistencyGuardTest` asserts they agree. Prefer the script over `versions:set`, which only rewrites a reactor and its inheriting children and misses the standalone poms.
  **`fonts/pom.xml` and `emoji/pom.xml` are intentionally NOT in this set** — they carry independent version lines (see §2.D) and must be free to diverge from the engine.
- [ ] `examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java` `GH_BASE` points to `/blob/develop`. The script flips it to `/blob/v<target>` and regenerates `web/examples.json`.

### E. Tag must not exist

- [ ] `git tag -l v<target>` and `git ls-remote --tags origin v<target>` both return empty. The script enforces this; if a stale tag remains from a failed previous attempt, delete it intentionally (`git tag -d v<target>` + `git push origin :refs/tags/v<target>`) only with explicit user approval.

---

## 1. What `cut-release.ps1` automates

Running `pwsh ./scripts/cut-release.ps1 -Version <X.Y.Z>` performs:

1. **Pre-flight** — re-checks all of A above (branch, clean tree, in-sync, no existing tag).
2. **Bump versions** to `<X.Y.Z>` across all 13 train poms (§0.D), **and** the install snippets — the root `README.md` plus every per-module README, which a separate pass (`Update-ModuleReadmeInstallVersion`) rewrites because their coordinates carry an artifact suffix the root regex does not match. All in one pass, so `VersionConsistencyGuardTest` stays green at Step 5. (`fonts/pom.xml` and `emoji/pom.xml` are left alone — they version independently; see §2.D.)
3. **Date the CHANGELOG** — flips `## v<X.Y.Z> — Planned` to `## v<X.Y.Z> — <today-ISO>`.
3b. **Validate release metadata** — a fast, build-free pre-tag gate: the CHANGELOG is dated for the target, the README `Latest stable` prose block names the target, the ROADMAP `Current stable` section names the target, the README install snippet reads the target, and every published-train pom carries the target version. Fails immediately (before showcase / verify / commit / tag) if any is stale. These are post-mutation checks: Step 2 rewrites both prose blocks (`Update-ReadmeReleaseStatus`, `Update-RoadmapCurrentStable`), and this step confirms the rewrite landed rather than trusting it. (The full per-module + Gradle + showcase snippet consistency is separately enforced by `VersionConsistencyGuardTest` in the Step-5 verify, which also holds the ROADMAP section to a published version — so a section left behind would fail the cut *after* the poms had already moved. That is why the script owns it.)

   **A cut that crosses a release line needs the next section staged.** `Update-RoadmapCurrentStable` swaps the version within the line the section already describes (2.1.1 → 2.1.2). Across lines (2.1.1 → 2.2.0) it cannot: the heading and prose describe the old line's headline feature, and rewriting one token would leave `## Current stable — 2.1` above `**2.2.0** is the current release`.

   Stage the new line **in its own commit, before the cut**, as a section immediately above the current one:

   ```markdown
   ## Upcoming — 2.2

   **2.2.0** is the current release. … what this line leads with …
   ```

   Leave `## Current stable` naming the published release while you do — `VersionConsistencyGuardTest` holds it to what is on Central, so a `develop` that already claimed 2.2.0 would fail its own build. The cut promotes `Upcoming` to `Current stable` and demotes the old line to `Previously`. `aPreparedUpcomingSectionNamesTheLineUnderDevelopment` keeps the staged section honest: it must name the line the poms are on, and must not linger after promotion.

   The compatibility check itself runs in **Step 0**, before any file is written, so a cut that cannot describe itself refuses with a clean tree rather than after every pom has moved.
4. **Switch ShowcaseMetadata GH_BASE** from `/blob/develop` to `/blob/v<X.Y.Z>` and regenerate `web/examples.json`.
5. **`mvnw verify`** — full reactor sanity build (the script auto-detects the layout by the presence of `core/pom.xml`, scoping to `-pl .` on the 1.x line). Skip with `-SkipVerify` only if you just ran it.
5b. **Binary-compatibility gate** — `mvnw -P japicmp verify -pl :graph-compose-core` against the published baseline (2.0 module layout only). Fails the cut if the tagged code breaks binary compatibility of the `graph-compose-core` public API (the japicmp profile lives only in `core/pom.xml`) with the baseline — a second line of defence independent of the PR-time CI japicmp job, which a direct-to-branch push could bypass. Skipped by `-SkipVerify`.
5c. **Regenerate the knowledge pack** — `node knowledge/tools/api-surface/extract-api.mjs --from-reactor` rewrites `knowledge/api/*.json|md` and `knowledge/manifest.json` from the classes the verify just compiled, so the surfaces embed the just-bumped version (the extractor reads it from the root `pom.xml`). Then `node knowledge/tools/claims/check-claims.mjs --check` and `node knowledge/tools/routing/check-routes.mjs` run as gates, mirroring the tag-time gate in [`release.yml`](../../.github/workflows/release.yml) — a stale claims index or a broken route fails the cut here, with nothing committed, instead of failing the tag after it is pushed. **Not** skipped by `-SkipVerify` (Step 4's installs already compiled every module the extractor reads). If `node` is not on `PATH` the step is skipped with a loud warning — stop before the script's push step (or cut with `-SkipPush`), regenerate, and amend, or the tag fails its own gate.
6. **Commit** as `Release v<X.Y.Z>`. Staging is an explicit allow-list, not `git add -A`. It covers the 13 train poms, `README.md`, `ROADMAP.md`, and the eight per-module READMEs (`core`, `render-pdf`, `render-docx`, `render-pptx`, `templates`, `testing`, `wrapper`, `bundle`), `CHANGELOG.md`, `ShowcaseMetadata.java`, `web/examples.json`, `web/index.html`, `web/showcase/`, the regenerated `assets/readme/repository_showcase_render.png`, and `knowledge/` (the surfaces Step 5c just regenerated). `examples/README.md` and every other doc are NOT touched by the script — fix those pre-release.
7. **Annotated tag** `v<X.Y.Z>` (`git tag -a -m "Release v<X.Y.Z>"`).
8. **Push** `develop` and the tag to `origin` (skip with `-SkipPush`).

The script supports `-DryRun` (preview every step), `-SkipPush` (commit + tag locally only), `-SkipVerify` (skip the verify + japicmp gates; the knowledge regen still runs), and `-PostReleaseOnly`. The latter skips release work entirely and instead **opens the next development line**: it bumps every train pom to the next patch `-SNAPSHOT`, flips GH_BASE back to `/blob/develop`, and regenerates the knowledge pack surfaces at the new `-SNAPSHOT` (same commands and gates as Step 5c — the surfaces embed the reactor version, so a bump committed without them turns develop's "Knowledge pack — API surface is current" CI job red until a follow-up regen lands), then commits — staging `knowledge/` beside the poms — and pushes. A pre-bump probe (`extract-api --from-reactor --check`) refuses before any pom moves when the tree cannot regenerate the surfaces — e.g. no compiled classes after a `clean` — so the failure lands on a clean tree, not mid-bump. It deliberately leaves the README/showcase **install snippets on the just-published release** — during a `-SNAPSHOT` cycle they must advertise the version actually on Central, which `VersionConsistencyGuardTest` enforces; `cut-release.ps1` rewrites them to the new version at the next release commit. `-PostReleaseOnly` is idempotent: if the poms already carry a `-SNAPSHOT`, the bump is skipped (only the showcase flip runs, if needed).

**Final vs pre-release.** A **final** release (`X.Y.Z`, no suffix) is the only kind that lands on Maven Central. A **pre-release** (`X.Y.Z-rc.N` / `-alpha` / `-beta`) ships only to the GitHub Release pre-release surface — [`publish.yml`](../../.github/workflows/publish.yml) skips hyphenated tags for Central. So `cut-release.ps1` splits its behaviour on `$isFinalRelease`: it bumps the train poms to the (pre-)release version either way, but for a **pre-release** it does **not** check the README `Latest stable` block and does **not** rewrite the README / module-README / showcase install snippets — those stay on the last stable, on-Central version (rewriting them to an RC would advertise a coordinate that 404s for anyone who copies it). `VersionConsistencyGuardTest` matches this: the snippets must equal the pom only when the pom is a concrete final version; for a `-SNAPSHOT` or a pre-release pom they must equal the latest *published* release.

---

## 2. What is manual (the agent must remember)

The script does **not** handle these. They are either pre-release or post-release responsibilities:

### 2.A Pre-release (before invoking the script, on develop)

- **Stale documentation claims** — examples count, gallery descriptors, version-anchored prose. Fix in a `docs: pre-release fixes — <what>` commit on develop, then commit, then push (or stage and let the user push).
- **CHANGELOG `## v<target> — Planned` header** — must exist before the script runs. If you bumped scope mid-cycle, ensure the planned header is still on the right version line.
- **Missing migration guide** for minor releases — write the note under `docs/migration/` if absent.
- **`InternalAnnotationCoverageTest` and other guard tests** — fix any failures by adjusting the source (annotation propagation, doc rewording), never by suppressing the test or extending the allowlist. Allowlist edits are reviewable evidence of an architecture decision; write or update an ADR before suppressing.

### 2.B Post-release (after `cut-release.ps1` succeeds and the tag is pushed)

Run within 1 hour of the tag push. Independent steps can run in parallel.

1. **Wait for Maven Central artefact** — once `.github/workflows/publish.yml` turns green (see step 9 below), poll `mvn dependency:get -DgroupId=io.github.demchaav -DartifactId=graph-compose -Dversion=<target>` until it resolves (usually 5–15 minutes after the workflow finishes). Then:
2. **README install snippets** — already flipped to `<target>` by `cut-release.ps1` in the release commit (section 1, Step 2/6) and enforced by `VersionConsistencyGuardTest`. No separate post-release commit is needed; just confirm the Central artefact resolves (step 1 above) means the version the README now advertises actually exists.
3. **Merge `develop` → `main`** on GitHub so GitHub Pages picks up the new docs. Fast-forward only — never force-push `main`. If the push is rejected with `non-fast-forward`, a hotfix landed on `main` after the audit and the merge has to be redone after merging `origin/main` back into `develop`.
4. **Verify CI green on main** — `gh run list --branch main --limit 1` shows `success` for the tag commit.
5. **Smoke-test the install snippet** — minimal POM in `$env:TEMP`, `mvn dependency:resolve` against the snippet copy-pasted from README, expect 0 exit.
6. **Re-run all examples against the published artifact** — `./mvnw -f examples/pom.xml clean package` followed by `exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples`. Expect one `Generated:` line per example.
6b. **Run the external release-smoke suite** — once Central has indexed the train, dispatch the **Release Smoke** workflow ([`.github/workflows/release-smoke.yml`](../../.github/workflows/release-smoke.yml)) with `version=<target>`, or run `bash scripts/release-smoke/run.sh --version <target>`. This resolves every published coordinate from Maven Central in a clean, GraphCompose-evicted repository (no reactor / local install) and exercises the documented consumer scenarios — the wrapper renders PDF, `graph-compose-core` alone throws `MissingBackendException`, core+render-pdf renders, and templates/testing/bundle perform their roles. It is the authoritative "a real user can install and use this" check; the minimal step-5 snippet resolve is a faster subset. (Release smoke tests **published** artifacts, so it necessarily runs post-publish, not pre-tag.)
7. **Open the next development line** — `pwsh ./scripts/cut-release.ps1 -PostReleaseOnly`. This bumps the train poms to the next patch `-SNAPSHOT` (so develop builds are distinguishable from the release and the japicmp gate compares against it) **and** restores linkable "View Code" buttons by flipping ShowcaseMetadata back to `/blob/develop`. The README/showcase install snippets stay on the just-published release.
8. **GitHub Release — automated.** Pushing the `v<target>` tag triggers [`.github/workflows/release.yml`](../../.github/workflows/release.yml): it re-runs `./mvnw clean verify` over the whole reactor against the tagged commit, then creates the Release with that version's CHANGELOG section as the body (hyphenated tags like `v1.7.0-rc.1` ship as pre-releases; the step is idempotent — it edits the notes if the Release already exists). The workflow titles it `GraphCompose v<target>`; for a **minor** release, edit the title to add the codename (`v1.4`=cinematic, `v1.5`=intuitive, `v1.6`=expressive; patches drop it). Create the Release by hand (`gh release create v<target> --notes-file <CHANGELOG section>`) only if the workflow is unavailable.
9. **Maven Central publish — automated (from v1.6.6).** The same `v<target>` tag push triggers [`.github/workflows/publish.yml`](../../.github/workflows/publish.yml): it re-runs `mvnw verify` at the tagged commit, signs each module's artefacts (main / sources / javadoc / pom &mdash; the `graph-compose` wrapper has no sources of its own, so it publishes no sources jar) with the repo's GPG key, and uploads to Maven Central via the `central-publishing-maven-plugin`. Hyphenated tags (`-rc`, `-alpha`, `-beta`, `-snapshot`) are skipped — those go only to the GitHub Release pre-release surface. `autoPublish=false` in the plugin config means the artefact lands in the Central validation queue; the maintainer flips the switch on [central.sonatype.com](https://central.sonatype.com) for the first publish, then can opt into auto-release in a follow-up. Verify via `mvn dependency:get -DgroupId=io.github.demchaav -DartifactId=graph-compose -Dversion=<target>` once the artifact appears (usually 5–15 minutes after the workflow turns green).
10. **Optional**: GitHub Discussions announcement (mirror the prior release's style; close with *"author intent, not coordinates"*), LinkedIn post, r/java post.

The release is **done** only when steps 1–7 are all green; step 9 adds Maven Central availability once the D-track of v1.6.6 has shipped.

### 2.D The fonts artifact and the bundle (since v1.8.0)

The bundled Google fonts ship as a **separate, independently-versioned**
artifact, `io.github.demchaav:graph-compose-fonts` (under `fonts/`), and there
is a convenience aggregate `io.github.demchaav:graph-compose-bundle` (under
`bundle/`).

- **The bundle tracks the engine line.** `cut-release.ps1` Step 1 bumps
  `bundle/pom.xml` in lockstep with the engine (its `<version>` and, via
  `${project.version}`, its `graph-compose` dependency). `VersionConsistencyGuardTest`
  enforces `bundle == engine`. The engine `v<target>` tag's
  [`publish.yml`](../../.github/workflows/publish.yml) deploys the engine **and**
  the bundle. Nothing extra to do for the bundle at release time.
- **The fonts artifact is NOT bumped by the engine release.** It carries its own
  version line (started at `1.0.0`) and is bumped **only when the font set
  changes**. `cut-release.ps1` deliberately does not touch `fonts/pom.xml`, and
  the version guard deliberately does not require it to equal the engine version.
- **Cutting a fonts release** (only when fonts change) — one sequence, in this
  order, because the last step is irreversible:

  1. **Land every font change meant for the version first.** All of it, on the
     release branch, before anything is tagged.
  2. Bump `fonts/pom.xml` `<version>`.
  3. Bump `<graphcompose.fonts.version>` to the same value in **every pom that
     declares it** — currently the root reactor `pom.xml` (inherited by examples
     and benchmarks), `core/pom.xml`, `render-pdf`, `render-docx`,
     `render-pptx` and `bundle/pom.xml`. Do not work from that list: run
     `VersionConsistencyGuardTest`, which discovers the declaring poms and fails
     on any that disagrees with the root — the list has been short before.
     Pinning a version that is not published yet is fine; CI installs the fonts
     module from source before the jobs that need it.
  4. Merge.
  5. **Then** push the `fonts-vX.Y.Z` tag. It triggers
     [`publish-fonts.yml`](../../.github/workflows/publish-fonts.yml), which
     deploys only `graph-compose-fonts` to Central.

  Step 5 is last because a Central artifact is immutable while the engine goes
  on naming that version as the one a family arrived in. A tag pushed between
  two font PRs publishes a jar that the catalog then promises families it does
  not contain, and the failure lands on a consumer as a missing resource at
  first use — not on us, and not on any build.

- **Bootstrapping the fonts artifact.** `core` takes `graph-compose-fonts` at
  **test** scope so its visual and snapshot suites render with the real binaries;
  test scope is not transitive, so the published engine jar stays fonts-free. A
  reactor build satisfies it from the just-built sibling, but a single-module
  build (`-pl :graph-compose-core` without `-am`, which is what several CI jobs
  run) resolves it from the local repository. Those jobs run
  `./mvnw -f fonts/pom.xml install` first; a fonts version that is not published
  yet needs the same locally.

### 2.E The emoji artifact (since v1.9.0)

The colour-emoji glyphs ship as a **separate, independently-versioned**
artifact, `io.github.demchaav:graph-compose-emoji` (under `emoji/`), mirroring
the fonts arrangement above — including the `graph-compose-bundle`, which pins
`graph-compose-emoji` (via `${graphcompose.emoji.version}`) just as it pins the
fonts, so cutting an emoji release needs the same consumer re-pin.

- **NOT bumped by the engine release.** It carries its own version line
  (started at `1.0.0`) and is bumped **only when the glyph set or shortcode
  index changes**. `cut-release.ps1` does not touch `emoji/pom.xml`, and the
  version guard does not require it to equal the engine version.
- **Cutting an emoji release** (only when the set changes): bump
  `emoji/pom.xml` `<version>`, push an `emoji-vX.Y.Z` tag. That tag triggers
  [`publish-emoji.yml`](../../.github/workflows/publish-emoji.yml), which deploys
  only `graph-compose-emoji` to Central. Then bump `<graphcompose.emoji.version>`
  in the root reactor `pom.xml` (inherited by examples) and `bundle/pom.xml` to the
  new emoji version so those consumers pin it — the `bundledEmojiVersionAgreesAcrossModules`
  version guard enforces that the two stay in agreement. `core/pom.xml` does
  **not** carry this property — the engine has no dependency on the emoji artifact
  (its tests read the glyphs from the sibling module's source via `<testResources>`).
- **Regenerating the set.** `emoji/tools/build-emoji-set.py` rebuilds
  `emoji/svg/` + `emoji-index.properties` from fresh Noto Emoji + gemoji sources,
  copying **only** the glyphs a shortcode resolves (see
  [`emoji/NOTICE.md`](../../emoji/NOTICE.md)). Bump the version when the
  regenerated set changes, then cut an `emoji-v*` release.
- **First publish:** `emoji/pom.xml` is already `1.0.0`; tag `emoji-v1.0.0` (on a
  commit that includes `publish-emoji.yml`) to ship it.

### 2.F The 2.0 module train (lockstep versioning)

From 2.0 the library ships as several modules that move together as one **version
train**: `graph-compose-core`, the `graph-compose` compat wrapper,
`graph-compose-render-pdf` / `-render-docx` / `-render-pptx`,
`graph-compose-templates`, `graph-compose-testing`, and the `graph-compose-bundle`
aggregate. They all carry the **same** version.

- **User rule (one sentence):** the same version across these modules is a
  tested-compatible set — pin one version, use it for every `graph-compose*` module
  you depend on, and upgrade them together.
- **What bumps when:** a fix in *any* train module bumps the **whole train** by a
  patch; a feature bumps it by a minor. There is no per-module version drift within
  the train. `cut-release.ps1` Step 1 bumps every train pom (`pom.xml`, `render-pdf`,
  `render-docx`, `render-pptx`, `templates`, `testing`, `wrapper`, `bundle`) plus
  `aggregator` / `examples` / `benchmarks` to the same `<X.Y.Z>` in one pass; each
  module's `graph-compose-core` dependency is `${project.version}`, so it follows
  automatically, and `VersionConsistencyGuardTest` enforces the agreement.
- **Not in the train:** `graph-compose-fonts` and `graph-compose-emoji` keep their
  own version lines and are pinned explicitly (§2.D / §2.E). They are the only
  published artifacts that do *not* move with the engine version.
- **Beta / pre-release tags:** a hyphenated train tag (e.g. `v2.0.0-beta.1`) publishes
  the whole train to the **GitHub Release pre-release surface only** — `publish.yml`
  skips Central for hyphenated tags (§2.B step 9). A beta is therefore installable from
  the GitHub pre-release (and from JitPack, which builds any tag), not from Central.

### 2.G Branch flow

The 2.x train is cut from **`develop`**, which is `cut-release.ps1`'s default branch.
On this layout the engine lives in `core/pom.xml` and the repository root `pom.xml` is
the reactor — the script bumps both plus the rest of the train, and its `Test-Path`
guard skips `core/pom.xml` on the older 1.x layout, so the same script serves both lines.

- **Release candidate:** `pwsh ./scripts/cut-release.ps1 -Version <X.Y.Z>-rc.1`.
  The hyphenated `-rc` tag ships to the GitHub pre-release surface only (Central skipped, §2.F).
- **GA:** `pwsh ./scripts/cut-release.ps1 -Version <X.Y.Z>`. The plain `v<X.Y.Z>` tag fires
  `publish.yml`, which deploys the eight-module train to Maven Central in dependency order
  — that sequence is version-agnostic and needs no per-release change.
- **1.9.x backport:** `pwsh ./scripts/cut-release.ps1 -Version 1.9.<n> -Branch 1.x`.

After a GA the branches settle back into their standing roles: `main` is fast-forwarded
to the release, `develop` carries on as the 2.x working branch, and `1.x` continues to
take critical fixes only. `cut-release.ps1 -PostReleaseOnly` opens the next
`-SNAPSHOT` line and returns `ShowcaseMetadata.GH_BASE` to `/blob/develop`.

> **Post-GA `-SNAPSHOT` naming.** `-PostReleaseOnly` always opens the next *patch*
> `-SNAPSHOT`. If the next release is a minor, bump the train to `<X.Y+1.0>-SNAPSHOT`
> by hand so the in-flight version matches the `@since` tags and the open CHANGELOG
> heading — nothing publishes from a SNAPSHOT, but the mismatch misleads every reader
> of a pom.

---

## 2.C One-time Maven Central setup (maintainer)

These steps are done **once per repo** before the publish workflow can succeed; they are *not* part of every release. Listed here so a future maintainer (or a future me) can reproduce the setup without spelunking through commit history.

1. **Generate a GPG key.**
   ```bash
   gpg --full-generate-key            # RSA 4096, no expiry, real-name / email of maintainer
   gpg --list-secret-keys --keyid-format=long
   gpg --armor --export-secret-keys <KEYID> > private-key.asc   # never commit this file
   ```
2. **Publish the public key to a keyserver pool.** Central validates the signature against one of these. Two redundant pools is the conventional minimum:
   ```bash
   gpg --keyserver keys.openpgp.org   --send-keys <KEYID>
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEYID>
   ```
   `keys.openpgp.org` requires a one-time email-verification round-trip on the address attached to the key.
3. **Register a Sonatype Central account.** At [central.sonatype.com](https://central.sonatype.com) — sign in with GitHub for the auto-verified `io.github.<gh-handle>` namespace (the `io.github.demchaav` namespace this repo claims). Verify the namespace via GitHub-account check or DNS TXT record on the published domain.
4. **Generate a Central user token.** Account → Generate User Token → copy the `username` and `password` halves. These are credentials, not the GitHub login.
5. **Wire four GitHub repo secrets.** Repo Settings → Secrets and variables → Actions → New repository secret:
   - `MAVEN_GPG_PRIVATE_KEY` — full ASCII-armored private key (the contents of `private-key.asc` from step 1).
   - `MAVEN_GPG_PASSPHRASE` — the passphrase guarding the key.
   - `CENTRAL_USERNAME` — token username from step 4.
   - `CENTRAL_TOKEN` — token password from step 4.
6. **Test the wiring on a release-candidate tag** *before* the first real release. `v1.6.6-rc.1` (hyphenated) skips Central per `publish.yml`'s `if:` guard, so it's safe — alternatively, cut `v1.6.6` for real and observe the workflow; `autoPublish=false` means a failed validation does not pollute Central, the artefact just sits in the validation queue until manually released or deleted.

If any of these stop working between releases (key expired, token rotated), the publish workflow surfaces the failure inside the workflow run — the GitHub Release is still cut by the other workflow, and the legacy JitPack URL keeps resolving for callers pinned to earlier versions.

---

## 3. Hotfix protocol (CI red after tag, or Central didn't pick up)

The published jar is final. **Never force-move a tag** that Maven Central has already validated — Central rejects re-uploading the same coordinates, and JitPack (still building legacy v1.6.5 and earlier) caches by tag SHA and won't rebuild either. Always fix forward with a `vX.Y.Z+1` patch tag.

- Diagnose: `gh run view <run-id> --log-failed`. The `Tests run: <N>, Failures: <F>` line is the source of truth, not the trace excerpt in the Actions UI annotation.
- Fix the test or doc, not the published artifact.
- Commit + push to `develop`, fast-forward to `main`.
- If the bug is in shipped runtime code (rare), publish a `vX.Y.Z+1` patch via the full pipeline. Add a CHANGELOG patch entry under the new version header.

### Common hotfix categories

| Symptom | Root cause | Fix |
|---|---|---|
| `DocumentationCoverageTest.readme<X>SectionShouldUseCanonicalDsl` red after a README rewrite | Section-anchored guard pinned to a heading the rewrite removed | Replace with one whole-file scan: `readmeShouldUseCanonicalDslAndAvoidLegacyApis` |
| `ShapeContainerVisualRegressionTest` (or any visual regression) red on Linux CI only | Cross-platform PDFBox font drift between Windows-rendered baselines and Linux CI (~1–2 % pixel diff) | Bump `mismatchedPixelBudget(0)` to ~`2_500` (calibrated against observed CI delta) |
| Runtime `Fixed column 0 width X is smaller than required natural width Y` from `GenerateAllExamples` | Table cell content's natural width exceeds its fixed column | Reduce font size, reduce padding, or pre-split via `DocumentTableCell.lines(parts)` so `cellNaturalWidth` measures the longest single line |
| `Spanned cell at row N over M fixed columns requires extra width` | Long unbroken text in a `colSpan(M)` cell over fixed columns | Same fix — multi-line cells via `lines(...)` |
| `incompatible types: possible lossy conversion from double to float` on `.margin(...)` | `DocumentInsets` accessor returns `double`, the `float` overload narrows | Switch the call to `.margin(layout.margin())` (the `DocumentInsets` overload) |
| `GenerateAllExamples` dies mid-run on a specific PDF | Windows file lock from an open viewer | Ask the user to close the viewer; do not retry blindly |
| `cut-release.ps1` aborts at "Working tree has uncommitted changes" | Untracked junk (zero-byte `{,`, `0)` etc.) or unstaged pre-release fix | Verify each is 0 bytes, delete by exact name; never `git clean -fd` blindly |

### Release-publication failure recovery

The GitHub Release ([`release.yml`](../../.github/workflows/release.yml)) and the Maven Central publish ([`publish.yml`](../../.github/workflows/publish.yml)) run independently off the same `v*` tag, so one can fail without the other. Recovery never mutates the tag.

| Symptom | Recovery |
|---|---|
| **GitHub Release not created** (release.yml failed or unavailable) | Re-run the workflow, or create it by hand: `gh release create v<X.Y.Z> --notes-file <changelog-section>`. The step is idempotent — safe to re-run. |
| **Central validation failed** (publish.yml red at a deploy/validate step) | The train deploys in dependency order — **core → render-pdf → wrapper → render-docx → render-pptx → templates → testing → bundle** — and each isolated deploy resolves inter-module deps from the local m2 the `clean install` preflight seeds (that is why the preflight uses `install`, not `verify`). Read the failing module's log, fix the cause (commonly a missing signature/sources jar or POM metadata). If it failed at **core**, re-dispatch `publish.yml` with `tag=v<X.Y.Z>` (a full re-run). If earlier modules already validated, re-dispatch with `tag=v<X.Y.Z>` **and `start_at=<the failed module>`** — the deploys always begin at core, and Central rejects re-uploading an already-validated coordinate, so a full re-run would choke on the first already-published module. |
| **Partial module publication** (some coordinates on Central, some not) | Do **not** blindly re-dispatch — the deploys always start at core, and Central rejects re-uploading an already-validated coordinate, so a full re-run fails at the first already-published module before ever reaching the missing ones. Inspect Central state (`mvn dependency:get` per coordinate, or the published-artifact matrix) to find the first **unpublished** module, then re-dispatch `publish.yml` with `tag=v<X.Y.Z>` and `start_at=<that module>` to resume from there. Never bump the tag to force a re-publish. |
| **Stale documentation discovered after the tag** | The tag is immutable — do NOT move it. Fix forward on `develop`, fast-forward to `main`; the deployed site and the `main` README correct themselves. If the stale text lives inside the immutable tag's README, clarify it in the GitHub Release body rather than re-tagging. Prose is never grounds for a patch release. |
| **When a patch release IS required** | A published coordinate is missing and cannot be completed via re-dispatch; a published POM has wrong dependencies; the default `graph-compose` wrapper does not render PDF; or a confirmed runtime defect affects normal users. Keep the patch minimal (no features/refactors), explain the exact fix in the CHANGELOG, and repeat the full release verification (including the release-smoke suite, §2.B step 6b). |

---

## 4. Lessons captured from past releases

Each learning maps to a check above.

- **v1.5.0** — README slim from 778 → 151 lines broke 4 section-anchored doc tests in a single push. *Mitigation*: section B ("Architecture-guard suite explicitly green") runs the guard suite on every release prep regardless of doc edits.
- **v1.5.0** — Visual regression baselines were Windows-rendered, CI is Linux. 1.9 % pixel drift exceeded `mismatchedPixelBudget(0)`. *Mitigation*: any new visual regression test ships with a non-zero, CI-calibrated budget from the start.
- **v1.5.0** — `develop` had not merged a v1.4.1 hotfix on `main`; `git push origin develop:main` was rejected with non-fast-forward mid-release. *Mitigation*: section A enforces `git log origin/develop..origin/main --oneline` is empty before any tag work.
- **v1.5.0** — README claimed "the current release is v1.5.0" before the tag existed; install snippets would have failed for any new user landing on the README in that window. *Mitigation*: section C pins the README install snippet to the previous published tag until JitPack confirms the new tag built; the flip is post-release (section 2.B step 2).
- **v1.5.0** — `Fixed column 0 width 90 is smaller than required natural width 92.44` only surfaced on `exec:java`, not `mvn test`. *Mitigation*: section B mandates a full `GenerateAllExamples` regen before every release.
- **v1.5.0** — 8 zero-byte junk files (`examples/p,`, `{,`, `[Help`, etc.) crept into the working tree from accidental shell-output expansions. *Mitigation*: section A hard-gates on `git status --short` cleanliness, not just on the script's pre-flight.
- **v1.6.0 prep** — slimming the README to a marketing landing renamed the canonical `DocumentSession document = …` example variable to `doc`, which silently broke `DocumentationCoverageTest.readmeShouldUseCanonicalDslAndAvoidLegacyApis` because the test asserts the literal string `document.pageFlow(` is present. *Mitigation*: any rewrite of the README "Hello world" snippet must keep `DocumentSession document` as the variable name and `document.pageFlow(`, `document.buildPdf()`, `GraphCompose.document(` as the literal canonical fingerprints the guard scans for. Renaming the variable is a guard-test break, not a stylistic preference.
- **v1.6.0 post-release** — the `examples-generation` CI job introduced after v1.6.0 went red on the first run because `examples/pom.xml` and `benchmarks/pom.xml` declare a `<graphcompose.version>` property used by their `graphcompose` dependency, and `cut-release.ps1` was only flipping the project's own `<version>` tag (the first `<version>` in each file). The subordinate POMs kept `<graphcompose.version>1.6.0-beta.1</graphcompose.version>` after the release commit; CI couldn't resolve a `1.6.0-beta.1` artifact (it never existed on any registry), so `mvnw -f examples/pom.xml clean compile` failed at dependency resolution. *Mitigation*: `Update-PomVersion` in `cut-release.ps1` now flips both the first `<version>` tag *and* a `<graphcompose.version>...</graphcompose.version>` property if present, in the same call. Future agents need not touch this — running the script handles both.
- **v1.6.5 prep** — the subordinate-POM `<graphcompose.version>` property flip from the v1.6.0 lesson above is now **superseded**: `examples/` and `benchmarks/` were converted to a reactor under a non-published `aggregator/pom.xml`, so they inherit their version from `graphcompose-build` and declare `<graphcompose.version>${project.version}</graphcompose.version>` instead of a literal. The library `pom.xml` stays standalone, so JitPack coordinates never change. Version drift is now structurally impossible *and* caught by `VersionConsistencyGuardTest` (wired into CI's guard job and the section 0.B gate). `cut-release.ps1` bumps the library pom, the aggregator, both inherited parent refs, and the README install snippets in one commit; `.github/workflows/release.yml` then gates the tag on `verify` and publishes the GitHub Release automatically. *Mitigation*: section 0.D verifies all four version sites agree; the guard fails the verify gate if any hand-edit leaves them out of sync.
- **v1.6.5 cut** — `cut-release.ps1` Step 4 (`ShowcaseSync`) aborted with `Could not find artifact io.github.demchaav:graphcompose:jar:1.6.5 in central` after Step 1 bumped the four pom.xml files to `1.6.5`: the examples module depends on `graphcompose:${project.version}`, the previous release (`1.6.4`) was the only version in the local `~/.m2`, and Step 4 had no install gate to put the just-bumped version there first. Cut had to be finished by hand — install root, re-run ShowcaseSync, verify, commit, tag, push. *Mitigation*: `Run-ShowcaseSync` now runs `./mvnw -B -ntp -DskipTests install -pl .` immediately before `exec:java`, in both Release and PostReleaseOnly modes; the dry-run preview shows both steps. Pre-flight branch / clean / sync gates are now relaxed for `-DryRun` so the script can be previewed from a feature branch while iterating on it.
- **v1.6.9 cut** — the README "Release status" prose blockquote ("Latest stable: vX") still showed the *previous* version (v1.6.8) on `main` after v1.6.9 shipped. `cut-release.ps1` flips the install snippets but not the prose block, and the agent updated the block only in the develop-only cycle-open commit — which never reaches `main`. So `main` (what GitHub renders) advertised v1.6.8 as "latest stable" right after v1.6.9 was released. Had to fix forward with a direct `docs(readme)` commit on `main`. *Mitigation*: section C now lists the prose status block as an explicit pre-cut gate — update it on develop **before** invoking the script so the release commit carries it to `main`. (Same class of bug as the v1.5.0 README-version-window lesson; recurred because the prose block, unlike the install snippets, has no guard test.)

## 5. Never do (during release)

- Force-move a tag that Maven Central has already validated or that JitPack has already built — Central rejects re-upload of the same coordinates, JitPack caches by tag SHA. Publish a new patch tag instead.
- Skip the `origin/main → develop` merge before tagging.
- Use `git add .` or `git add -A` — the develop tree often has accidental untracked junk. Stage by exact filename.
- Skip the full `GenerateAllExamples` regen — `mvn test` does not catch runtime layout exceptions in fixed-column tables.
- Suppress a guard test or extend its allowlist to make the build green. Fix the source, or write an ADR documenting the carve-out before changing the guard.
- Commit a release with `Co-Authored-By: Claude` (or any other tooling-attribution trailer). Releases are authored as `DemchaAV` only.
- Run `cut-release.ps1` without explicit human approval in the chat for the specific version being cut. "Approved last release" does not approve this one.

---

## 6. Done criteria

The release is **done** when all of these are true:

- [ ] Tag visible at `https://github.com/DemchaAV/GraphCompose/releases/tag/v<version>`
- [ ] GitHub Release created with the CHANGELOG `v<version>` body
- [ ] CI green on `main` for the tag commit
- [ ] `.github/workflows/publish.yml` succeeded for the tag
- [ ] Maven Central artefact resolves: `mvn dependency:get -DgroupId=io.github.demchaav -DartifactId=graph-compose -Dversion=<version>` exit 0
- [ ] `mvn dependency:resolve` succeeds against the README install snippet
- [ ] README install snippets read `<version>` (flipped by the release commit; `VersionConsistencyGuardTest` green)
- [ ] `develop` and `main` synced at the same SHA
- [ ] Working tree clean on develop (`git status --short` empty)
- [ ] `ShowcaseMetadata.GH_BASE` flipped back to `/blob/develop` (run `cut-release.ps1 -PostReleaseOnly`)

If any line is unchecked, the release is not done — even if the tag is up.
