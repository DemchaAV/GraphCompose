# GraphCompose release process

This checklist applies to v2.x releases of GraphCompose. The library is published via two channels:

- Maven Central — `io.github.demchaav:GraphCompose:<version>` (planned)
- JitPack — `com.github.DemchaAV:GraphCompose:v<version>` (current)

## Pre-release

- [ ] all CI tests green on `main` (`./mvnw test`) — the full architecture-guard suite included
- [ ] `CHANGELOG.md` has an entry for the upcoming version under `## v<version> - <date>` with the date set to release day
- [ ] `pom.xml` and `examples/pom.xml` `<version>` fields match the target release version (no `-SNAPSHOT` suffix)
- [ ] `README.md` installation snippets reference the new version
- [ ] migration notes (`docs/migration-v1-to-v2.md` for v2.x, or a new file for later majors) are up to date
- [ ] roadmap (`docs/v2-roadmap.md`) reflects what shipped vs. what remains for the next milestone

## Release

- [ ] commit the version bump and CHANGELOG entry on `main`
- [ ] tag the release: `git tag v<version> && git push origin v<version>`
- [ ] create a GitHub Release with:
  - title `GraphCompose v<version>`
  - body copied from the matching `CHANGELOG.md` section
  - "Generated release notes" disabled (we author the changelog by hand)
- [ ] verify JitPack picked up the tag: `https://jitpack.io/com/github/DemchaAV/GraphCompose/v<version>/build.log` returns success
- [ ] announce the release where appropriate (pinned README badge already points at JitPack)

## Post-release

- [ ] run the runnable examples module against the new artifact:

```bash
./mvnw -pl examples -am package
java -jar examples/target/examples-2.0.0.jar
```

- [ ] confirm the public API surface is unchanged for patch releases — `PublicApiNoEngineLeakTest` and `SemanticLayerNoPdfBoxDependencyTest` should pass without allowlist edits
- [ ] open a follow-up issue for any deferred roadmap work that did not make this release

## Hotfixes

For a `vX.Y.Z+1` patch release, follow the same flow but limit the change set to:

- bug fixes
- documentation corrections
- security updates

Do not introduce new public API in a patch release. If a fix needs new types or methods, ship a minor version (`vX.Y+1.0`) instead.
