# GraphCompose release process

This is the canonical release runbook for GraphCompose 1.x.

- Maven Central — `io.github.demchaav:graphcompose:<version>` (canonical, from v1.6.6)
- JitPack — `com.github.DemchaAV:GraphCompose:v<version>` (legacy; resolves for callers pinned to v1.6.5 and earlier, no longer the documented install channel)

The release workflow is automated by [`scripts/cut-release.ps1`](../scripts/cut-release.ps1). The script must run from the `develop` branch with a clean working tree. The agent (Claude / Codex) **must complete every audit gate below before a release tag is cut**, and **must wait for explicit human approval** ("yes, cut the tag" / "делаем тег") before invoking the script.

> **Agent contract**: audit and pre-release fixes are local-only by default. The script is the only step that mutates remotes (push develop, push tag). Never tag, push tags, or merge to `main` without an explicit go-signal in the chat.

---

## 0. Pre-release agent checklist

Run this every time, in order. Stop on the first red gate and fix it before continuing.

The shell setup and exact PowerShell commands live in the `graphcompose-release-engineer` skill (loaded via `Skill` tool). On Git Bash use `./mvnw` instead of `.\mvnw.cmd`; the gates are identical.

### A. Branch + working tree

- [ ] On `develop` branch (or in a `develop` worktree). Never tag from `main`.
- [ ] `git status --short` is clean. No `??` zero-byte stragglers (`{,`, `0)`, `[Help`, etc.). Verify any leftover with `wc -c <file>` before deleting.
- [ ] `git log origin/develop..origin/main --oneline` is empty. If not, merge `origin/main` into `develop` and resolve conflicts before proceeding (a hotfix on `main` blocks the fast-forward at script Step 8).
- [ ] `git rev-parse develop origin/develop` returns identical SHAs (script enforces this in pre-flight).

### B. Build + test gates

- [ ] `./mvnw -B -ntp -q clean verify -pl .` exits 0. Every test must pass — no skips, no flake retries. Confirm `Tests run: <N>, Failures: 0, Errors: 0, Skipped: 0` from `target/surefire-reports/*.txt`.
- [ ] Examples module compiles cleanly: `./mvnw -B -ntp -q -f examples/pom.xml clean compile` exits 0. Catches `double → float` lossy narrowing and similar bugs that don't surface in the root module.
- [ ] All examples regenerate: `./mvnw -B -ntp -q -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples` produces 26+ `Generated:` lines, exits 0, and emits no `Fixed column ... is smaller than required natural width` or `Spanned cell ... requires extra width` errors. (Requires `./mvnw install -DskipTests -pl .` once first so the local `~/.m2` resolves the current SNAPSHOT/beta version.)
- [ ] Architecture-guard suite explicitly green: `./mvnw -B -ntp test -pl . -Dtest='CanonicalSurfaceGuardTest,DocumentationCoverageTest,DocumentationExamplesTest,InternalAnnotationCoverageTest,PublicApiNoEngineLeakTest,SemanticLayerNoPdfBoxDependencyTest,VersionConsistencyGuardTest'` exits 0. These guard against legacy-API leakage in docs and engine internals leaking into the public surface, and — via `VersionConsistencyGuardTest` — against version drift between the library pom, the aggregator, the inherited examples/benchmarks modules, and the README install snippets. They fail loudly when README/CHANGELOG drift from the canonical authoring surface.

### C. Documentation freeze (matches target version)

- [ ] `CHANGELOG.md` has a `## v<target> — Planned` header at the top. The script flips `Planned` → today's date during release execution; if the header is missing or already dated, the script silently skips and the release ships with the wrong header.
- [ ] CHANGELOG `v<target>` section: every linked file resolves on disk. Common offenders: new `docs/adr/00XX-*.md`, `docs/migration-v1-N-to-v1-M.md`, recipe pages.
- [ ] `README.md` test-count claim matches the actual surefire total (`grep -E '[0-9]+ green tests' README.md` vs the surefire aggregate).
- [ ] `README.md` install snippets match the **current** `pom.xml` version (on `develop` between releases that is the last published version). `VersionConsistencyGuardTest` enforces README == pom, so the two move together: `cut-release.ps1` rewrites the README Maven + Gradle install snippets to the new version in the *same* release commit it bumps the POMs (section 1, Step 2/6). The README therefore flips to the new version at release-execution time, never on `develop` ahead of the tag — a snippet pointing at a version that has not been published yet would 404 for any user who copies it. Do **not** hand-flip the README ahead of the script: that desyncs README from the still-unbumped pom and fails the guard at the verify gate.
- [ ] `README.md` and `examples/README.md` link audits resolve: every `(./...)` and `(../...)` link must exist on disk. Use `grep -oE '\(\.?\.?/[^)]+\.(md|java|png|pdf|jpg)\)' README.md examples/README.md | sed 's/^(//;s/)$//' | sort -u | xargs -I{} test -e {} || echo MISSING: {}`.
- [ ] `examples/README.md` gallery row count matches the file count: `find examples/src/main/java -name '*Example.java' | wc -l` equals `grep -c '^| \[' examples/README.md`.
- [ ] For minor releases (`vX.Y.0`): `docs/migration-v1-<Y-1>-to-v1-<Y>.md` exists. Patch releases skip this.

