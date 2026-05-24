# Roadmap

GraphCompose is solo-maintained. This roadmap is a direction, not a contract. Dates are intentionally omitted. Concrete work is tracked in [issues](https://github.com/DemchaAV/GraphCompose/issues) and shipped work is recorded in [CHANGELOG.md](CHANGELOG.md). For v1.6 phase-level detail, see [docs/roadmaps/v1.6-roadmap.md](docs/roadmaps/v1.6-roadmap.md).

## Now (v1.6.x)

In flight on `main` / `develop`.

- v1.6 polish &mdash; documentation, examples, visual baselines, fixes.
- Open-source hygiene &mdash; security policy, support guidance, dependency automation, security scanning.

## Next (v1.7)

Committed direction. Tracked in CHANGELOG (Phase E) and issues.

- **Maven Central distribution** &mdash; replace JitPack as the primary install channel. Tracked in [#7](https://github.com/DemchaAV/GraphCompose/issues/7).
- **JMH benchmark migration** &mdash; replace the current custom benchmark harness with `org.openjdk.jmh` so the published numbers are credible and machine-comparable.
- **Templates v2 component refactor** &mdash; 13 of the 14 v2 CV presets are currently hand-coded `DocumentTemplate` subclasses. Route more visual decisions through `CvBuilder` and equivalent component recipes so each preset becomes a thin composition rather than a 400&ndash;700-line class.

## Later (directional)

Not committed. Reflects current thinking; priorities may shift based on user feedback and adoption signals.

- **DOCX visibility for unsupported nodes.** Make currently-silent skips (`shape`, `line`, `ellipse`, `barcode`) loud &mdash; minimum a warn log, ideally a strict-mode flag that fails instead of dropping content silently.
- **Backend-neutral layout measurement.** Decouple measurement from PDFBox-specific resources so non-PDF backends do not pull PDFBox into the dependency graph.
- **Multi-module Maven layout.** Split the artifact into `graphcompose-core` / `graphcompose-pdf` / `graphcompose-docx` / `graphcompose-templates` / `graphcompose-testing` if there is clear demand. Adds release complexity, so requires a real adoption signal first.
- **DOCX maturity.** Either expand DOCX coverage toward PDF parity, or move DOCX behind an explicitly experimental flag.
- **Property-based testing.** Random table spans, pagination edge cases, deeply nested layouts.
- **Real PPTX export.** Current state is a manifest skeleton. Will only be built out if there is concrete user demand.
- **Public Javadoc site.** Generated and hosted, kept in sync with releases.

## Not on the roadmap

- Hosted PDF rendering service.
- WYSIWYG editor.
- HTML / CSS input.
- Browser-side rendering.

See [README &mdash; What GraphCompose is not](README.md#what-graphcompose-is-not).

## Feedback

Have a use case that should be on this list, or strong feelings about priority? Open a [discussion issue](https://github.com/DemchaAV/GraphCompose/issues/new?labels=question&title=Roadmap%3A+) or comment on the relevant tracked issue.
