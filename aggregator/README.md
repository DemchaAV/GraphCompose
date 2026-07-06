# GraphCompose Build Aggregator

**Internal reactor module — not published to Maven Central.** Do not depend on it.

`aggregator/` (artifact `graph-compose-build`) is the non-published reactor parent that builds
the whole project in one pass — the engine and its sibling modules plus the `examples`,
`benchmarks`, and `qa` children. It exists to give a single build entry point and to host the
aggregator-child modules (examples / benchmarks / qa) that inherit their version from it.

The **published** artifacts (`graph-compose-core`, `graph-compose`, the render backends,
`graph-compose-templates`, `graph-compose-testing`, `graph-compose-bundle`) are standalone
poms — they do **not** use this aggregator as their Maven parent; it only lists them as
`<modules>` so a single reactor build compiles everything.

Build everything:

```bash
./mvnw -f aggregator/pom.xml clean verify
```

Module layout and per-package ownership: [docs/architecture/package-map.md](../docs/architecture/package-map.md).
See [CONTRIBUTING.md](../CONTRIBUTING.md) for the workflow.
