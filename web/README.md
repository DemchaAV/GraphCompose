# GraphCompose showcase site (`web/`)

The static GitHub Pages site for GraphCompose — plain HTML, CSS, JavaScript,
and a generated JSON manifest. GitHub Pages serves this folder **exactly as
committed** and runs nothing, so what is here is what ships. It lives
**outside `docs/`** (which is documentation only) so the two never tangle.

The pages are **generated**: `index.html`, `sitemap.xml` and one page per
document in the catalogue (`<category>/<group>/<id>/index.html`) are rendered
from `web-src/` by `node scripts/site/build.mjs`. Everything else — the
stylesheet, the scripts, the assets and the whole `showcase/` tree — is static:
the build writes none of it, and reads from `showcase/` only the pixel size the
later page images and the thumbnails state in their PNG headers. Do not hand-edit
a generated page:
`scripts/site/build.test.mjs` fails when what is committed here is not what
`web-src/` builds, and CI's guard job runs it.

## Files
- `index.html` — **generated** from `web-src/pages/index.html`. Single-page
  showcase: hero, install snippets, feature / architecture sections, and the
  searchable gallery shell. What comes from data rather than from the template:
  the release the page advertises (from `web-src/data/release.json`: the seven spots the
  version guard holds, the release named above the install scenarios and every coordinate in them — held to the
  release by `scripts/site/build.test.mjs`, since the guard reads the bare `graph-compose`
  coordinate alone — and the latest-release link in the documentation block),
  the hero's documents, the JSON-LD item list and the sitemap's documents (names are editorial, in
  `web-src/data/featured.json`; the URLs are resolved from the manifest so a
  renamed PDF cannot leave a crawler pointed at nothing), the preset counts, and
  the no-JavaScript index of every document in the catalogue, each linking to its page.
  The site header, the footer and the theme scripts are partials in `web-src/partials/`,
  shared with the document pages.
- `<category>/<group>/<id>/index.html` — **generated** from `web-src/pages/document.html`, one
  per card, at the same three segments as the card's viewer address: everything the viewer
  shows of a document, at an address of its own and readable without JavaScript. Every page of
  the document as an image at its own pixel size, each linking into the PDF (at that page, in the
  viewers that read `#page=`); the PDF and, where one is published, the deck; the reproduction
  panel, open; and the other documents of its family. Each page carries a canonical URL, a title,
  a description, Open Graph tags and a small JSON-LD block, and the sitemap lists them all. A
  family's link on a page is its viewer address, which without JavaScript lands on that family's
  list in the home page's no-JavaScript index. **The build owns these pages and only these:** a
  page carrying `<meta name="generator" content="GraphCompose site build">` *and* a canonical URL
  naming the place it sits — so a generated page copied elsewhere to start a page by hand is not
  the build's. An owned page no card builds any more — a card renamed, moved or removed — is
  deleted with the directories it leaves empty, and fails `--check` until it is.
- `styles.css` — visual system and responsive layout.
- `examples.js` — client script that fetches the manifest and renders the gallery.
  It also resolves the anchors the menu and the sitemap link to (`#showcase`,
  `#<category>-section`) by selecting that category's filter first: a category
  section exists only while its filter is shown.
- `gallery-viewer.js` — the viewer the gallery opens: one family at a time, at
  `#/<category>/<group>/<id>`. A document of several pages is paged through in place —
  "Page x of N" under the stage, while the arrows, the keys and a swipe keep moving between
  documents. A disclosure under that opens what reproducing the document takes: the
  coordinates at the release the page names, the preset and model a card composes, its
  family's compiled snippet, the run command, and the source and guide at the release tag.
  It is collapsed by default, because open it took twice the room of the document it
  describes; Details beside the PDF link goes to the document's own page. What the panel says
  is `panelModel`, a pure function the build loads to render the same panel on each document
  page, and `pagePath` is the one place a page's address is formatted, so a page and the links
  to it cannot disagree. `scripts/site/gallery-viewer.test.mjs` tests the addresses, the
  navigation, the paging, that model and the panel drawn from it in CI's guard job.
- `home.js` — the hero's document switch. The page is built with the first document
  already on it — its PDF and its page linked — so a reader without JavaScript still sees a
  real result and can open it. The switch is rendered hidden and shown by the script. A choice
  changes the picture, the caption and both links together, once the new preview is ready.
  `scripts/site/home.test.mjs` runs it against the hero parsed out of the built page.
- `examples.json` — **generated** gallery manifest. Do **not** hand-edit it; it is
  rewritten by `ShowcaseSync` (see below).
- `sitemap.xml` — **generated** from `web-src/pages/sitemap.xml`; `robots.txt` — SEO, static.
- `assets/logo/` — site logo. (`assets/pdf` + `assets/screenshots` are legacy
  landing previews, superseded by `showcase/`; safe to prune.)
- `showcase/pdf/<category>/<group>/…` — generated example PDFs.
- `showcase/screenshots/<category>/<group>/…` — PNG previews of those PDFs.
- `showcase/thumbnails/<category>/<group>/…` — the same first pages at 320px wide, which
  the viewer's strip reads. A strip slot is 54px, 46px on a narrow screen: pointed at the
  previews above, it would pull a whole page per slot.
