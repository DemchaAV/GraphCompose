# GraphCompose release process

This is the canonical release runbook for GraphCompose 1.x.

- JitPack — `com.github.DemchaAV:GraphCompose:v<version>` (current)
- Maven Central — `io.github.demchaav:graphcompose:<version>` (planned, v1.7+)

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
- [ ] Architecture-guard suite explicitly green: `./mvnw -B -ntp test -pl . -Dtest='CanonicalSurfaceGuardTest,DocumentationCoverageTest,DocumentationExamplesTest,InternalAnnotationCoverageTest,PublicApiNoEngineLeakTest,SemanticLayerNoPdfBoxDependencyTest'` exits 0. These guard against legacy-API leakage in docs and engine internals leaking into the public surface — they fail loudly when README/CHANGELOG drift from the canonical authoring surface.

### C. Documentation freeze (matches target version)

- [ ] `CHANGELOG.md` has a `## v<target> — Planned` header at the top. The script flips `Planned` → today's date during release execution; if the header is missing or already dated, the script silently skips and the release ships with the wrong header.
- [ ] CHANGELOG `v<target>` section: every linked file resolves on disk. Common offenders: new `docs/adr/00XX-*.md`, `docs/migration-v1-N-to-v1-M.md`, recipe pages.
- [ ] `README.md` test-count claim matches the actual surefire total (`grep -E '[0-9]+ green tests' README.md` vs the surefire aggregate).
- [ ] `README.md` install snippets stay pinned to **the previously published tag** (e.g. `v1.5.1`) with explanatory prose like "stay pinned to `v1.X.Y` until `v<target>` ships on JitPack". This is intentional — flipping to `v<target>` before the tag exists breaks JitPack for any user copying the snippet in the publish window. The README install flip happens **post-release**, after JitPack `BUILD SUCCESS` confirms the new version resolves (see section 4.B).
- [ ] `README.md` and `examples/README.md` link audits resolve: every `(./...)` and `(../...)` link must exist on disk. Use `grep -oE '\(\.?\.?/[^)]+\.(md|java|png|pdf|jpg)\)' README.md examples/README.md | sed 's/^(//;s/)$//' | sort -u | xargs -I{} test -e {} || echo MISSING: {}`.
- [ ] `examples/README.md` gallery row count matches the file count: `find examples/src/main/java -name '*Example.java' | wc -l` equals `grep -c '^| \[' examples/README.md`.
- [ ] For minor releases (`vX.Y.0`): `docs/migration-v1-<Y-1>-to-v1-<Y>.md` exists. Patch releases skip this.

### D. Version artifacts (script-handled, agent verifies state)

The script's Step 1–4 mutates these. The agent only confirms the *current state is one the script can transition from*:

- [ ] Root `pom.xml`, `examples/pom.xml`, `benchmarks/pom.xml` `<version>` is the in-flight value (e.g. `1.6.0-beta.1`) or already at the target. Anything else (e.g. a stale `1.5.1-SNAPSHOT`) means the develop line never bumped — fix manually before running the script.
- [ ] `examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java` `GH_BASE` points to `/blob/develop`. The script flips it to `/blob/v<target>` and regenerates `docs/examples.json`.

### E. Tag must not exist

- [ ] `git tag -l v<target>` and `git ls-remote --tags origin v<target>` both return empty. The script enforces this; if a stale tag remains from a failed previous attempt, delete it intentionally (`git tag -d v<target>` + `git push origin :refs/tags/v<target>`) only with explicit user approval.

---

## 1. What `cut-release.ps1` automates

Running `pwsh ./scripts/cut-release.ps1 -Version <X.Y.Z>` performs:

1. **Pre-flight** — re-checks all of A above (branch, clean tree, in-sync, no existing tag).
2. **Bump POM versions** in `pom.xml`, `examples/pom.xml`, `benchmarks/pom.xml` to `<X.Y.Z>`.
3. **Date the CHANGELOG** — flips `## v<X.Y.Z> — Planned` to `## v<X.Y.Z> — <today-ISO>`.
4. **Switch ShowcaseMetadata GH_BASE** from `/blob/develop` to `/blob/v<X.Y.Z>` and regenerate `docs/examples.json`.
5. **`mvnw verify -pl .`** — full sanity build (skip with `-SkipVerify` only if you just ran it).
6. **Commit** as `Release v<X.Y.Z>`. Files committed: 3 POMs + CHANGELOG + ShowcaseMetadata.java + docs/examples.json. **Nothing else.** README install snippets, examples/README.md, and any other docs are NOT touched by the script.
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

1. **Wait for JitPack `BUILD SUCCESS`** — `https://jitpack.io/com/github/DemchaAV/GraphCompose/v<target>/build.log` ends in `BUILD SUCCESS`. Then:
2. **Flip README install snippets** to `v<target>` in a separate commit. The same commit removes the "stay pinned to `v<prev>` until `v<target>` ships on JitPack" defensive prose. Suggested commit message: `docs: README install snippets — v<target> is now on JitPack`.
3. **Merge `develop` → `main`** on GitHub so GitHub Pages picks up the new docs. Fast-forward only — never force-push `main`. If the push is rejected with `non-fast-forward`, a hotfix landed on `main` after the audit and the merge has to be redone after merging `origin/main` back into `develop`.
4. **Verify CI green on main** — `gh run list --branch main --limit 1` shows `success` for the tag commit.
5. **Smoke-test the JitPack snippet** — minimal POM in `$env:TEMP`, `mvn dependency:resolve` against the snippet copy-pasted from README, expect 0 exit.
6. **Re-run all examples against the published artifact** — `./mvnw -f examples/pom.xml clean package` followed by `exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples`. Expect 26+ `Generated:` lines.
7. **Flip ShowcaseMetadata back to develop** — `pwsh ./scripts/cut-release.ps1 -PostReleaseOnly`. This restores linkable "View Code" buttons for ongoing v1.x.y dev work.
8. **Create the GitHub Release** — `gh release create v<target> --title "GraphCompose v<target> — <codename> release" --notes-file <CHANGELOG section>`. Codename pattern: `v1.4`=cinematic, `v1.5`=intuitive, `v1.6`=expressive. Patch releases drop the codename. Disable "Generated release notes" — we author the body by hand from CHANGELOG.
9. **Optional**: GitHub Discussions announcement (mirror the prior release's style; close with *"author intent, not coordinates"*), LinkedIn post, r/java post.

The release is **done** only when steps 1–7 are all green.

---

## 3. Hotfix protocol (CI red after tag, or JitPack didn't pick up)

The published jar is final. **Never force-move a tag** that JitPack has already built — JitPack caches by tag SHA and won't rebuild. Always fix forward with a `vX.Y.Z+1` patch tag.

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

---

## 5. Never do (during release)

- Force-move a tag JitPack has already built — publish a new patch tag instead.
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
- [ ] JitPack `build.log` ends in `BUILD SUCCESS`
- [ ] `mvn dependency:resolve` succeeds against the README JitPack snippet
- [ ] README install snippets flipped to `v<version>` and the defensive "pinned until …" prose removed
- [ ] `develop` and `main` synced at the same SHA
- [ ] Working tree clean on develop (`git status --short` empty)
- [ ] `ShowcaseMetadata.GH_BASE` flipped back to `/blob/develop` (run `cut-release.ps1 -PostReleaseOnly`)

If any line is unchecked, the release is not done — even if the tag is up.
