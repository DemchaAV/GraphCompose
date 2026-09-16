/**
 * node scripts/site/build.mjs           — write web/index.html and web/sitemap.xml
 * node scripts/site/build.mjs --check   — build in memory; exit 1 when a committed file differs
 *
 * The published site is served from `web/` exactly as committed (deploy-web.yml uploads the
 * folder, GitHub Pages runs nothing), so this build writes into `web/` and the result is what
 * ships. Only the two pages below are generated: the stylesheet, the scripts, the assets and
 * the whole `showcase/` tree are static and are never read or written here.
 *
 * Everything that used to be hand-copied into the markup now comes from data: the release the
 * page advertises, the no-JavaScript catalogue, the JSON-LD item list, the preset counts, and
 * the sitemap's document URLs. `--check` is what holds the committed pages equal to a fresh
 * build; it runs in CI's guard job through the `scripts/site/*.test.mjs` loop.
 *
 * Line endings are always LF. The repository sets core.autocrlf=true, so a Windows checkout can
 * hand this process CRLF templates while the committed blobs are LF; writing LF and comparing
 * LF-normalised text is what keeps the check reading the same on every clone rather than
 * reporting a whole-file difference that is only the checkout's.
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const SITE = "https://demchaav.github.io/GraphCompose/";

const readText = (...parts) => fs.readFileSync(path.join(root, ...parts), "utf8");
const readJson = (...parts) => JSON.parse(readText(...parts));

/** LF, whatever the checkout handed us. */
const lf = (text) => text.replace(/\r\n/g, "\n");

/**
 * HTML escaping, for text and for attribute values alike. Group labels carry `&`
 * ("Lists & Bullets"), which must not reach the page raw, and `"` is escaped too because the
 * same function writes `href` values — a quote there would end the attribute.
 */
