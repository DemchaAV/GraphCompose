# `knowledge/tools` — the authoritative API-surface generator

This directory holds the generator for GraphCompose's machine-readable API
contract and the CLI that queries it. **This is the upstream copy.** AI Flow
syncs *from* here; nothing here is a mirror of anything.

## Why it lives in this repository

GraphCompose's public API was, until 2026-08-31, described authoritatively by a
tool in a *different* repository — while this repo shipped a regex parser
(`.llm-wiki/tools/api-index/api-index.py`) whose output was wrong and was
nonetheless quoted to agents as a closed set. A library's own API surface is the
library's to define. That is what moving the generator here fixes.

## What is here

| Path | Job |
|---|---|
| `api-surface/extract-api.mjs` | reads class files with `javap`, writes the surface JSON and its Markdown view |
| `api-surface/lib/javap.mjs` | drives `javap` and normalises what it prints |
| `api-surface/lib/source-names.mjs` | lifts real parameter names out of a sources jar |
| `api-surface/lib/zip.mjs` | reads a jar without unpacking it |
| `api-surface/lib/render-markdown.mjs` | renders the Markdown view *from the JSON* |
| `api-query/api-query.mjs` | answers a question about the surfaces instead of making you read them |

## The rules

**Never hand-edit generated output.** `knowledge/api/*.json` and the Markdown
views beside them are outputs. If an entry looks wrong, the generator is wrong —
fix it here and regenerate. A hand-edit survives exactly until the next run, and
in the meantime it makes the file look verified when it is not. This is not a
hypothetical: the defect that motivated this whole directory recurred through an
entire major version because the generated file kept being regenerated from an
unfixed generator.

**The Markdown is a view, not a source.** `render-markdown.mjs` renders it from
the JSON, so the two cannot disagree. Read the JSON, or ask `api-query`.

**The JSON is the contract.** Anything downstream — the CI drift gate, the
published bundle, AI Flow's version packs — reads the JSON.

## Querying

```powershell
node knowledge\tools\api-query\api-query.mjs --type GraphCompose
node knowledge\tools\api-query\api-query.mjs --exists GraphCompose.DocumentBuilder.pageSize
node knowledge\tools\api-query\api-query.mjs --search timeline
node knowledge\tools\api-query\api-query.mjs --surface backends --package com.demcha.compose.document.backend.fixed.pptx
```

Exit codes are meant to be branched on: `0` found, `3` nothing matched, `2`
usage. A `3` is a real answer — it means the symbol does not exist — and it is
the answer that stops an invented call.

## Regenerating

```powershell
node knowledge\tools\api-surface\extract-api.mjs --from-release 2.2.0
```

`--check` compares against what is on disk and exits `1` on drift instead of
writing. That is what CI runs.

## Provenance and carried-forward work