### D. Version artifacts (script-handled, agent verifies state)

The script's Step 1–4 mutates these. The agent only confirms the *current state is one the script can transition from*:

- [ ] The version lives in four sites that must stay in lockstep: the standalone library `pom.xml`, the reactor `aggregator/pom.xml`, and the inherited `<parent>` version of `examples/pom.xml` and `benchmarks/pom.xml` (the children no longer pin their own `<version>` — they inherit from `graphcompose-build`, and declare `<graphcompose.version>${project.version}</graphcompose.version>` rather than a literal). All four read the same value: either the in-flight develop value or already the target. `VersionConsistencyGuardTest` asserts they agree; `cut-release.ps1` Step 1 moves all four (plus the README) together. Bumping by hand outside the script — or `mvn versions:set` on a single pom — is what previously left benchmarks on the prior release; if you must bump outside the script, use `mvn -f aggregator/pom.xml versions:set -DnewVersion=<X>`.
- [ ] `examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java` `GH_BASE` points to `/blob/develop`. The script flips it to `/blob/v<target>` and regenerates `docs/examples.json`.

### E. Tag must not exist

- [ ] `git tag -l v<target>` and `git ls-remote --tags origin v<target>` both return empty. The script enforces this; if a stale tag remains from a failed previous attempt, delete it intentionally (`git tag -d v<target>` + `git push origin :refs/tags/v<target>`) only with explicit user approval.

---

## 1. What `cut-release.ps1` automates

Running `pwsh ./scripts/cut-release.ps1 -Version <X.Y.Z>` performs:

1. **Pre-flight** — re-checks all of A above (branch, clean tree, in-sync, no existing tag).
2. **Bump versions** to `<X.Y.Z>` across the library `pom.xml`, the `aggregator/pom.xml`, the inherited `<parent>` refs in `examples/pom.xml` and `benchmarks/pom.xml`, **and** the README Maven + Gradle install snippets — all in one pass, so `VersionConsistencyGuardTest` stays green at Step 5.
3. **Date the CHANGELOG** — flips `## v<X.Y.Z> — Planned` to `## v<X.Y.Z> — <today-ISO>`.
4. **Switch ShowcaseMetadata GH_BASE** from `/blob/develop` to `/blob/v<X.Y.Z>` and regenerate `docs/examples.json`.
5. **`mvnw verify -pl .`** — full sanity build (skip with `-SkipVerify` only if you just ran it).
6. **Commit** as `Release v<X.Y.Z>`. Files committed: the library `pom.xml`, `aggregator/pom.xml`, `examples/pom.xml`, `benchmarks/pom.xml`, `README.md` (install snippets), `CHANGELOG.md`, `ShowcaseMetadata.java`, and `docs/examples.json`. `examples/README.md` and any other docs are NOT touched by the script — fix those pre-release.
7. **Annotated tag** `v<X.Y.Z>` (`git tag -a -m "Release v<X.Y.Z>"`).
8. **Push** `develop` and the tag to `origin` (skip with `-SkipPush`).

The script supports `-DryRun` (preview every step), `-SkipPush` (commit + tag locally only), and `-PostReleaseOnly` (skip release work entirely, only flip GH_BASE back to `/blob/develop` and push).

---

## 2. What is manual (the agent must remember)

The script does **not** handle these. They are either pre-release or post-release responsibilities:

### 2.A Pre-release (before invoking the script, on develop)

- **Stale documentation claims** — examples count, gallery descriptors, version-anchored prose. Fix in a `docs: pre-release fixes — <what>` commit on develop, then commit, then push (or stage and let the user push).
- **CHANGELOG `## v<target> — Planned` header** — must exist before the script runs. If you bumped scope mid-cycle, ensure the planned header is still on the right version line.
- **Missing migration guide** for minor releases — write `docs/migration-v1-<prev>-to-v1-<target>.md` if absent.
- **`InternalAnnotationCoverageTest` and other guard tests** — fix any failures by adjusting the source (annotation propagation, doc rewording), never by suppressing the test or extending the allowlist. Allowlist edits are reviewable evidence of an architecture decision; write or update an ADR before suppressing.