function escapeHtml(text) {
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/**
 * Replaces every `{{token}}`, and refuses anything it cannot account for.
 *
 * A token with no value, or one left in the template after a rename, would otherwise be
 * published verbatim — `{{stableVersion}}` on the live page reads as a broken site to a
 * visitor and as nothing at all to the version guard, which looks for a version-shaped
 * string and reports "the shape this guard reads is gone". Both directions fail here
 * instead: an unknown token, and a value nothing uses.
 */
export function render(template, tokens, what) {
  const used = new Set();
  const out = template.replace(/\{\{(\w+)\}\}/g, (_, name) => {
    if (!(name in tokens)) {
      throw new Error(`${what}: the template asks for {{${name}}}, which this build does not produce`);
    }
    used.add(name);
    return tokens[name];
  });
  const unused = Object.keys(tokens).filter((name) => !used.has(name));
  if (unused.length > 0) {
    throw new Error(`${what}: nothing in the template uses ${unused.map((n) => `{{${n}}}`).join(", ")}`);
  }
  if (/\{\{\w+\}\}/.test(out)) {
    throw new Error(`${what}: a token survived rendering`);
  }
  return out;
}

/** Every card in the manifest by id, and where each one sits: its category and its group. */
function catalogue(manifest) {
  const byId = new Map();
  const places = new Map();
  for (const category of manifest.categories) {
    for (const group of category.groups) {
      for (const example of group.examples) {
        byId.set(example.id, example);
        places.set(example.id, { categoryId: category.id, groupId: group.id });
      }
    }
  }
  return { byId, places };
}

/**
 * The distinct presets one group ships.
 *
 * The page tells a visitor how many CV presets and cover letters there are, and
 * ShowcaseSiteGuardTest holds those numbers against the catalogue by exactly this rule —
 * a Set of `presetClass` within one group, so a card that re-renders another's preset with
 * different options is not counted twice. Counting any other way here would fail that guard
 * against this build's own output.
 */
function presetCount(manifest, categoryId, groupId) {
  const presets = new Set();
  const category = manifest.categories.find((c) => c.id === categoryId);
  const group = category && category.groups.find((g) => g.id === groupId);
  for (const example of (group && group.examples) || []) {
    if (typeof example.presetClass === "string") {
      presets.add(example.presetClass);
    }
  }
  return presets.size;
}

/** A featured document, resolved against the catalogue so a dead id stops the build. */
function featuredCard(byId, entry, what) {
  const card = byId.get(entry.id === undefined ? entry : entry.id);
  if (!card) {
    throw new Error(`${what}: no card in web/examples.json has the id "${entry.id || entry}"`);
  }
  return card;
}

/**
 * The JSON-LD item list: the display names stay editorial (they read "Cinematic Project Proposal",
 * not the catalogue's "Project Proposal (cinematic)"), while the URL is resolved from the manifest so a
 * renamed PDF cannot leave a crawler pointed at nothing.
 */
function jsonLdItemList(featured, byId) {
  if (!Array.isArray(featured.structuredData) || featured.structuredData.length === 0) {
    throw new Error("web-src/data/featured.json: structuredData is empty, so the page would publish an item list naming nothing");
  }
  return featured.structuredData
    .map((entry, index) => {
      const card = featuredCard(byId, entry, "featured.json structuredData");
      return [
        "          {",
        '            "@type": "ListItem",',
        `            "position": ${index + 1},`,
        `            "name": ${JSON.stringify(entry.name)},`,
        `            "url": ${JSON.stringify(SITE + card.pdf)}`,
        "          }",
      ].join("\n");
    })
    .join(",\n");
}

/**
 * The hero's document: the first entry rendered in full, and a switch to the others.
 *
 * The first document is on the page as built, so a reader without JavaScript still sees a real
 * result and can open its PDF. The switch and the viewer link are rendered `hidden` and shown by
 * `home.js`, because neither does anything without a script — a switch that swaps nothing is a
 * control that lies. Every entry is a card of the catalogue, and its image, size, PDF and viewer
 * address are read from that card, so the hero cannot point at a document the site does not
 * publish.
 */
function heroDocument(featured, byId, places) {
  if (!Array.isArray(featured.hero) || featured.hero.length === 0) {
    throw new Error("web-src/data/featured.json: hero is empty, so the page has no document to lead with");
  }
  const entries = featured.hero.map((entry) => {
    const card = featuredCard(byId, entry, "featured.json hero");
    if (typeof entry.label !== "string" || entry.label === "") {
      throw new Error(`web-src/data/featured.json: the hero entry "${entry.id}" has no label for its switch`);
    }
    for (const field of ["title", "screenshot", "pdf"]) {
      if (typeof card[field] !== "string" || card[field] === "") {
        throw new Error(`web/examples.json: the hero card "${card.id}" has no ${field}`);
      }
    }
    if (!(card.previewWidth > 0 && card.previewHeight > 0)) {
      throw new Error(`web/examples.json: the hero card "${card.id}" has no preview size to reserve its space with`);
    }
    const place = places.get(card.id);
    return { label: entry.label, card, route: `#/${place.categoryId}/${place.groupId}/${card.id}` };
  });

  const first = entries[0];
  const options = entries.map((entry, index) =>
    [
      `          <button type="button" class="hero-switch-option" data-hero-option aria-pressed="${index === 0}"`,
      `                  data-title="${escapeHtml(entry.card.title)}" data-screenshot="${escapeHtml(entry.card.screenshot)}"`,
      `                  data-width="${entry.card.previewWidth}" data-height="${entry.card.previewHeight}"`,
      `                  data-pdf="${escapeHtml(entry.card.pdf)}" data-route="${escapeHtml(entry.route)}">${escapeHtml(entry.label)}</button>`,
    ].join("\n")
  );
  return [
    '      <div class="hero-visual">',
    '        <figure class="hero-document" data-hero>',
    `          <img class="hero-document-image" data-hero-image src="${escapeHtml(first.card.screenshot)}"`,
    `               width="${first.card.previewWidth}" height="${first.card.previewHeight}"`,
    `               alt="${escapeHtml(first.card.title)}, first page" fetchpriority="high">`,
    '          <figcaption class="hero-document-caption">',
    `            <span class="hero-document-title" data-hero-title aria-live="polite">${escapeHtml(first.card.title)}</span>`,
    `            <a class="hero-document-link" data-hero-pdf href="${escapeHtml(first.card.pdf)}">Open PDF</a>`,
    `            <a class="hero-document-link" data-hero-open href="${escapeHtml(first.route)}" hidden>Open in viewer</a>`,
    "          </figcaption>",
    "        </figure>",
    '        <div class="hero-switch" role="group" aria-label="Show another document" data-hero-switch hidden>',
    ...options,
    "        </div>",
    "      </div>",
  ].join("\n");
}

/**
 * The index a visitor with no JavaScript gets: every document in the catalogue, under its
 * category and group.
 *
 * The category headings keep their `<category>-section` ids — the menu, the sitemap and
 * ShowcaseSiteGuardTest all resolve to them, so the ids are a contract even though the text
 * around them is generated.
 */
function noscriptCatalogue(manifest) {
  const sections = manifest.categories.map((category) => {
    const groups = category.groups.map((group) => {
      const items = group.examples.map((example) => {
        // Both fields reach a visitor directly, so a card missing either stops the build. A
        // missing title used to publish the string "undefined" with nothing objecting.
        for (const field of ["title", "pdf"]) {
          if (typeof example[field] !== "string" || example[field] === "") {
            throw new Error(
              `web/examples.json: the card "${example.id}" has no ${field}, so the no-JavaScript index cannot name it`
            );
          }
        }
        return `            <li><a href="${escapeHtml(example.pdf)}">${escapeHtml(example.title)}</a></li>`;
      });
      return [`          <h4>${escapeHtml(group.label)}</h4>`, "          <ul>", ...items, "          </ul>"].join("\n");
    });
    // Blank lines between the blocks, as the hand-written index had them: this is markup a
    // reader still meets in a diff, and a 117-item wall of list items is worse to read.
    return [`          <h3 id="${category.id}-section">${escapeHtml(category.label)}</h3>`, ...groups].join("\n\n");
  });
  return sections.join("\n\n");
}

/** The hero PDFs the sitemap surfaces, their URLs resolved from the manifest. */
function sitemapDocuments(featured, byId) {
  return featured.sitemapDocuments
    .map((id) => {
      const card = featuredCard(byId, id, "featured.json sitemapDocuments");
      return [
        "  <url>",
        `    <loc>${SITE + card.pdf}</loc>`,
        "    <changefreq>monthly</changefreq>",
        "    <priority>0.7</priority>",
        "  </url>",
      ].join("\n");
    })
    .join("\n");
}

/**
 * The generated pages, as `web/`-relative path → content.
 *
 * `sources` substitutes an input instead of reading it from disk. Only the test harness
 * passes it: the refusals below — a featured id no card has, a token the build does not
 * produce — are the behaviour worth testing, and testing them against the real tree would
 * mean writing a broken catalogue into `web/` to watch the build reject it.
 */
export function build(sources = {}) {
  const manifest = sources.manifest ?? readJson("web", "examples.json");
  const release = sources.release ?? readJson("web-src", "data", "release.json");
  const featured = sources.featured ?? readJson("web-src", "data", "featured.json");
  const { byId, places } = catalogue(manifest);

  const index = render(
    lf(readText("web-src", "pages", "index.html")),
    {
      stableVersion: release.stableVersion,
      releaseTag: release.releaseTag,
      javaMinimum: release.javaMinimum,
      cvPresetCount: String(presetCount(manifest, "templates", "cv")),
      letterCount: String(presetCount(manifest, "templates", "coverletter")),
      jsonLdItemList: jsonLdItemList(featured, byId),
      heroDocument: heroDocument(featured, byId, places),
      noscriptCatalogue: noscriptCatalogue(manifest),
    },
    "web-src/pages/index.html"
  );

  const sitemap = render(
    lf(readText("web-src", "pages", "sitemap.xml")),
    { sitemapDocuments: sitemapDocuments(featured, byId) },
    "web-src/pages/sitemap.xml"
  );

  return { "index.html": index, "sitemap.xml": sitemap };
}

/** The first line at which two texts diverge, as a human-readable report. */
function firstDifference(expected, actual) {
  const want = expected.split("\n");
  const got = actual.split("\n");
  for (let i = 0; i < Math.max(want.length, got.length); i++) {
    if (want[i] !== got[i]) {
      return [
        `  line ${i + 1}`,
        `    committed: ${want[i] === undefined ? "(end of file)" : want[i]}`,
        `    built:     ${got[i] === undefined ? "(end of file)" : got[i]}`,
      ].join("\n");
    }
  }
  return "  (the files differ only in line endings)";
}

function main() {
  const check = process.argv.includes("--check");
  const pages = build();
  const stale = [];

  for (const [name, content] of Object.entries(pages)) {
    const target = path.join(root, "web", name);
    const committed = fs.existsSync(target) ? lf(fs.readFileSync(target, "utf8")) : null;
    if (check) {
      if (committed !== content) {
        stale.push(`web/${name} is not what web-src/ builds:\n${firstDifference(committed || "", content)}`);
      }
      continue;
    }
    if (committed === content) {
      console.log(`  unchanged  web/${name}`);
      continue;
    }
    fs.writeFileSync(target, content, "utf8");
    console.log(`  written    web/${name}`);
  }

  if (stale.length > 0) {
    console.error(stale.join("\n\n"));
    console.error("\nRun `node scripts/site/build.mjs` and commit the result.");
    process.exit(1);
  }
  console.log(check ? "site: the committed pages match web-src/" : "site: pages built from web-src/");
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main();
}
