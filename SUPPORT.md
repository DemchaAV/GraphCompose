# Support

GraphCompose is a solo-maintained open-source library. This file describes where to go for what.

## Channels

| Need | Where |
|------|-------|
| Usage question | [GitHub Discussions](https://github.com/DemchaAV/GraphCompose/discussions) — not the issue tracker |
| Bug report | [Bug report template](https://github.com/DemchaAV/GraphCompose/issues/new?template=bug_report.md) |
| Feature request | [Feature request template](https://github.com/DemchaAV/GraphCompose/issues/new?template=feature_request.md) |
| Security vulnerability | [SECURITY.md](SECURITY.md) — do **not** use public issues |
| Documentation gap | Open an issue with the `docs` label, or a PR against the affected file |

## Before asking

Self-serve, ordered by likelihood of containing the answer:

1. [README.md](README.md) — installation, hello-world, which artifact to depend on.
2. [docs/first-document.md](docs/first-document.md) — the five-minute path to a rendered PDF.
3. [docs/getting-started.md](docs/getting-started.md) — DSL or templates, and the first-render walk-through.
4. [examples/README.md](examples/README.md) — runnable examples with PDF previews.
5. [docs/recipes.md](docs/recipes.md) — patterns for common layouts.
6. [docs/troubleshooting.md](docs/troubleshooting.md) — symptom-first fixes for the common gotchas.
7. [docs/architecture/overview.md](docs/architecture/overview.md) and [docs/architecture/package-map.md](docs/architecture/package-map.md) — when extending the engine.
8. [docs/migration/v2.0.0-modules.md](docs/migration/v2.0.0-modules.md) — upgrading from the 1.x line; [CHANGELOG.md](CHANGELOG.md) for minor-to-minor steps.

## Response expectations

Best-effort. Triage typically within a week. Reports with minimal reproductions get priority. There is no paid support tier.

## Out of scope

- Hosted PDF rendering service. GraphCompose is a library; deploy it inside your application.
- Commercial integration support. Issues are public and best-effort.
- Modifications to third-party dependencies (PDFBox, Apache POI, etc.). Report those upstream.
- Correctness of generated content. The DSL renders what you describe — sanitising user-supplied data is the caller's responsibility.

## Contributing

If you want to contribute rather than ask for help, see [CONTRIBUTING.md](CONTRIBUTING.md). Lane structure and architecture rules: [docs/architecture/overview.md](docs/architecture/overview.md).
