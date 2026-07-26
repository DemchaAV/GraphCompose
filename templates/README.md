# GraphCompose Templates

`io.github.demchaav:graph-compose-templates`

The built-in CV, cover-letter, invoice, and proposal document templates
(`com.demcha.compose.document.templates.**`). Pure authoring code over the canonical DSL —
no engine internals, no PDFBox — so it depends only on `graph-compose-core`.

## When to depend on it

Add it when you want the ready-made presets. It is **opt-in**: neither the `graph-compose`
wrapper nor `graph-compose-core` bundles it (`graph-compose-bundle` does). A 1.x caller that
reached a preset through the single jar must add this artifact — the packages are unchanged.

## Usage

Each preset is a final class with a `create(BrandTheme)` factory returning a
`DocumentTemplate<S>` you compose into an open session:

```java
try (var doc = GraphCompose.document(out).create()) {
    ModernInvoice.create(theme).compose(doc, invoiceSpec);   // needs graph-compose-templates
}
```

Families: `cv`, `coverletter`, `invoice`, `proposal` — each a layered stack (data / components
/ widgets / presets) over the shared `core` layer (`BrandTheme`, identity, text, widgets).
See the [layered-template guides](../docs/templates/v2-layered/README.md).

## Install

Same version as the rest of the GraphCompose train (lockstep):

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-templates</artifactId>
    <version>2.1.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-templates:2.1.0") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
