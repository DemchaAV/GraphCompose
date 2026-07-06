# GraphCompose Bundle

`io.github.demchaav:graph-compose-bundle`

The batteries-included aggregate (pom-packaged). One dependency pulls the default PDF stack
(the `graph-compose` wrapper = core + render-pdf), the built-in templates
(`graph-compose-templates`), the bundled Google fonts (`graph-compose-fonts`), and the
colour-emoji set (`graph-compose-emoji`) at compatible versions.

## When to depend on it

Depend on it when you want everything wired up in one coordinate — the closest thing to the
pre-split single jar. The office backends (`graph-compose-render-docx` /
`graph-compose-render-pptx`) stay opt-in and are **not** bundled.

Because it is `pom`-packaged, a dependency on it needs `<type>pom</type>`.

## Install

The engine/templates track the GraphCompose train version (lockstep); `graph-compose-fonts`
and `graph-compose-emoji` are pinned to compatible independent versions. Copy-paste snippet
and the full "which artifact?" table: [root README → Installation](../README.md#installation).
Upgrading from 1.x: [modules migration guide](../docs/migration/v2.0.0-modules.md).
