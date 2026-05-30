# GraphCompose GitHub Pages Showcase

This folder is the static showcase site for GraphCompose. It is plain HTML,
CSS, JavaScript, and a generated JSON manifest, so GitHub Pages publishes it
directly from the main repository with no build step.

## Files

- `index.html` — the single-page showcase. Holds the hero, install snippets,
  feature/architecture sections, and the searchable gallery shell.
- `styles.css` — the visual system and responsive layout.
- `examples.js` — client script that fetches the manifest and renders the
  gallery (search, category filters, highlights strip, lightbox).
- `examples.json` — **generated** gallery manifest. Do **not** hand-edit it;
  it is rewritten by `ShowcaseSync` (see below).
- `assets/logo/` — the showcase logo asset.
- `showcase/pdf/<category>/<group>/…` — generated PDF examples linked from the
  gallery.
- `showcase/screenshots/<category>/<group>/…` — PNG previews auto-rendered from
  those PDFs.

> Older revisions of this doc referenced `docs/assets/screenshots` and
> `docs/assets/pdf` plus a manual "copy the PDF, add a PNG, edit examples.json"
> flow. That is no longer accurate — previews and the manifest are generated
> under `docs/showcase/**` by `ShowcaseSync`.

## Add a new showcase item

The gallery is driven by code, not by hand-editing JSON. Source of truth:
`examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java`.

1. Add the example `.java` under the right category sub-package in
   `examples/src/main/java/com/demcha/examples/`.
2. Make it write its PDF via `ExampleOutputPaths.prepare(category, fileName)`
   so the output lands under the matching `examples/target/generated-pdfs/`
   subfolder.
3. Wire it into `GenerateAllExamples.main`.
4. Register a metadata entry in `ShowcaseMetadata.java` (title, one-line
   description, search tags, source link) keyed by the generated PDF basename.
5. Regenerate, then sync:

   ```bash
   ./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples
   ./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.support.ShowcaseSync
   ```

   `ShowcaseSync` copies each PDF into `docs/showcase/pdf/…`, rasterises a PNG
   into `docs/showcase/screenshots/…`, and rewrites `docs/examples.json`.
6. Commit the regenerated `docs/showcase/**` assets and `docs/examples.json`.

## Version and source links

- The displayed version lives **only** in `index.html` (JSON-LD
  `softwareVersion`, the JitPack download URL, the hero badge, and the Maven +
  Gradle install snippets). It does not inherit from the pom.
- `scripts/cut-release.ps1` flips all of those — plus the README and poms — to
  the release tag in the release commit, and `VersionConsistencyGuardTest`
  fails the `verify` gate if any of them drift out of sync with the library
  `pom.xml`. Do not hand-bump the site version ahead of a release.
- "View source" links resolve through `ShowcaseMetadata.GH_BASE`, which
  `cut-release.ps1` flips between `/blob/develop` (while developing) and
  `/blob/v<tag>` (at release) so the deployed site points at the exact source
  that produced each artefact.

## Local preview

The gallery uses `fetch("examples.json")`, which browsers block for local
`file://` pages. Run any tiny static server from the repository root and open
the `/docs/` path:

```bash
python -m http.server 8000
# then open http://localhost:8000/docs/
```

Opening `docs/index.html` directly still renders everything except the
JS-driven gallery (the `<noscript>` static index lists every PDF as a
fallback). No build step is required.

## GitHub Pages

Published from the `main` branch `/docs` folder (Settings → Pages → Deploy from
a branch → `main` / `/docs`). Live at:

https://demchaav.github.io/GraphCompose/
