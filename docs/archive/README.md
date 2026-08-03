# Archived documentation

Documents in this folder describe earlier release planning or past
migration paths. They are kept as historical reference — do not link
to them from the public docs surface or from any user-facing material.

The current docs live under [`docs/`](../) at the repository root:

- [`docs/getting-started.md`](../getting-started.md) — quick start
  and core concepts.
- [`docs/recipes.md`](../recipes.md) and the
  [`docs/recipes/`](../recipes/) folder — copy-paste catalogues per
  feature area.
- [`docs/architecture/canonical-legacy-parity.md`](../architecture/canonical-legacy-parity.md) —
  living parity matrix.
- [`docs/architecture/overview.md`](../architecture/overview.md),
  [`docs/architecture/lifecycle.md`](../architecture/lifecycle.md),
  [`docs/architecture/package-map.md`](../architecture/package-map.md) — internal architecture
  references.

## What's archived

| File | Why archived |
| --- | --- |
| [`v1.2-roadmap.md`](v1.2-roadmap.md) | Roadmap planning notes for the v1.2 release. Superseded by `CHANGELOG.md` for shipped features and by the Phase A–F execution plan for in-flight work. |
| [`migration-v1-1-to-v1-2.md`](migration-v1-1-to-v1-2.md) | Migration guide from v1.1 to v1.2. Both versions are out of support; v1.4 → v1.5 callers should follow the migration doc at `docs/roadmaps/migration-v1-4-to-v1-5.md`. |
| [`implementation-guide.md`](implementation-guide.md) | Implementation notes for an engine model 2.0 removed. The current route for adding a node, a handler or a backend is [`docs/contributing/extension-guide.md`](../contributing/extension-guide.md), with [`docs/architecture/package-map.md`](../architecture/package-map.md) for where things live. |
