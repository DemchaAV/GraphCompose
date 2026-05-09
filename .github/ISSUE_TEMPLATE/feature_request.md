---
name: Feature request
about: Propose a new feature, public API addition, or template
title: "[FEATURE] "
labels: enhancement
---

## Use case

<!-- What document-authoring scenario is harder than it should be today?
     Be concrete: a sentence or two about the document being produced and
     where the friction is (e.g. "invoices need a watermarked total cell
     that wraps long currency strings — currently I have to compose two
     separate cells and merge them by hand"). -->

## Proposed API

```java
// Sketch the public-API shape you'd like to use.
// Stick to the canonical surface: GraphCompose.document(...), DocumentSession,
// document.pageFlow(...), DocumentNode + NodeDefinition, BusinessTheme.
document.pageFlow().add???(...);
```

## Alternatives considered

<!-- Workarounds you've tried, related issues, prior art in iText / JasperReports / openhtmltopdf, etc. -->

## Would this be a breaking change?

- [ ] No — additive only (new node, new builder method, new template preset).
- [ ] Possibly — affects public-record shape, deprecates an existing API, or changes default rendering behaviour.

## Optional: ADR-worthy?

If this is a structural change (sealed hierarchy, new sub-package, new SPI), please describe the architectural impact briefly. The maintainer may ask for a draft ADR under `docs/adr/` before implementation.
