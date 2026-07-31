# Security Policy

GraphCompose is a Java library for generating PDF documents. This policy covers vulnerabilities in the library itself: layout engine, PDF/DOCX/PPTX backends, template rendering, and any code shipped in published artifacts.

## Supported versions

Security fixes are issued for the latest minor release. Older minors do not receive backports unless the issue is severe and the upgrade path is non-trivial.

| Version | Supported |
|---------|-----------|
| 2.1.x   | Yes — actively patched |
| 1.9.x   | Critical fixes and security backports only, on the `1.x` branch — no features |
| < 1.9   | No — upgrade required (see [CHANGELOG.md](CHANGELOG.md) for per-version migration notes, and the [2.0 modules migration guide](docs/migration/v2.0.0-modules.md) for the 1.x → 2.x step) |

## Reporting a vulnerability

Do not open a public GitHub issue for security reports.

Use GitHub's private vulnerability reporting:
<https://github.com/DemchaAV/GraphCompose/security/advisories/new>

Include:

- Affected version(s).
- Minimal reproduction (a small Java snippet or PDF/DOCX input is ideal).
- Impact assessment (denial of service, information disclosure, arbitrary file write, RCE via crafted input, etc.).
- Suggested remediation, if you have one.

## Response timeline

Best-effort, solo-maintained project. Realistic targets:

| Stage | Target |
|-------|--------|
| Acknowledge report | within 5 business days |
| Triage and severity assignment | within 14 days |
| Fix in supported minor | next patch release (typically within 30 days for high/critical) |
| Public disclosure | after a fix ships, or 90 days from acknowledgement — whichever is sooner |

Critical issues may compress this timeline. Embargo terms are negotiated case-by-case via the GitHub security advisory thread.

## Scope

In scope:

- Layout engine (`com.demcha.compose.document.layout`).
- PDF backend (`com.demcha.compose.document.backend.fixed.pdf`).
- PPTX backend (`com.demcha.compose.document.backend.fixed.pptx`).
- Semantic export backends (`com.demcha.compose.document.backend.semantic`) — DOCX, and the legacy PPTX manifest.
- Templates shipped in `com.demcha.compose.document.templates`.
- Public authoring API (`GraphCompose`, `DocumentSession`, DSL).
- Build and release artifacts on Maven Central (`io.github.demchaav:graph-compose`). The legacy JitPack URL remains available for consumers pinned to v1.6.5 and earlier but is no longer the documented install channel.

Out of scope:

- Vulnerabilities in third-party dependencies (PDFBox, Apache POI, Jackson, Flexmark, SnakeYAML, ZXing, SLF4J, Lombok). Report those upstream. GraphCompose tracks dependency CVEs via Dependabot and will issue a release if a transitive issue requires action.
- Downstream applications consuming GraphCompose. Report to the application owner.
- Correctness of generated content. The document author is responsible for sanitising user-supplied data before passing it into the DSL.

## Hardening notes for consumers

- Treat user-supplied input as untrusted before injecting it into the DSL. Long strings, control characters, oversize tables, and pathological pagination inputs can affect render time and memory.
- For server-side rendering, scope `DocumentSession` to a single request and close it via try-with-resources. See [docs/architecture/lifecycle.md](docs/architecture/lifecycle.md).
- Production rendering guidance: [docs/operations/production-rendering.md](docs/operations/production-rendering.md).