`extract-api.mjs` and its `lib/` were copied **byte-for-byte** from
`C:\Dev\projects\graphcompose-ai-flow\tools\api-surface\` on 2026-08-31, so that
any later difference in behaviour is attributable to a deliberate edit rather
than to transcription. Both copies were run against 2.2.0 and produced
byte-identical `api-surface.json` and `00-api-surface.md`.

Phase 3 then rewrote the classification and output layers on top of it:
`lib/annotations.mjs` and `lib/surfaces.mjs` are new, `buildSurface` classifies
rather than filtering by a package list, and output is one document per surface
instead of one flat allow-list. The output paths, the emitted `generator:` field
and the `--check` failure hint now name this repository.

Phase 4 added the second input mode and split the bookkeeping. `--pack` survives
as a deprecated alias for `--out` so the AI Flow invocation keeps working; there
is nothing else carried forward.

See `docs/private/PLAN-knowledge-pack.md`.

## Two inputs, and why both exist

```powershell
node knowledge\tools\api-surface\extract-api.mjs --from-reactor
node knowledge\tools\api-surface\extract-api.mjs --from-release 2.2.0
```

`--from-release` resolves published jars through Maven; it is how a bundle for a
shipped version is built. `--from-reactor` reads this working tree's
`*/target/classes`, and it is what CI runs: **a gate that checks the last release
tells you nothing about the commit under review.** The two are mutually
exclusive — silently preferring one would let a gate check the wrong artifact.

Reactor mode needs the modules built. A module that is missing is fatal rather
than skipped, because a surface quietly short one module is the exact failure
this pack exists to prevent; the error names the build command.

Parameter names come from the sources jar in release mode and from
`src/main/java` in reactor mode, where no sources jar exists. Reading the source
tree was chosen over compiling with `-parameters`: that flag would change every
published class file to serve a documentation tool.

## manifest vs provenance

Four files, four jobs, and the first two must never be merged:

| File | Tracked | Holds |
|---|---|---|
| `knowledge/manifest.json` | **yes** | schema, version line, generator version, the surface list, the classification rules |
| `target/knowledge/provenance.json` | **no** | git commit, branch, dirty flag, timestamp, per-artifact SHA-256 |

A tracked file cannot hold the SHA of the commit it is part of — the value is
false the instant it is committed. Excluding the field from `--check` would not
make it true, only unenforced, so the split is physical rather than a rule about
which fields to compare.

The digests are provenance, not identity. Class files are not byte-reproducible
across machines and JDK builds, so a gate comparing them would be red on every
run no matter what the API did. `--check` compares the surfaces and the manifest;
it never reads provenance, and regenerating twice a minute apart leaves it green.
What provenance is for is telling two builds of the same `2.2.1-SNAPSHOT` apart
after the fact, which the version string alone cannot do.

`target/` is already gitignored, so provenance stays uncommitted without a rule
of its own.

## Classification

`lib/surfaces.mjs` holds the rules, and they run in three stages, because
admission and stability are different questions: **`@Beta` says how mature a
type is, never whether it is API at all.**

- **A — admission.** First match wins: type `@Internal` → out; on the Extension
  SPI list → in, *even from an `@Internal` package*; package `@Internal` → out;
  explicit exclusion rule → out with a reason; surface rule → in; nothing
  matched → **the run fails**.
- **B — stability.** Type `@Beta`, else package `@Beta`, else stable. Resolved
  per type including each nested type separately: `DocumentPaint` is stable while
  the records nested inside it are beta.
- **C — members.** Member `@Internal` → dropped; member `@Beta` → beta at member
  level; otherwise inherit the type.

Two things follow from this that are easy to get wrong:

**The SPI list is authored, not derived.** `docs/api-stability.md:33-34` gives
Extension SPI and Experimental the *same* `@Beta` annotation and says the
distinction lives in the docstring. No byte-code signal separates them, so no
classifier can route between `extension-spi` and `backends` on its own.

**Reachability decides the engine tree, and it runs after the rules, not
before.** `com.demcha.compose.engine.components` is not engine internals despite
the name — it holds `TextStyle`, `DocumentMetadata`, `ImageData`, `Padding`,
`Anchor`: value types the public API makes you construct. They are admitted
because admitted signatures mention them. Their engine-side twins
(`HeaderFooterConfig` beside the public `DocumentHeaderFooter`,
`WatermarkConfig` beside `DocumentWatermark`) are mentioned by nothing and are
excluded. Putting an exclusion rule on `engine.*` up front would drop both.

Every exclusion is written to `knowledge/api/excluded.json` with its reason. An
exclusion nobody can see is indistinguishable from a bug.

`api-query.mjs` is *not* a verbatim copy. It was rewritten thin against
`knowledge/api/*.json`: AI Flow's `--version` / `--project-dir` pack-pinning was
dropped (this repo describes one tree, not several released lines), the Markdown
fallback parser was dropped with it (it exists there for packs predating the
extractor), and `--surface` was added because here the API is split by surface
rather than by version.

## The release bundle

```powershell
node knowledge\tools\bundle\build-bundle.mjs --verify
```

A tracked `knowledge/` directory is not a published pack. Without an archive,
"the plugin consumes GraphCompose's knowledge" quietly means "the plugin needs a
GraphCompose checkout on the machine" — the offline problem restated, not solved.

`graph-compose-knowledge-<version>.zip` carries `manifest.json`,
`provenance.json`, `api/`, `routing/`, `claims/`, a `README.md`, and
`bin/query.mjs` — **a query CLI of its own**. That last part is not convenience.
The acceptance test is that the pack answers on a machine with no GraphCompose
source; without a bundled `--exists` / `--task` there is nothing on that machine
to run the test *with*, so "self-sufficient" would be asserted rather than
demonstrated. `--verify` unpacks the archive into a scratch directory well away
from the repository and queries it there.

Two levels of checksum, and the outer one lives outside:

| File | Where | Answers |
|---|---|---|
| `<bundle>.zip.sha256` | beside the archive | is the archive intact |
| `bundle-checksums.json` | inside the archive | *which* entry is wrong |

A checksum stored inside the archive it describes verifies nothing, because
whatever rewrote the archive rewrote it too — so the outer hash is published as a
separate release asset.

The archive is written by `lib/zip-write.mjs`, the companion to the `zip.mjs` the
extractor already uses for reading, and every build round-trips the result back
through that reader before reporting success. Timestamps inside are fixed rather
than taken from the clock: two builds of the same content must produce the same
archive, or the checksum beside it would describe the moment it was built instead
of what is in it.

**Distribution is a GitHub Release asset, not a Maven artifact.** Maven would
give a stable coordinate and dependency resolution, but the consumer is a Node
plugin that resolves nothing through Maven, and no Java build compiles against a
documentation pack. It would mean adding an artifact to the Central staging path
— and its allow-list — for a file nothing on that path needs.
