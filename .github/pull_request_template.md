<!--
Title (the field above) follows Conventional Commits:  type(scope): concrete effect
  type:  feat | fix | refactor | chore | docs | test
  scope: api | engine | layout | style | pdf | docx | svg | charts | text | examples | recipes
  e.g.   fix(api): reject non-finite chart values   ·   feat(charts): native vector chart subsystem

Write the body in three beats — Why → What → Verification. Prefer descriptive headings when a
section has a sharper one (e.g. "What moved", "Before → after", "Binary compatibility").
Delete any heading you have nothing real to put under — never ship an empty placeholder.
-->

## Why

<!-- The problem or motivation: what was broken, missing, or risky, and where it surfaced.
     Name the root cause. Do NOT restate the title, and do NOT narrate the review/audit that found it. -->

## What changed

<!-- Bullets. Name the real class/method. Justify each non-obvious decision inline (the "why behind the how"). -->
-

## Verification

<!-- The proof it works:
     - command run + result, e.g.  `./mvnw -B -ntp clean verify` → BUILD SUCCESS, <N> tests, 0 failures
     - the new tests and what each asserts; regenerate renders if the change is visual -->

**Lane:** <!-- canonical | shared-engine | templates | build | docs | test --> — <one-line scope note>

Closes #<!-- issue number; delete this line if none -->

---

<details>
<summary>Pre-merge checklist</summary>

- [ ] Targets **`develop`** (not `main`); branch is `<type>/<short-description>`.
- [ ] `./mvnw -B -ntp clean verify` passes locally — this is the **Verification** proof above.
- [ ] **Java 17 compatible** — no `getFirst()`/`getLast()`, `Thread.threadId()`, type/deconstruction `switch`, `case null, default`. (CI runs Temurin 17 / 21 / 25.)
- [ ] **Public API changed** → `CHANGELOG.md` entry under the next `## v<X.Y.Z> — Planned` heading.
- [ ] **README / examples touched** → `DocumentationCoverageTest` stays green; a new example is wired into `GenerateAllExamples` and given a gallery row in `examples/README.md`.

</details>