- `showcase/pptx/<category>/<group>/…` — the PowerPoint decks of the examples that
  also render one.
- `showcase/pages/<category>/<group>/<name>-<n>.png` — every page after the first, at 1.0×,
  numbered as a reader counts them (page 1 is the screenshot above, so these start at `-2`).
  Only the 33 multi-page documents have any: 70 images, 3.66 MiB. The viewer pages through
  these instead of sending a reader to the PDF to see page 2.

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
   alongside what rendering it measured (`previewWidth`, `previewHeight`, `pageCount`,
   `needsBundledFonts`) and what its own source says (`runnable`, and any backend added to
   `requiredArtifacts`).

   `requiredArtifacts` is the register's list plus whatever the card actually needs. The DOCX
   backend is named in an import. The PPTX one is discovered by format and so appears in no
   source at all: a card needs it when its example calls one of the deck methods **or** when
   the card publishes a deck — four of the flagship twins are rendered by a sibling class, so
   their own example never mentions it and reading the source alone would miss them. A reader
   without these gets a `MissingBackendException` at render rather than a compile error. `runnable` says whether that class has a `main` a reader can
   start — the two that do not are rendered by `GenerateAllExamples`, and the site offers
   them no `exec:java` command, which would fail. Both are held to the example source by
   `ShowcaseCardInstructionsTest`.

   `needsBundledFonts` is read from the document itself: a PDF that embeds a face of its
   own cannot be reproduced from the engine and the templates alone — it compiles and then
   throws on a missing font resource — and the artifact carrying the bundled faces is
   versioned independently of the release, so those cards send a reader to
   `graph-compose-bundle` instead. It is measured rather than registered because it differs
   card by card inside one family: 25 of 27 CVs embed a face, 4 of 7 invoices do.

   The manifest also carries a `snippets` object, one entry per family, holding the compiled
   code block from that family's guide. The site is served from `web/` alone and cannot
   reach a page under `docs/`, so the block is copied in at sync time; the blocks are the
   ones `DocumentationSnippetCompileTest` compiles, so what a reader copies off the site is
   text a compiler has accepted.

   The preset and model on a card are held to the example that renders them by
   `ShowcasePresetRegistrationTest`, and each card's font claim to its own document by
   `ShowcaseBundledFontClaimTest` — both in the examples module, which is where PDFBox and a
   JSON reader are both on the test classpath.
5. Rebuild the site — `node scripts/site/build.mjs` — and commit the regenerated
   `web/showcase/**`, `web/examples.json` **and** the rebuilt pages: `web/index.html`,
   `web/sitemap.xml`, and the document pages under each category's directory — a new card's
   page added, a removed card's page deleted. The home page's no-JavaScript index and its
   preset counts, the sitemap and every page of a card's family are rendered from the
   manifest, so a new card changes all of them; skipping the rebuild leaves the committed
   pages disagreeing with `web-src/`, which `scripts/site/build.test.mjs` fails on.

## What a release changes here
`scripts/cut-release.ps1` is what edits this folder at a release; no CI workflow
writes to it.

- **Version.** The displayed version is written down once, in
  `web-src/data/release.json` (`stableVersion`, `releaseTag`, `javaMinimum`), and the build
  injects it everywhere the page states it: the latest-release link in the documentation block,
  and the seven spots that inherit from no pom — the
  `<script type="application/json" id="release-context">` block the page itself reads, the
  JSON-LD `softwareVersion` and Maven Central `downloadUrl` a crawler reads, and the hero
  badge (`Java 17+ &middot; v… &middot; MIT`) and the Maven and Gradle snippets a visitor reads.
  On a final release `cut-release.ps1` moves those two values and then runs the build; a
  pattern that matches nothing stops the cut rather than leaving that spot behind. Rewriting
  the page directly is what the generated site rules out — the next build would undo it.
  `VersionConsistencyGuardTest` holds every occurrence of all seven **in the built page**, so
  a page that was not rebuilt fails the cut's verify gate. The document pages name the
  release in their coordinates and source links too; they are rebuilt in the same run, held
  to `release.json` by `scripts/site/build.test.mjs`, and staged by the cut with one glob
  pathspec that also stages a page the rebuild deleted — `release-script-check.yml` runs that
  pathspec over a rewritten, a deleted and an added page, and fails if it stages anything under
  `showcase/`. A pre-release cut leaves them all on the last published version, and so does
  the post-release bump.
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
- `index.html`, `sitemap.xml`, `robots.txt` or any generated document page — found, not
  listed — links to a site file that is not here, read from the linking page's own
  directory;
- a `#<category>-section` anchor or a filter pill names a category `examples.json`
  does not have, a `#/<category>/<group>[/<id>]` viewer address names a family or card
  it does not have, or another anchor names no element on the page it points into;
- a card, family or category id repeats, or would need escaping in a viewer address;
- `examples.json` is not strict JSON, which the page's `fetch` would refuse as well;
- the manifest was written to a `schemaVersion` this site does not read;
- a published snippet is no longer the block it was compiled from, or names a page or a
  marker that has gone — the panel would otherwise publish code that no longer builds;
- a `PRESET` card is missing anything its panel shows: the preset class, the model, the
  templates artifact, or a source path that is a file.

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
