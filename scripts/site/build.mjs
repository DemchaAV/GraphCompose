/**
 * node scripts/site/build.mjs           — write the generated pages into web/
 * node scripts/site/build.mjs --check   — build in memory; exit 1 when a page is missing, stale or orphaned
 *
 * The published site is served from `web/` exactly as committed (deploy-web.yml uploads the
 * folder, GitHub Pages runs nothing), so this build writes into `web/` and the result is what
 * ships. It generates the home page, the sitemap, and one page for every document in the
 * catalogue, at `<category>/<group>/<id>/index.html`. The stylesheet, the scripts, the assets and
 * the whole `showcase/` tree are static: none of them is written here, and all that is read from
 * `showcase/` is the pixel size the later page images and the thumbnails state in their PNG
 * headers (a first page's size is the catalogue's, which ShowcaseSiteGuardTest holds to its file).
 *
 * Everything that used to be hand-copied into the markup now comes from data: the release the
 * page advertises, the no-JavaScript catalogue, the JSON-LD item list, the preset counts, the
 * sitemap, and the document pages. What a document page tells a reader to add, run and read is
 * not decided here at all: it is `panelModel` in `web/gallery-viewer.js`, loaded the way the page
 * loads it, so a document's page and the viewer's panel cannot tell a reader different things.
 *
 * The build owns a document page it wrote and no other file: one that carries GENERATOR_MARK and
 * whose canonical address is the place it sits, so a generated page copied elsewhere to start a
 * page by hand is not the build's to delete. An owned page no card builds any more is deleted,
 * with any directory it leaves empty, and `--check` reports it — along with a page that is
 * missing or differs from a fresh build. CI's guard job runs `scripts/site/build.test.mjs`, which
 * runs `--check` itself and the checks behind it.
 *
 * Line endings are always LF. The repository sets core.autocrlf=true, so a Windows checkout can
 * hand this process CRLF templates while the committed blobs are LF; writing LF and comparing
 * LF-normalised text is what keeps the check reading the same on every clone rather than
 * reporting a whole-file difference that is only the checkout's.
 */
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const webRoot = path.join(root, "web");
export const SITE = "https://demchaav.github.io/GraphCompose/";

/** Carried by every document page the build writes; with the page's canonical address, what it owns. */
export const GENERATOR_MARK = '<meta name="generator" content="GraphCompose site build">';

/**
 * A category, family or card id becomes a directory name and an address segment as it stands.
 * Anything else is refused before a file is written: a `..` or a `/` in an id would write outside
 * the page's own directory. ShowcaseSiteGuardTest holds the catalogue to the same rule.
 */
const ADDRESS_SAFE = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

/** The static trees under web/ that a category's pages would be written into. */
const STATIC_ROOTS = new Set(["assets", "showcase"]);

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

/**
 * A block every page shares — the site header, the footer, the two theme scripts — rendered with
 * the tokens it asks for. Written once so the home page and the document pages cannot carry two
 * menus that drift apart; `base` is the path back to the site root, empty on the home page.
 */
function partial(name, tokens) {
  const text = lf(readText("web-src", "partials", name)).replace(/\n$/, "");
  return render(text, tokens, `web-src/partials/${name}`);
}

/**
 * web/gallery-viewer.js, run the way the page runs it: as a plain script. Its page paths and its
 * panel model are the ones this build writes pages with.
 */
function loadGallery() {
  const sandbox = {};
  vm.runInNewContext(readText("web", "gallery-viewer.js"), sandbox, { filename: "web/gallery-viewer.js" });
  return sandbox.GraphComposeGallery;
}

function requireAddressSafe(id, what) {
  if (typeof id !== "string" || !ADDRESS_SAFE.test(id)) {
    throw new Error(
      `web/examples.json: the ${what} id ${JSON.stringify(id)} is not lowercase words joined by hyphens, ` +
        "and it would be written into a directory name and an address as it stands"
    );
  }
}

/**
 * Every card in the manifest by id, where each one sits, and the catalogue in the shape
 * gallery-viewer.js reads — the shape examples.js hands the viewer.
 */
