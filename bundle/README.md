# GraphCompose Bundle

`io.github.demchaav:graph-compose-bundle`

The batteries-included aggregate. One dependency pulls the default PDF stack
(the `graph-compose` wrapper = core + render-pdf), the built-in templates
(`graph-compose-templates`), the bundled Google fonts (`graph-compose-fonts`), and the
colour-emoji set (`graph-compose-emoji`) at compatible versions.

## When to depend on it

Depend on it when you want everything wired up in one coordinate — the closest thing to the
pre-split single jar. The office backends (`graph-compose-render-docx` /
`graph-compose-render-pptx`) stay opt-in and are **not** bundled.

Packaged as an empty `jar` carrying only `<dependencies>` (like the `graph-compose` wrapper),
so a plain `<dependency>` works — no `<type>pom</type>` needed.

## Install

Tracks the GraphCompose train version (lockstep); the fonts and emoji it carries are pinned
to compatible independent versions:

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose-bundle</artifactId>
    <version>2.2.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose-bundle:2.2.0") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
