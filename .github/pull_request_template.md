## Summary

<!-- 1–3 sentences: what changed and why. Reference any related issue. -->

## Type of change

- [ ] Bug fix (no public API change)
- [ ] New feature / public API addition
- [ ] Documentation only
- [ ] CI / build / tooling
- [ ] Refactor (no behavioural change)

## Checklist

- [ ] **PR targets `develop`** (the integration branch); **not `main`**.
- [ ] Branch name follows `<type>/<short-description>` (e.g. `feature/canvas-clip`, `fix/table-overflow`, `docs/recipe-themes`); issue-prefixed names like `42/fix/short-description` are also fine.
- [ ] `./mvnw -B -ntp clean verify` passes locally.
- [ ] **Java 17 compatible** &mdash; no `List.getFirst()` / `getLast()`, no `Thread.threadId()`, no switch type / deconstruction patterns, no `case null, default ->`. CI matrices Temurin 17 / 21 / 25 and will catch JDK regressions.
- [ ] **Public API change** (if any): `CHANGELOG.md` entry added under the next `## v<X.Y.Z> — Planned` heading.
- [ ] **README touched** (if any): `DocumentationCoverageTest.readmeShouldUseCanonicalDslAndAvoidLegacyApis` still passes &mdash; canonical fingerprints (`GraphCompose.document(`, `DocumentSession`, `document.pageFlow(`, `BusinessTheme`) preserved, no legacy tokens (`GraphCompose.pdf(`, `PdfComposer`, `CvTemplateV1`, …).
- [ ] **Examples touched** (if any): runnable via `./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=…`; if a new example, also added to `GenerateAllExamples` and the gallery row count in `examples/README.md`.

## Linked issue

Closes #<issue-number>