function catalogue(manifest) {
  const byId = new Map();
  const places = new Map();
  for (const category of manifest.categories) {
    requireAddressSafe(category.id, "category");
    if (STATIC_ROOTS.has(category.id)) {
      throw new Error(`web/examples.json: a category named "${category.id}" would write its pages into web/${category.id}/`);
    }
    for (const group of category.groups) {
      requireAddressSafe(group.id, "family");
      for (const example of group.examples) {
        requireAddressSafe(example.id, "card");
        if (byId.has(example.id)) {
          throw new Error(`web/examples.json: two cards have the id "${example.id}", and an address can name only one of them`);
        }
        byId.set(example.id, example);
        places.set(example.id, { category, group });
      }
    }
  }
  const viewerCatalogue = {
    snippets: manifest.snippets || {},
    get: (id) =>
      byId.has(id)
        ? { categoryId: places.get(id).category.id, groupId: places.get(id).group.id, example: byId.get(id) }
        : undefined,
  };
  return { byId, places, viewerCatalogue };
}

/** A card's address: the three segments its page path and its viewer address are made of. */
function routeOf(places, id) {
  const place = places.get(id);
  return { category: place.category.id, group: place.group.id, id };
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
 * not the catalogue's "Project Proposal (cinematic)"), while each URL is the document's own page,
 * resolved from the manifest so a renamed card cannot leave a crawler pointed at nothing.
 */
function jsonLdItemList(featured, byId, places, gallery) {
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
        `            "url": ${JSON.stringify(SITE + gallery.pagePath(routeOf(places, card.id)))}`,
        "          }",
      ].join("\n");
    })
    .join(",\n");
}

/**
 * The hero's document: the first entry rendered in full, and a switch to the others.
 *
 * The first document is on the page as built, so a reader without JavaScript still sees a real
 * result and can open its PDF or its page. The switch is rendered `hidden` and shown by `home.js`,
 * because it does nothing without a script — a switch that swaps nothing is a control that lies.
 * Every entry is a card of the catalogue, and its image, size, PDF and page are read from that
 * card, so the hero cannot point at a document the site does not publish.
 */
