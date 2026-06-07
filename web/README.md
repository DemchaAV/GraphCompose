# GraphCompose showcase site (`web/`)

The static GitHub Pages site for GraphCompose — plain HTML, CSS, JavaScript,
and a generated JSON manifest, served directly with **no build step**. It lives
**outside `docs/`** (which is documentation only) so the two never tangle.

## Files
- `index.html` — single-page showcase: hero, install snippets, feature /
  architecture sections, and the searchable gallery shell.
- `styles.css` — visual system and responsive layout.
- `examples.js` — client script that fetches the manifest and renders the gallery.
- `examples.json` — **generated** gallery manifest. Do **not** hand-edit it; it is
  rewritten by `ShowcaseSync` (see below).
- `robots.txt`, `sitemap.xml` — SEO.
- `assets/logo/` — site logo. (`assets/pdf` + `assets/screenshots` are legacy
  landing previews, superseded by `showcase/`; safe to prune.)
- `showcase/pdf/<category>/<group>/…` — generated example PDFs.
- `showcase/screenshots/<category>/<group>/…` — PNG previews of those PDFs.

## Regenerating the gallery
Driven by code, not hand-edited JSON. Source of truth:
`examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java`.

1. Add the example under `examples/src/main/java/com/demcha/examples/`, writing its
   PDF via `ExampleOutputPaths.prepare(category, fileName)`.
2. Wire it into `GenerateAllExamples.main`.
3. Register a metadata entry in `ShowcaseMetadata.java` keyed by the PDF basename.
4. Regenerate, then sync:

   ```bash
   ./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples
   ./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.support.ShowcaseSync
   ```

   `ShowcaseSync` copies each PDF into `web/showcase/pdf/…`, rasterises a PNG into
   `web/showcase/screenshots/…`, and rewrites `web/examples.json`.
5. Commit the regenerated `web/showcase/**` + `web/examples.json`.

## Version + source links
- The displayed version lives **only** in `index.html` (JSON-LD `softwareVersion`,
  the Maven Central URL, the hero badge, and the Maven + Gradle snippets) — it does
  not inherit from the pom. `scripts/cut-release.ps1` flips it on release and
  `VersionConsistencyGuardTest` fails the `verify` gate if it drifts.
- "View source" links resolve through `ShowcaseMetadata.GH_BASE`, which
  `cut-release.ps1` flips between `/blob/develop` and `/blob/v<tag>` at release.

## Deploy
Published to GitHub Pages by **`.github/workflows/deploy-web.yml`** (GitHub Actions),
which uploads this `web/` folder on pushes to `main`. Pages must be set to
**Settings → Pages → Source: GitHub Actions** — that one-time switch replaced the old
branch-deploy from `/docs` when the site moved out of `docs/`.

Live: https://demchaav.github.io/GraphCompose/

## Local preview
The gallery uses `fetch("examples.json")`, which browsers block over `file://`.
Run a static server from this folder:

```bash
python -m http.server 8000   # then open http://localhost:8000/
```