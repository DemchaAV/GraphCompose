# AGENTS.md

Operating rules for coding agents working on GraphCompose.

GraphCompose is a modular Java document layout engine, not a generic Java
application. Its architecture, module boundaries, public API tiers and
deterministic layout behaviour are documented — read them before changing them.
This file does not restate them. It covers what an agent needs that those
documents do not say, and points at the rest.

## Read first

- [`CONTRIBUTING.md`](CONTRIBUTING.md) — contribution flow, branch policy, Java
  baseline, repository map, architecture lanes, testing expectations
- [`docs/architecture/overview.md`](docs/architecture/overview.md) — the
  authoring pipeline and where each stage's responsibility ends
- [`docs/architecture/package-map.md`](docs/architecture/package-map.md) —
  package ownership and module layout
- [`docs/contributing/extension-guide.md`](docs/contributing/extension-guide.md)
  — how to add a node, builder, definition or backend handler

Read when the change reaches them:

- [`docs/api-stability.md`](docs/api-stability.md) — before changing public API
- [`knowledge/api/`](knowledge/api/) — what the public API *is*, generated from
  the compiled classes. Ask it rather than read it:
  `node knowledge/tools/api-query/api-query.mjs --exists Type.method`
  (exit 0 found, 3 absent). Split by surface — `authoring` is the one a
  compose task wants; `backends`, `templates`, `testing` and `extension-spi`
  are opt-in.
- [`docs/architecture/backend-capability-matrix.md`](docs/architecture/backend-capability-matrix.md)
  — before changing what a PDF, PPTX or DOCX export can do, and to record it
- [`docs/adr/`](docs/adr/) — before revisiting a decision already taken
- [`docs/operations/benchmarks.md`](docs/operations/benchmarks.md) — for
  layout, render or performance work
- [`docs/contributing/release-process.md`](docs/contributing/release-process.md)
  — for release work

These documents are the source of truth. Do not copy them into code comments,
and do not answer from memory of how the engine used to work: prose describing
an architecture this repository no longer has once survived a full release line,
green under every guard.

## Stay inside the task

Do not run release scripts, create tags, change versions, or edit release
metadata unless the task is release work. A cut is not a side effect of a
feature branch.

Do not refactor code the task does not require. Keep a refactor and a behaviour
change in separate commits when both are genuinely needed.

Before adding a class, builder, abstraction, render handler or template
component, search for the extension point that already exists. A parallel
abstraction is cheaper to write and more expensive than anything it saves.

## Working tree hygiene

Stage explicit paths — `git add <path> …`. Never `git add .` or `git add -A`.

The tree accumulates zero-byte files with names like `$env`, `` b)` `` or
`'Current`, left by shell quoting accidents rather than by intent. They are
invisible in a summary and permanent once committed. Staging by path cannot pick
them up.

Read `git status --porcelain --untracked-files=all` before committing and remove
such files by name. Do not run `git clean -f`: it deletes new source files that
have not been staged yet along with the junk.

## Regenerate the knowledge pack with the API change

`knowledge/api/*.json` and their Markdown views are generated from the compiled
classes and committed. Treat them exactly like a lockfile: **a PR that changes
public API regenerates the pack in the same commit.**

```bash
./mvnw -q -DskipTests install -pl :graph-compose-core,:graph-compose-templates,:graph-compose-render-pdf,:graph-compose-render-docx,:graph-compose-render-pptx,:graph-compose-testing -am
node knowledge/tools/api-surface/extract-api.mjs --from-reactor
```

CI runs `--check` on the same input and fails the build when the committed pack
does not match the tree. That failure is not a flake and is not the gate being
strict: it means the surfaces on disk describe an API that no longer exists.

Never hand-edit a generated file to make the check pass. The generator is the
thing to fix — a hand-edit survives until the next regeneration and, in the
meantime, makes the file look verified when it is not. That is not hypothetical:
this pack exists because a generated allow-list was wrong for an entire major
version while being quoted to agents as authoritative.

If a public type is new and the run fails with *"matched no classification
rule"*, the classifier is telling you it does not know whether the type is API.
Add a rule in `knowledge/tools/api-surface/lib/surfaces.mjs` — the surface it
belongs to, or an exclusion with a written reason. Do not widen a package prefix
to make the message go away.

## Verification

Run the smallest relevant tests while working:

```
./mvnw -B -ntp verify -pl :graph-compose-core
```

Before reporting the work complete:

```
./mvnw -B -ntp clean verify
```

`CONTRIBUTING.md` lists the guard suites and the documentation guards in the
`qa` module, including the install a standalone `-f qa/pom.xml` run needs first.

Do not weaken, delete or bypass an existing regression test to make a change
pass, unless the behaviour it pins deliberately changed.

**Never report that a test passed unless it was executed and observed to pass.**
Report any command that could not be run, and why.

### Two results that mislead rather than fail

Both of these return success while telling you the opposite of the truth.

**A standalone examples run resolves from `~/.m2`.** The full reactor builds
`examples` from source, but `cd examples && ../mvnw compile exec:java …` does
not — it resolves `graph-compose-*` from the local repository, exactly as a
standalone `-f qa/pom.xml` run does. After changing a render backend, install
first:

```
./mvnw -B -ntp -DskipTests install
```

Without it the examples regenerate through the artifacts you last installed, and
the output looks unchanged because the change was never in it.

**Example output is not byte-comparable.** Rendering the same example twice
produces different bytes — the writers embed timestamps. Comparing a fresh
render against the committed preview under `assets/readme/` therefore reports a
difference for every example, including ones the change cannot reach. To decide
whether a render change owes regenerated previews, establish the changed code
path's trigger condition and find the examples that meet it; the
visual-regression baselines in `qa` are the independent check.

## Before finishing

1. Read the final diff, and confirm nothing unrelated changed.
2. Confirm the documentation the change touches was updated with it — a
   capability matrix row, an example, a package map entry.
3. Run the focused tests, then the full reactor when feasible.
4. State what was run, and what was not.
