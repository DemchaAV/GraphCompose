# Roadmap

GraphCompose is solo-maintained. This roadmap is a direction, not a contract. Dates are intentionally omitted. Concrete work is tracked in [issues](https://github.com/DemchaAV/GraphCompose/issues) and shipped work is recorded in [CHANGELOG.md](CHANGELOG.md). For v1.6 phase-level detail, see [docs/roadmaps/v1.6-roadmap.md](docs/roadmaps/v1.6-roadmap.md).

## Now (v1.6.x)

In flight on `main` / `develop`.

- v1.6 polish &mdash; documentation, examples, visual baselines, fixes.
- Open-source hygiene &mdash; security policy, support guidance, dependency automation, security scanning.
- **Maven Central distribution** &mdash; debuted in v1.6.6 under `io.github.demchaav:graph-compose`. Replaces JitPack as the primary install channel; the JitPack URL stays alive for existing pinned consumers but is no longer documented as a primary option. Shipped per [#7](https://github.com/DemchaAV/GraphCompose/issues/7).
- **Transitive dependency cleanup** &mdash; shipped in v1.6.7. Kotlin stdlib gone (the library is Java-first), `flexmark-all` aggregator narrowed to the three modules actually consumed by `MarkDownParser`, `jackson-dataformat-yaml` marked optional, unused `jackson-module-jsonSchema` + direct `snakeyaml` dropped, `jcl-over-slf4j` made explicit so PDFBox's commons-logging routes through SLF4J without `flexmark-all`'s transitive bridge. Also fixes a layout-cache staleness bug on `DocumentSession.registry().register(...)`. Zero breaking public API changes (japicmp `semver PATCH`).
- **CV v2 migration completion + design-token expansion** &mdash; shipped in v1.6.8. Hyperlink-aware project / entry titles: a CV row authored as `"[GraphCompose](https://github.com/x/y) (Java, PDFBox)"` now renders the title as a clickable link with the technology-stack tail intact. `MarkdownInline.append(...)` learned the `[label](url)` form and routes through `RichText.link(...)`; `ProjectRenderer` adopts it transparently. Four new contemporary `BusinessTheme` presets (`nordic()`, `editorial()`, `cinematic()`, `monochrome()`) expand the built-in design-token range to seven. Plus senior-review polish from v1.6.7 (registry symmetry on `DocumentSession`, `target-branch: develop` pinned in Dependabot, `logback-classic` 1.5.34 for CVE-2026-9828). Zero breaking public API changes (japicmp `semver PATCH`).
- **Showcase site separated from docs** &mdash; the static GitHub Pages site (hero, install snippets, searchable gallery of generated example PDFs) moved out of `docs/` into a top-level `web/` so `docs/` holds documentation only. It now deploys via GitHub Actions (`.github/workflows/deploy-web.yml`), and `cut-release.ps1` keeps its version + `web/examples.json` gallery manifest in lockstep. A Next.js 14 rebuild was prototyped under `site/` but never adopted as the live deploy target — the static site remains the one served.

## Next (v1.7)

Committed direction. Tracked in CHANGELOG (Phase E) and issues.

- **JMH benchmark migration** &mdash; replace the current custom benchmark harness with `org.openjdk.jmh` so the published numbers are credible and machine-comparable.
- **Templates v2 component refactor** &mdash; 13 of the 14 v2 CV presets are currently hand-coded `DocumentTemplate` subclasses. Route more visual decisions through `CvBuilder` and equivalent component recipes so each preset becomes a thin composition rather than a 400&ndash;700-line class.

## Later (directional)

Not committed. Reflects current thinking; priorities may shift based on user feedback and adoption signals.

- **DOCX visibility for unsupported nodes.** Make currently-silent skips (`shape`, `line`, `ellipse`, `barcode`) loud &mdash; minimum a warn log, ideally a strict-mode flag that fails instead of dropping content silently.
- **Backend-neutral layout measurement.** Decouple measurement from PDFBox-specific resources so non-PDF backends do not pull PDFBox into the dependency graph.
- **Multi-module Maven layout.** Split the artifact into `graph-compose-core` / `graph-compose-pdf` / `graph-compose-docx` / `graph-compose-templates` / `graph-compose-testing` if there is clear demand. Adds release complexity, so requires a real adoption signal first.
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
