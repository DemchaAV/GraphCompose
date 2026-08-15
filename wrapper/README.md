# GraphCompose (compat wrapper)

`io.github.demchaav:graph-compose`

The `graph-compose` coordinate — an **empty jar** that depends on `graph-compose-core` +
`graph-compose-render-pdf`. It exists so the 1.x coordinate keeps meaning "PDF out of the
box": a caller who upgrades to 2.0 with `graph-compose` on their classpath renders PDF with
**no code and no dependency change**.

## When to depend on it

This is the **default**. Depend on `graph-compose` for the drop-in PDF experience. Reach past
it only to go leaner (`graph-compose-core`, bring your own backend) or fuller
(`graph-compose-bundle`, batteries included). Templates are opt-in — add `graph-compose-templates`
if you used the built-in presets through the 1.x single jar.

Packaging stays `jar` (never `pom`) so existing `<dependency>` declarations need no `<type>`.

## Install

Same version as the rest of the GraphCompose train (lockstep):

```xml
<dependency>
    <groupId>io.github.demchaav</groupId>
    <artifactId>graph-compose</artifactId>
    <version>2.2.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.demchaav:graph-compose:2.2.0") }
```

The full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