function heroDocument(featured, byId, places, gallery) {
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
    return { label: entry.label, card, page: gallery.pagePath(routeOf(places, card.id)) };
  });

  const first = entries[0];
  const options = entries.map((entry, index) =>
    [
      `          <button type="button" class="hero-switch-option" data-hero-option aria-pressed="${index === 0}"`,
      `                  data-title="${escapeHtml(entry.card.title)}" data-screenshot="${escapeHtml(entry.card.screenshot)}"`,
      `                  data-width="${entry.card.previewWidth}" data-height="${entry.card.previewHeight}"`,
      `                  data-pdf="${escapeHtml(entry.card.pdf)}" data-page="${escapeHtml(entry.page)}">${escapeHtml(entry.label)}</button>`,
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
    `            <a class="hero-document-link" data-hero-page href="${escapeHtml(first.page)}">Details</a>`,
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
 * category and group, each linking to its own page — which carries the PDF, every page and how to
 * reproduce it.
 *
 * The category headings keep their `<category>-section` ids — the menu, the sitemap and
 * ShowcaseSiteGuardTest all resolve to them, so the ids are a contract even though the text
 * around them is generated. Each family heading's id is its viewer address without the `#`
 * (`/templates/cv`): with JavaScript that address opens the viewer, and without it — where this
 * index is the page — the browser lands on the family's list instead of the top of the page. A
 * document page's family link is such an address.
 */
function noscriptCatalogue(manifest, places, gallery) {
  const sections = manifest.categories.map((category) => {
    const groups = category.groups.map((group) => {
      const items = group.examples.map((example) => {
        // A missing title used to publish the string "undefined" with nothing objecting.
        if (typeof example.title !== "string" || example.title === "") {
          throw new Error(
            `web/examples.json: the card "${example.id}" has no title, so the no-JavaScript index cannot name it`
          );
        }
        const page = gallery.pagePath(routeOf(places, example.id));
        return `            <li><a href="${escapeHtml(page)}">${escapeHtml(example.title)}</a></li>`;
      });
      const address = gallery.formatRoute({ category: category.id, group: group.id }).slice(1);
      return [
        `          <h4 id="${escapeHtml(address)}">${escapeHtml(group.label)}</h4>`,
        "          <ul>",
        ...items,
        "          </ul>",
      ].join("\n");
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

/** One sitemap entry for every document page, in catalogue order. */
function sitemapPages(manifest, places, gallery) {
  return manifest.categories
    .flatMap((category) => category.groups.flatMap((group) => group.examples))
    .map((example) =>
      [
        "  <url>",
        `    <loc>${SITE + gallery.pagePath(routeOf(places, example.id))}</loc>`,
        "    <changefreq>monthly</changefreq>",
        "    <priority>0.6</priority>",
        "  </url>",
      ].join("\n")
    )
    .join("\n");
}

const PNG_SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];

/**
 * The pixel size a PNG under web/ states in its header — read, not decoded. A page image reserves
 * its space with it before the image arrives, and a file that is missing or not a PNG stops the
 * build: the page would otherwise ask for an image nothing publishes.
 */
function pngSize(sitePath, what) {
  const file = path.resolve(webRoot, ...String(sitePath).split("/"));
  if (!file.startsWith(webRoot + path.sep)) {
    throw new Error(`${what}: ${sitePath} is not a path inside web/`);
  }
  const header = Buffer.alloc(24);
  let read = 0;
  try {
    const descriptor = fs.openSync(file, "r");
    try {
      read = fs.readSync(descriptor, header, 0, header.length, 0);
    } finally {
      fs.closeSync(descriptor);
    }
  } catch {
    throw new Error(`${what}: ${sitePath} is not a file under web/`);
  }
  if (read < header.length || PNG_SIGNATURE.some((byte, i) => header[i] !== byte) || header.toString("latin1", 12, 16) !== "IHDR") {
    throw new Error(`${what}: ${sitePath} is not a readable PNG`);
  }
  return { width: header.readUInt32BE(16), height: header.readUInt32BE(20) };
}

/** The panel model as the page draws it: labelled lines, captioned listings, and the links under them. */
function panelHtml(model) {
  const lines = [];
  for (const item of model.items) {
    if ("code" in item) {
      lines.push(
        '        <figure class="reproduce-listing">',
        `          <figcaption class="reproduce-label">${escapeHtml(item.label)}</figcaption>`,
        // The listing's own line breaks are the code's; nothing may indent the lines after the first.
        `          <pre class="reproduce-code"><code>${escapeHtml(item.code)}</code></pre>`,
        "        </figure>"
      );
    } else {
      // A class name or a path is set as code; a sentence is not.
      const element = item.literal ? "code" : "span";
      lines.push(
        `        <p class="reproduce-row"><span class="reproduce-label">${escapeHtml(item.label)}</span> ` +
          `<${element} class="reproduce-value">${escapeHtml(item.value)}</${element}></p>`
      );
    }
  }
  if (model.links.length > 0) {
    lines.push(
      '        <p class="reproduce-links">',
      ...model.links.map(
        (link) => `          <a class="button button-secondary button-small" href="${escapeHtml(link.href)}">${escapeHtml(link.text)}</a>`
      ),
      "        </p>"
    );
  }
  return lines.join("\n");
}

/**
 * One document's page: what the viewer shows of it, at an address of its own and readable without
 * JavaScript — every page of the document, the files, the reproduction panel, and the other
 * documents of its family.
 */
function documentPage({ card, category, group, template, gallery, catalogueView, release, sizeOf }) {
  const what = `web/examples.json: the card "${card.id}"`;
  for (const field of ["title", "description", "pdf", "screenshot"]) {
    if (typeof card[field] !== "string" || card[field] === "") {
      throw new Error(`${what} has no ${field}, and its page would publish a gap where it goes`);
    }
  }
  if (!(card.previewWidth > 0 && card.previewHeight > 0)) {
    throw new Error(`${what} has no preview size to reserve its first page with`);
  }

  const route = { category: category.id, group: group.id, id: card.id };
  const pagePath = gallery.pagePath(route);
  const base = "../".repeat(pagePath.split("/").filter(Boolean).length);
  const canonical = SITE + pagePath;

  const images = [
    { src: card.screenshot, width: card.previewWidth, height: card.previewHeight },
    ...(card.pages || []).map((src) => ({ src, ...sizeOf(src, `${what}, a page after the first`) })),
  ];
  const total = images.length;
  const pageImages = images
    .map((image, index) => {
      const number = index + 1;
      // The first page is what a reader came for; the rest wait until they are scrolled to.
      const loading = index === 0 ? ' fetchpriority="high"' : ' loading="lazy" decoding="async"';
      return [
        '        <figure class="document-page-image">',
        // The PDF is the zoom: vector, at whatever size the reader's viewer offers. `#page=` opens it
        // at this page in the viewers that read the parameter; the rest open it at its start, so
        // the text a screen reader hears promises only the PDF.
        `          <a href="${escapeHtml(base + card.pdf)}#page=${number}">`,
        `            <img src="${escapeHtml(base + image.src)}" width="${image.width}" height="${image.height}"`,
        `                 alt="${escapeHtml(`${card.title}, page ${number} of ${total}`)}"${loading}>`,
        '            <span class="visually-hidden">(opens the PDF)</span>',
        "          </a>",
        ...(total > 1 ? [`          <figcaption>Page ${number} of ${total}</figcaption>`] : []),
        "        </figure>",
      ].join("\n");
    })
    .join("\n");

  const actions = [
    `          <a class="button button-primary" href="${escapeHtml(base + card.pdf)}">Open PDF</a>`,
    // Only a card that published a deck offers one: the link would otherwise be a 404.
    ...(card.pptx ? [`          <a class="button button-secondary" href="${escapeHtml(base + card.pptx)}" download>Get PPTX</a>`] : []),
  ].join("\n");

  const breadcrumb = [
    `        <li><a href="${base}">Home</a></li>`,
    `        <li><a href="${base}#${category.id}-section">${escapeHtml(category.label)}</a></li>`,
    `        <li><a href="${base}${escapeHtml(gallery.formatRoute({ category: category.id, group: group.id }))}">${escapeHtml(group.label)}</a></li>`,
    `        <li aria-current="page">${escapeHtml(card.title)}</li>`,
  ].join("\n");

  const siblings = group.examples.filter((other) => other.id !== card.id);
  const related =
    siblings.length === 0
      ? ""
      : [
          "",
          '    <section class="document-related" aria-labelledby="related-title">',
          `      <h2 id="related-title">More in ${escapeHtml(group.label)}</h2>`,
          '      <ul class="document-related-list">',
          ...siblings.map((other) => {
            if (typeof other.thumbnail !== "string" || other.thumbnail === "") {
              throw new Error(`web/examples.json: the card "${other.id}" has no thumbnail to be listed beside its family with`);
            }
            const size = sizeOf(other.thumbnail, `web/examples.json: the card "${other.id}", its thumbnail`);
            const href = base + gallery.pagePath({ category: category.id, group: group.id, id: other.id });
            return [
              "        <li>",
              `          <a href="${escapeHtml(href)}">`,
              `            <img src="${escapeHtml(base + other.thumbnail)}" width="${size.width}" height="${size.height}" alt="" loading="lazy" decoding="async">`,
              `            <span>${escapeHtml(other.title)}</span>`,
              "          </a>",
              "        </li>",
            ].join("\n");
          }),
          "      </ul>",
          "    </section>",
        ].join("\n");

  // A web page about one document: its first page as the page's image, and the PDF as what the page
  // is about. Serialised JSON inside a script element, so `<` is written as its escape and no title
  // can close the element early.
  const structuredData = JSON.stringify(
    {
      "@context": "https://schema.org",
      "@type": "WebPage",
      name: card.title,
      description: card.description,
      url: canonical,
      primaryImageOfPage: {
        "@type": "ImageObject",
        url: SITE + card.screenshot,
        width: card.previewWidth,
        height: card.previewHeight,
      },
      mainEntity: {
        "@type": "DigitalDocument",
        name: card.title,
        encodingFormat: "application/pdf",
        url: SITE + card.pdf,
      },
      isPartOf: { "@type": "WebSite", name: "GraphCompose", url: SITE },
    },
    null,
    2
  )
    .replace(/</g, "\\u003c")
    .split("\n")
    .map((line) => `  ${line}`)
    .join("\n");

  const model = gallery.panelModel(card, catalogueView, release);
  return render(
    template,
    {
      themeInit: partial("theme-init.html", {}),
      siteHeader: partial("site-header.html", { base }),
      siteFooter: partial("site-footer.html", {}),
      themeToggle: partial("theme-toggle.html", {}),
      base,
      pageTitle: escapeHtml(`${card.title} · ${group.label} · GraphCompose`),
      description: escapeHtml(card.description),
      canonical: escapeHtml(canonical),
      previewUrl: escapeHtml(SITE + card.screenshot),
      previewWidth: String(card.previewWidth),
      previewHeight: String(card.previewHeight),
      structuredData,
      breadcrumb,
      title: escapeHtml(card.title),
      facts: `${total} page${total === 1 ? "" : "s"}`,
      actions,
      pageImages,
      action: escapeHtml(model.action),
      panel: panelHtml(model),
      related,
    },
    `web-src/pages/document.html (${pagePath})`
  );
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
  const gallery = loadGallery();
  const { byId, places, viewerCatalogue } = catalogue(manifest);

  const index = render(
    lf(readText("web-src", "pages", "index.html")),
    {
      themeInit: partial("theme-init.html", {}),
      siteHeader: partial("site-header.html", { base: "" }),
      siteFooter: partial("site-footer.html", {}),
      themeToggle: partial("theme-toggle.html", {}),
      stableVersion: release.stableVersion,
      releaseTag: release.releaseTag,
      javaMinimum: release.javaMinimum,
      cvPresetCount: String(presetCount(manifest, "templates", "cv")),
      letterCount: String(presetCount(manifest, "templates", "coverletter")),
      jsonLdItemList: jsonLdItemList(featured, byId, places, gallery),
      heroDocument: heroDocument(featured, byId, places, gallery),
      noscriptCatalogue: noscriptCatalogue(manifest, places, gallery),
    },
    "web-src/pages/index.html"
  );

  const sitemap = render(
    lf(readText("web-src", "pages", "sitemap.xml")),
    {
      documentPages: sitemapPages(manifest, places, gallery),
      sitemapDocuments: sitemapDocuments(featured, byId),
    },
    "web-src/pages/sitemap.xml"
  );

  const pages = { "index.html": index, "sitemap.xml": sitemap };
  const template = lf(readText("web-src", "pages", "document.html"));
  const sizes = new Map();
  const sizeOf = (sitePath, what) => {
    if (!sizes.has(sitePath)) sizes.set(sitePath, pngSize(sitePath, what));
    return sizes.get(sitePath);
  };
  for (const category of manifest.categories) {
    for (const group of category.groups) {
      for (const card of group.examples) {
        const pagePath = gallery.pagePath({ category: category.id, group: group.id, id: card.id });
        pages[`${pagePath}index.html`] = documentPage({
          card, category, group, template, gallery, catalogueView: viewerCatalogue, release, sizeOf,
        });
      }
    }
  }
  return pages;
}

/**
 * Every page under the site root the build wrote where it sits, as root-relative paths: an
 * `index.html` that carries GENERATOR_MARK and names its own directory as its canonical address.
 * Both, because the mark alone travels with a copy — a generated page copied to
 * `web/guides/start/` to begin a page by hand still carries it, and deleting that would destroy
 * work the build never did. `showcase/` is never walked: it holds the catalogue's files, not
 * pages, and it is most of the tree. `site` is web/ except in the test harness, which proves the
 * rule on a directory of its own.
 */
export function ownedPages(site = webRoot) {
  const found = [];
  const walk = (directory, relative) => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const name = relative ? `${relative}/${entry.name}` : entry.name;
      if (entry.isDirectory()) {
        if (name !== "showcase") walk(path.join(directory, entry.name), name);
      } else if (entry.isFile() && entry.name === "index.html" && relative) {
        const text = fs.readFileSync(path.join(directory, entry.name), "utf8");
        if (text.includes(GENERATOR_MARK) && text.includes(`<link rel="canonical" href="${SITE}${relative}/">`)) {
          found.push(name);
        }
      }
    }
  };
  walk(site, "");
  return found.sort();
}

/** The pages the build owns on disk that no card builds any more. */
export function orphanedPages(pages, site = webRoot) {
  return ownedPages(site).filter((name) => !(name in pages));
}

/** Deletes a page the build owns, and each directory it leaves empty on the way back to the site root. */
export function removePage(name, site = webRoot) {
  const file = path.join(site, ...name.split("/"));
  fs.unlinkSync(file);
  for (let directory = path.dirname(file); directory !== site; directory = path.dirname(directory)) {
    if (fs.readdirSync(directory).length > 0) break;
    fs.rmdirSync(directory);
  }
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

/** The text of a page on disk, LF-normalised, or null when there is no such file. */
function committedText(site, name) {
  const target = path.join(site, ...name.split("/"));
  return fs.existsSync(target) ? lf(fs.readFileSync(target, "utf8")) : null;
}

/**
 * What `--check` reports for a site root: every page that is missing or is not what a fresh build
 * produces, and every page the build owns that no card builds any more. It writes and deletes
 * nothing — a check that tidied the tree would hand CI a clean one to pass.
 */
export function checkSite(pages, site = webRoot) {
  const problems = [];
  for (const [name, content] of Object.entries(pages)) {
    const committed = committedText(site, name);
    if (committed === null) {
      problems.push(`web/${name} is missing: web-src/ builds it`);
    } else if (committed !== content) {
      problems.push(`web/${name} is not what web-src/ builds:\n${firstDifference(committed, content)}`);
    }
  }
  for (const orphan of orphanedPages(pages, site)) {
    problems.push(`web/${orphan} is a generated page that no card in web/examples.json builds any more`);
  }
  return problems;
}

/** Writes every page that differs from what is on disk and deletes every orphan; says what it did. */
export function writeSite(pages, site = webRoot) {
  const report = { written: [], unchanged: 0, deleted: [] };
  for (const [name, content] of Object.entries(pages)) {
    if (committedText(site, name) === content) {
      report.unchanged++;
      continue;
    }
    const target = path.join(site, ...name.split("/"));
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, content, "utf8");
    report.written.push(name);
  }
  for (const orphan of orphanedPages(pages, site)) {
    removePage(orphan, site);
    report.deleted.push(orphan);
  }
  return report;
}

/** How many problems are printed in full before the rest are only counted. */
const REPORTED_IN_FULL = 8;

function main() {
  const pages = build();
  if (process.argv.includes("--check")) {
    const problems = checkSite(pages);
    if (problems.length > 0) {
      console.error(problems.slice(0, REPORTED_IN_FULL).join("\n\n"));
      if (problems.length > REPORTED_IN_FULL) {
        console.error(`\n...and ${problems.length - REPORTED_IN_FULL} more.`);
      }
      console.error("\nRun `node scripts/site/build.mjs` and commit the result.");
      process.exit(1);
    }
    console.log("site: the committed pages match web-src/");
    return;
  }
  const report = writeSite(pages);
  for (const name of report.written) console.log(`  written    web/${name}`);
  for (const name of report.deleted) console.log(`  deleted    web/${name}`);
  console.log(`  unchanged  ${report.unchanged} of ${report.written.length + report.unchanged} pages`);
  console.log("site: pages built from web-src/");
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main();
}
