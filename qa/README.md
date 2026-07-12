# GraphCompose QA

**Internal module — not published to Maven Central.** Do not depend on it.

`qa/` (artifact `graph-compose-build`'s `graph-compose-qa` child) holds the **cross-module
test suites and shared fixtures** that cannot live in any single module's own test scope —
tests that need a render backend plus the engine test-jar, or that span core + render-pdf +
templates at once. It is a reactor tail: an aggregator child that depends on the published
modules at test scope, so heavy render/integration tests run here without creating a reactor
cycle back into the lean core.

Examples of what lives here: template visual-parity and smoke suites, layout-snapshot
extraction tests, the documentation snippet-compile guard, and the dev-tool launcher.

Run it as part of the reactor:

```bash
./mvnw test        # or: ./mvnw verify -pl :graph-compose-qa -am
```

See [CONTRIBUTING.md](../CONTRIBUTING.md) for the build/test workflow.
