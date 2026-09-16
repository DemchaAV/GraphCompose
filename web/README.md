# GraphCompose showcase site (`web/`)

The static GitHub Pages site for GraphCompose — plain HTML, CSS, JavaScript,
and a generated JSON manifest, served directly with **no build step**. It lives
**outside `docs/`** (which is documentation only) so the two never tangle.

## Files
- `index.html` — single-page showcase: hero, install snippets, feature /
  architecture sections, and the searchable gallery shell.
- `styles.css` — visual system and responsive layout.
- `examples.js` — client script that fetches the manifest and renders the gallery.
  It also resolves the anchors the menu and the sitemap link to (`#showcase`,
  `#<category>-section`) by selecting that category's filter first: a category
  section exists only while its filter is shown.
- `gallery-viewer.js` — the viewer the gallery opens: one family at a time, at
  `#/<category>/<group>/<id>`. `scripts/site/gallery-viewer.test.mjs` tests its addresses
  and navigation in CI's guard job.
- `examples.json` — **generated** gallery manifest. Do **not** hand-edit it; it is
  rewritten by `ShowcaseSync` (see below).
- `robots.txt`, `sitemap.xml` — SEO.
- `assets/logo/` — site logo. (`assets/pdf` + `assets/screenshots` are legacy
  landing previews, superseded by `showcase/`; safe to prune.)
- `showcase/pdf/<category>/<group>/…` — generated example PDFs.
- `showcase/screenshots/<category>/<group>/…` — PNG previews of those PDFs.
- `showcase/thumbnails/<category>/<group>/…` — the same first pages at 320px wide, which
  the viewer's strip reads. A strip slot is 54px, 46px on a narrow screen: pointed at the
  previews above, it would pull a whole page per slot.
- `showcase/pptx/<category>/<group>/…` — the PowerPoint decks of the examples that
  also render one.

## Regenerating the gallery
Driven by code, not hand-edited JSON. Source of truth:
`examples/src/main/java/com/demcha/examples/support/ShowcaseMetadata.java`.

1. Add the example under `examples/src/main/java/com/demcha/examples/`, writing its
   PDF via `ExampleOutputPaths.prepare(category, fileName)`.
2. Wire it into `GenerateAllExamples.main`.
3. Register a metadata entry in `ShowcaseMetadata.java` keyed by the PDF basename.
4. Regenerate, then sync:

   ```bash
   ./mvnw -B -ntp -DskipTests install
   ./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.GenerateAllExamples
   ./mvnw -f examples/pom.xml exec:java -Dexec.mainClass=com.demcha.examples.support.ShowcaseSync
   ```

   The `install` is not optional: the examples module resolves the engine, templates and
   backend jars from your local repository, not from the working tree, so without it the two
   `exec:java` runs stop at dependency resolution — and with a stale one they would render
   the last-installed code.

   `ShowcaseSync` copies each PDF into `web/showcase/pdf/…`, rasterises a PNG into
   `web/showcase/screenshots/…` and a 320px thumbnail into `web/showcase/thumbnails/…`, and
   rewrites `web/examples.json`.

   The manifest carries a `schemaVersion`, and each card carries what the register knows
   (`kind`, `sourcePath`, `requiredArtifacts`, and `presetClass` + `dataModel` where the
   example builds exactly one preset, plus `variantOf` where it re-renders another card's)
   alongside what rendering it measured (`previewWidth`, `previewHeight`, `pageCount`).
   The preset and model on a card are held to the example that renders them by
   `ShowcasePresetRegistrationTest` in the examples module.
5. Commit the regenerated `web/showcase/**` + `web/examples.json`.

## What a release changes here
`scripts/cut-release.ps1` is what edits this folder at a release; no CI workflow
writes to it.

- **Version.** The displayed version lives only in `index.html`, in five places the
  script rewrites by pattern on a final release: the JSON-LD `softwareVersion`, the
  Maven Central `downloadUrl`, the hero badge (`Java &middot; v… &middot; MIT`), and
  the Maven and Gradle snippets for `graph-compose`. Keep each in its current shape:
  a place the patterns no longer match is left unchanged. `VersionConsistencyGuardTest`
  holds four of them to the release — every one but the `downloadUrl` — so a stale one
  of those fails the cut's verify gate. A pre-release cut leaves all five on the last
  published version, and so does the post-release bump.
- **Catalogue.** Unless run with `-SkipShowcase`, the cut sets
  `ShowcaseMetadata.GH_BASE` to `/blob/v<version>` and runs `ShowcaseSync`, so the
  release commit carries a regenerated `examples.json` and `showcase/` whose source
  links name the tag. `-PostReleaseOnly` sets them back to the branch it runs from
  (`/blob/develop` by default).
  Between releases the committed catalogue is the last sync: an example added on
  `develop` appears here at the next cut.

## Checks
`ShowcaseSiteGuardTest` runs in CI's guard job and fails when:

- an id in the featured list of `examples.js` is not a card in `examples.json` —
  the page would skip it without a sign;
- a card names a PDF, preview or deck that is not a file under `showcase/`;
- `index.html`, `sitemap.xml` or `robots.txt` links to a site file that is not here;
- a `#<category>-section` anchor or a filter pill names a category `examples.json`
  does not have, a `#/<category>/<group>[/<id>]` viewer address names a family or card
  it does not have, or another anchor names no element in `index.html`;
- a card, family or category id repeats, or would need escaping in a viewer address;
- `examples.json` is not strict JSON, which the page's `fetch` would refuse as well.

## Deploy
Published to GitHub Pages by **`.github/workflows/deploy-web.yml`** (GitHub Actions),
which uploads this `web/` folder as committed on every push to `main` — at a release,
the fast-forward of `main` after the tag. Pages must be set to
**Settings → Pages → Source: GitHub Actions** — that one-time switch replaced the old
branch-deploy from `/docs` when the site moved out of `docs/`.

The deploy does not wait for Maven Central. Each Central deployment is published by
hand, so a push to `main` before that step shows install snippets for a version that
does not resolve yet.

Live: https://demchaav.github.io/GraphCompose/

## Local preview
The gallery uses `fetch("examples.json")`, which browsers block over `file://`.
Run a static server from this folder:

```bash
python -m http.server 8000   # then open http://localhost:8000/
```