### 2.B Post-release (after `cut-release.ps1` succeeds and the tag is pushed)

Run within 1 hour of the tag push. Independent steps can run in parallel.

1. **Wait for Maven Central artefact** — once `.github/workflows/publish.yml` turns green (see step 9 below), poll `mvn dependency:get -DgroupId=io.github.demchaav -DartifactId=graphcompose -Dversion=<target>` until it resolves (usually 5–15 minutes after the workflow finishes). Then:
2. **README install snippets** — already flipped to `<target>` by `cut-release.ps1` in the release commit (section 1, Step 2/6) and enforced by `VersionConsistencyGuardTest`. No separate post-release commit is needed; just confirm the Central artefact resolves (step 1 above) means the version the README now advertises actually exists.
3. **Merge `develop` → `main`** on GitHub so GitHub Pages picks up the new docs. Fast-forward only — never force-push `main`. If the push is rejected with `non-fast-forward`, a hotfix landed on `main` after the audit and the merge has to be redone after merging `origin/main` back into `develop`.
4. **Verify CI green on main** — `gh run list --branch main --limit 1` shows `success` for the tag commit.
5. **Smoke-test the install snippet** — minimal POM in `$env:TEMP`, `mvn dependency:resolve` against the snippet copy-pasted from README, expect 0 exit.
6. **Re-run all examples against the published artifact** — `./mvnw -f examples/pom.xml clean package` followed by `exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples`. Expect 26+ `Generated:` lines.
7. **Flip ShowcaseMetadata back to develop** — `pwsh ./scripts/cut-release.ps1 -PostReleaseOnly`. This restores linkable "View Code" buttons for ongoing v1.x.y dev work.
8. **GitHub Release — automated.** Pushing the `v<target>` tag triggers [`.github/workflows/release.yml`](../../.github/workflows/release.yml): it re-runs `./mvnw clean verify -pl .` against the tagged commit, then creates the Release with that version's CHANGELOG section as the body (hyphenated tags like `v1.7.0-rc.1` ship as pre-releases; the step is idempotent — it edits the notes if the Release already exists). The workflow titles it `GraphCompose v<target>`; for a **minor** release, edit the title to add the codename (`v1.4`=cinematic, `v1.5`=intuitive, `v1.6`=expressive; patches drop it). Create the Release by hand (`gh release create v<target> --notes-file <CHANGELOG section>`) only if the workflow is unavailable.
9. **Maven Central publish — automated (from v1.6.6).** The same `v<target>` tag push triggers [`.github/workflows/publish.yml`](../../.github/workflows/publish.yml): it re-runs `mvnw verify` at the tagged commit, signs the four artefacts (main / sources / javadoc / pom) with the repo's GPG key, and uploads to Maven Central via the `central-publishing-maven-plugin`. Hyphenated tags (`-rc`, `-alpha`, `-beta`, `-snapshot`) are skipped — those go only to the GitHub Release pre-release surface. `autoPublish=false` in the plugin config means the artefact lands in the Central validation queue; the maintainer flips the switch on [central.sonatype.com](https://central.sonatype.com) for the first publish, then can opt into auto-release in a follow-up. Verify via `mvn dependency:get -DgroupId=io.github.demchaav -DartifactId=graphcompose -Dversion=<target>` once the artifact appears (usually 5–15 minutes after the workflow turns green).
10. **Optional**: GitHub Discussions announcement (mirror the prior release's style; close with *"author intent, not coordinates"*), LinkedIn post, r/java post.

The release is **done** only when steps 1–7 are all green; step 9 adds Maven Central availability once the D-track of v1.6.6 has shipped.

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

---

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
- [ ] Maven Central artefact resolves: `mvn dependency:get -DgroupId=io.github.demchaav -DartifactId=graphcompose -Dversion=<version>` exit 0
- [ ] `mvn dependency:resolve` succeeds against the README install snippet
- [ ] README install snippets read `<version>` (flipped by the release commit; `VersionConsistencyGuardTest` green)
- [ ] `develop` and `main` synced at the same SHA
- [ ] Working tree clean on develop (`git status --short` empty)
- [ ] `ShowcaseMetadata.GH_BASE` flipped back to `/blob/develop` (run `cut-release.ps1 -PostReleaseOnly`)

If any line is unchecked, the release is not done — even if the tag is up.
