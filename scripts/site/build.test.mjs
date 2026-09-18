/**
 * node scripts/site/build.test.mjs — exit 0 when every case holds.
 *
 * Covers the site build: that the committed pages are the ones web-src/ produces, that the
 * build refuses rather than publishes when an input has moved, and that the generated page
 * still carries the two contracts other things depend on — the version in every shape the
 * release guard reads, and the preset counts by the same rule the site guard counts them.
 *
 * CI's guard job runs every scripts/site/*.test.mjs, so this file needs no wiring of its own.
 */
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import vm from "node:vm";
import { fileURLToPath } from "node:url";

import {
  GENERATOR_MARK, SITE, build, checkSite, orphanedPages, ownedPages, removePage, render, writeSite,
} from "./build.mjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const web = (name) => fs.readFileSync(path.join(root, "web", name), "utf8").replace(/\r\n/g, "\n");
const manifest = JSON.parse(web("examples.json"));
const release = JSON.parse(fs.readFileSync(path.join(root, "web-src", "data", "release.json"), "utf8"));

const failures = [];
function test(name, fn) {
  try {
    fn();
    console.log(`  ok    ${name}`);
  } catch (error) {
    failures.push(name);
    console.log(`  FAIL  ${name}`);
    console.log(`        ${error.message.split("\n").join("\n        ")}`);
  }
}

const built = build();
const cards = manifest.categories.flatMap((c) => c.groups.flatMap((g) => g.examples));

// The viewer script, loaded the way the build loads it: its page paths and panel model are what
// the generated pages are held to here.
const sandbox = {};
vm.runInNewContext(fs.readFileSync(path.join(root, "web", "gallery-viewer.js"), "utf8"), sandbox);
const gallery = sandbox.GraphComposeGallery;

/** Where a card sits: its category and its group. */
function placeOf(id) {
  for (const category of manifest.categories) {
    for (const group of category.groups) {
      if (group.examples.some((example) => example.id === id)) return { category, group };
    }
  }
  return null;
}

/** A card's page, relative to the site root, the way gallery-viewer.js formats it. */
function pageOf(id) {
  const { category, group } = placeOf(id);
  return gallery.pagePath({ category: category.id, group: group.id, id });
}

/** The distinct presets of one group — the rule ShowcaseSiteGuardTest counts by. */
function presetsOf(categoryId, groupId) {
  const category = manifest.categories.find((c) => c.id === categoryId);
  const group = category.groups.find((g) => g.id === groupId);
  return new Set(group.examples.filter((e) => e.presetClass).map((e) => e.presetClass)).size;
}

test("the committed pages are the ones web-src/ builds", () => {
  for (const name of Object.keys(built)) {
    assert.equal(
      built[name],
      web(name),
      `web/${name} is not what web-src/ builds — run node scripts/site/build.mjs and commit it`
    );
  }
});

/** The real manifest with one edit, for the refusals that need a catalogue the tree does not have. */
function manifestWith(edit) {
  const copy = JSON.parse(JSON.stringify(manifest));
  edit(copy);
  return copy;
}

test("a card with no title stops the build", () => {
  // It used to publish the literal string "undefined" as the document's name, and nothing
  // objected: the link worked, so every link-resolving guard stayed green.
  const doctored = manifestWith((m) => {
    delete m.categories[0].groups[0].examples[0].title;
  });
  assert.throws(() => build({ manifest: doctored }), /has no title/);
});

test("a featured list with nothing in it stops the build", () => {
  assert.throws(
    () => build({ featured: { structuredData: [], sitemapDocuments: ["master-showcase"] } }),
    /structuredData is empty/
  );
});

test("a template token the build does not produce stops it", () => {
  assert.throws(() => render("<p>{{nowhere}}</p>", {}, "fixture"), /does not produce/);
});

test("a value no template uses stops the build", () => {
  // The direction that would otherwise pass in silence: a token renamed in the template
  // leaves the build computing a value nothing publishes, and the page keeps the old text.
  assert.throws(() => render("<p>nothing</p>", { spare: "1" }, "fixture"), /nothing in the template uses/);
});

test("no token survives into a built page", () => {
  for (const [name, content] of Object.entries(built)) {
    assert.equal(/\{\{\w+\}\}/.test(content), false, `web/${name} still carries a template token`);
  }
});

test("a featured id that names no card stops the build", () => {
  assert.throws(
    () =>
      build({
        featured: { structuredData: [{ id: "no-such-card", name: "Nothing" }], sitemapDocuments: [] },
      }),
    /no-such-card/
  );
});

test("the page counts the presets the catalogue has", () => {
  const cvPresets = presetsOf("templates", "cv");
  const letters = presetsOf("templates", "coverletter");
  assert.ok(cvPresets > 0, "no CV card names a preset, so this case would hold the page against nothing");

  // The two shapes ShowcaseSiteGuardTest reads out of the page. Held here as well because a
  // build that counts differently turns that Java guard red against this build's own output,
  // which is a confusing place to find out.
  const cvClaims = [...built["index.html"].matchAll(/(\d+)\s+CV presets/g)].map((m) => m[1]);
  const letterClaims = [...built["index.html"].matchAll(/(\d+)\s+matching\s+(?:cover\s+)?letters/g)].map((m) => m[1]);
  assert.ok(cvClaims.length > 0, "the built page no longer counts CV presets in the shape the site guard reads");
  assert.ok(letterClaims.length > 0, "the built page no longer counts cover letters in that shape");
  assert.deepEqual(new Set(cvClaims), new Set([String(cvPresets)]));
  assert.deepEqual(new Set(letterClaims), new Set([String(letters)]));
});

test("the no-JavaScript index links every document's page", () => {
  const noscript = built["index.html"].match(/<noscript>([\s\S]*?)<\/noscript>/)[1];
  const linked = [...noscript.matchAll(/<li><a href="([^"]+)"/g)].map((m) => m[1]);
  assert.equal(
    linked.length,
    cards.length,
    "a card in the catalogue reaches no visitor without JavaScript — the point of generating this block"
  );
  assert.deepEqual(new Set(linked), new Set(cards.map((card) => pageOf(card.id))));
});

test("a family's viewer address lands on its list in the no-JavaScript index", () => {
  // A document page links its family by viewer address. With JavaScript that opens the viewer;
  // without it the browser looks for an element with that id, and without one the reader lands at
  // the top of the home page.
  const noscript = built["index.html"].match(/<noscript>([\s\S]*?)<\/noscript>/)[1];
  const headings = new Set([...noscript.matchAll(/<h4 id="([^"]+)">/g)].map((m) => m[1]));
  for (const category of manifest.categories) {
    for (const group of category.groups) {
      const address = gallery.formatRoute({ category: category.id, group: group.id });
      assert.ok(headings.has(address.slice(1)), `${address} names no family heading in the no-JavaScript index`);
    }
  }
});

test("every category heading keeps the id the menu and the sitemap resolve to", () => {
  for (const category of manifest.categories) {
    assert.ok(
      built["index.html"].includes(`<h3 id="${category.id}-section">`),
      `#${category.id}-section is linked from the menu and the sitemap, and the generated heading dropped it`
    );
  }
});

test("the built page states the release in every shape the version guard reads", () => {
  // VersionConsistencyGuardTest holds each of these against the pom, and the release cut
  // rewrites them. They now come out of web-src/data/release.json, so a build that stopped
  // emitting one would leave that guard reading a page shape that is gone.
  const page = built["index.html"];
  const shapes = {
    "release-context stableVersion": [/"stableVersion":\s*"v?([^"]+)"/g, release.stableVersion],
    "release-context releaseTag": [/"releaseTag":\s*"v?([^"]+)"/g, release.stableVersion],
    "JSON-LD softwareVersion": [/"softwareVersion":\s*"v?([^"]+)"/g, release.stableVersion],
    "Central downloadUrl": [
      /central\.sonatype\.com\/artifact\/io\.github\.demchaav\/graph-compose\/v?([^"]+)/g,
      release.stableVersion,
    ],
    "hero badge": [/v([0-9][^\s&]*)\s*&middot;\s*MIT/g, release.stableVersion],
    "Maven snippet": [
      /&lt;artifactId&gt;graph-compose&lt;\/artifactId&gt;\s*&lt;version&gt;v?([^&]+)&lt;\/version&gt;/g,
      release.stableVersion,
    ],
    "Gradle snippet": [/io\.github\.demchaav:graph-compose:v?([^')]+)/g, release.stableVersion],
  };
  for (const [what, [pattern, expected]] of Object.entries(shapes)) {
    const found = [...page.matchAll(pattern)].map((m) => m[1]);
    assert.ok(found.length > 0, `the built page no longer carries ${what}`);
    assert.deepEqual(new Set(found), new Set([expected]), `${what} does not state ${expected}`);
  }
});

test("the release the page advertises is the one written down, not one the build invented", () => {
  assert.match(built["index.html"], new RegExp(`"releaseTag":\\s*"${release.releaseTag}"`));
  assert.match(built["index.html"], new RegExp(`${release.releaseTag} &middot; MIT`));
  assert.match(built["index.html"], new RegExp(`Java ${release.javaMinimum}\\+ &middot;`));
});

test("every URL the sitemap surfaces is a document the catalogue publishes", () => {
  const pdfs = new Set(cards.map((card) => card.pdf));
  const locs = [...built["sitemap.xml"].matchAll(/<loc>https:\/\/demchaav\.github\.io\/GraphCompose\/(.+?)<\/loc>/g)]
    .map((m) => m[1])
    .filter((loc) => loc.endsWith(".pdf"));
  assert.ok(locs.length > 0, "the sitemap surfaces no documents, so this case would check nothing");
  for (const loc of locs) {
    assert.ok(pdfs.has(loc), `the sitemap sends a crawler to ${loc}, which no card publishes`);
  }
});

/** Every GraphCompose coordinate the built page tells a reader to add, Maven form and Gradle form. */
function coordinatesOnPage(page) {
  const found = [];
  const maven = /&lt;artifactId&gt;(graph-compose[\w-]*)&lt;\/artifactId&gt;\s*&lt;version&gt;([^&]+)&lt;\/version&gt;/g;
  for (const [, artifact, version] of page.matchAll(maven)) found.push({ form: "Maven", artifact, version });
  const gradle = /io\.github\.demchaav:(graph-compose[\w-]*):([^\s'")<]+)/g;
  for (const [, artifact, version] of page.matchAll(gradle)) found.push({ form: "Gradle", artifact, version });
  return found;
}

test("every install scenario offers its coordinate in both forms, at the release", () => {
  // VersionConsistencyGuardTest reads the bare graph-compose coordinate on this page and no other, so
  // a bundle or backend snippet pinned to an old release would pass it; this holds all of them. Each
  // artifact has to be read in both forms: a snippet the pattern cannot read — a line slipped in
  // between artifactId and version, say — would otherwise go unchecked while its other form passed.
  const coordinates = coordinatesOnPage(built["index.html"]);
  for (const companion of ["graph-compose-fonts", "graph-compose-emoji"]) {
    assert.ok(!coordinates.some((c) => c.artifact === companion),
      `the page offers ${companion}, which is versioned apart from the release; the bundle pins it`);
  }
  for (const artifact of ["graph-compose", "graph-compose-bundle", "graph-compose-render-pptx", "graph-compose-render-docx"]) {
    for (const form of ["Maven", "Gradle"]) {
      const found = coordinates.filter((c) => c.artifact === artifact && c.form === form);
      assert.equal(found.length, 1, `expected one readable ${form} coordinate for ${artifact}, found ${found.length}`);
      assert.equal(found[0].version, release.stableVersion, `the ${form} coordinate for ${artifact} names ${found[0].version}`);
    }
  }
});

const featured = JSON.parse(fs.readFileSync(path.join(root, "web-src", "data", "featured.json"), "utf8"));
const cardsById = new Map(cards.map((card) => [card.id, card]));

test("the hero leads with a whole document a reader without JavaScript can open", () => {
  const page = built["index.html"];
  const first = cardsById.get(featured.hero[0].id);
  assert.ok(first, "the first hero entry is not a card in the catalogue");
  const image = page.match(/<img class="hero-document-image"[^>]*>/);
  assert.ok(image, "the built page has no hero document image");
  assert.match(image[0], new RegExp(`src="${first.screenshot}"`));
  // The size attributes are what reserve the document's space before the image arrives.
  assert.match(image[0], new RegExp(`width="${first.previewWidth}"\\s+height="${first.previewHeight}"`));
  assert.match(page, new RegExp(`data-hero-pdf href="${first.pdf}"`));
  assert.match(page, new RegExp(`data-hero-page href="${pageOf(first.id)}">`), "the hero's second link is the document's own page");
});

test("the hero's switch waits for a script, and its page link does not", () => {
  const page = built["index.html"];
  assert.match(page, /<div class="hero-switch"[^>]*\bdata-hero-switch hidden>/);
  const link = page.match(/<a class="hero-document-link" data-hero-page[^>]*>/);
  assert.ok(link, "the hero has no link to the document's page");
  assert.doesNotMatch(link[0], /\shidden\b/, "a page is a link like any other, and needs no script to be followed");
});

const decodeHtml = (text) =>
  text.replace(/&(?:amp|lt|gt|quot);/g, (entity) => ({ "&amp;": "&", "&lt;": "<", "&gt;": ">", "&quot;": '"' })[entity]);

test("the hero's PDF link works without JavaScript", () => {
  const pdfLink = built["index.html"].match(/<a class="hero-document-link" data-hero-pdf[^>]*>/);
  assert.ok(pdfLink, "the hero has no PDF link");
  assert.doesNotMatch(pdfLink[0], /\shidden\b/, "the PDF link is the one control a reader without JavaScript has");
});

test("every hero option carries its own card's files and page", () => {
  const options = [
    ...built["index.html"].matchAll(/<button type="button" class="hero-switch-option"([\s\S]*?)>([^<]*)<\/button>/g),
  ];
  assert.equal(options.length, featured.hero.length, "one switch option per hero entry");
  options.forEach(([, attributes, label], index) => {
    const entry = featured.hero[index];
    const card = cardsById.get(entry.id);
    const attribute = (name) => (attributes.match(new RegExp(`${name}="([^"]*)"`)) || [])[1];
    assert.equal(label, entry.label);
    assert.equal(attribute("aria-pressed"), String(index === 0), "only the first option starts pressed");
    assert.equal(decodeHtml(attribute("data-title")), card.title);
    assert.equal(attribute("data-screenshot"), card.screenshot);
    assert.equal(attribute("data-pdf"), card.pdf);
    assert.equal(attribute("data-width"), String(card.previewWidth));
    assert.equal(attribute("data-height"), String(card.previewHeight));
    assert.equal(attribute("data-page"), pageOf(card.id));
  });
});

test("a hero entry that names no card stops the build", () => {
  assert.throws(
    () => build({ featured: { ...featured, hero: [{ id: "no-such-card", label: "Nothing" }] } }),
    /no-such-card/
  );
});

/* ---------------------------------------------------------------------------
 * The document pages: one per card at <category>/<group>/<id>/index.html.
 * ------------------------------------------------------------------------ */

/** Every page the build owns: a page per card, and the documentation page. */
const generatedPages = Object.keys(built).filter((name) => name.endsWith("/index.html"));
const pageFile = (id) => `${pageOf(id)}index.html`;

test("every card has exactly one page, and the build writes no page beyond them and the documentation", () => {
  const cardPages = generatedPages.filter((name) => name.split("/").length === 4);
  assert.deepEqual(new Set(cardPages), new Set(cards.map((card) => pageFile(card.id))));
  assert.equal(cardPages.length, cards.length);
  assert.deepEqual(generatedPages.filter((name) => !cardPages.includes(name)), ["documentation/index.html"]);
  for (const name of generatedPages) {
    assert.ok(built[name].includes(GENERATOR_MARK), `web/${name} does not carry the mark the build keys ownership on`);
  }
});

test("the committed tree holds no generated page that no card builds", () => {
  // A card renamed or removed leaves its old page behind; the build deletes it, and --check fails
  // on it. Read from disk: the committed tree is what ships.
  assert.deepEqual(orphanedPages(built), [], "these generated pages are orphans — run node scripts/site/build.mjs");
  // The card pages are the ones the build owns; it writes the documentation page by name.
  assert.deepEqual(new Set(ownedPages()), new Set(cards.map((card) => pageFile(card.id))));
});

/** A throwaway site root with the given files, handed to `body`, and removed afterwards. */
function withSite(files, body) {
  const site = fs.mkdtempSync(path.join(os.tmpdir(), "site-build-"));
  try {
    for (const [name, text] of Object.entries(files)) {
      fs.mkdirSync(path.dirname(path.join(site, name)), { recursive: true });
      fs.writeFileSync(path.join(site, name), text);
    }
    body(site);
  } finally {
    fs.rmSync(site, { recursive: true, force: true });
  }
}

/** A page as the build marks one it wrote at `directory`. */
const ownedAt = (directory, body = "") => `<head>${GENERATOR_MARK}<link rel="canonical" href="${SITE}${directory}"></head>${body}`;
const existsIn = (site, name) => fs.existsSync(path.join(site, ...name.split("/")));

test("an orphan is a page the build wrote where it sits and no card builds — never another file", () => {
  withSite({
    "templates/cv/kept/index.html": ownedAt("templates/cv/kept/"),
    "templates/cv/gone/index.html": ownedAt("templates/cv/gone/"),
    "oldcategory/family/card/index.html": ownedAt("oldcategory/family/card/"),
    "assets/notes/index.html": "<p>written by hand</p>",
    // A generated page copied elsewhere to begin a page by hand: it carries the mark, but its
    // canonical address still names the page it was copied from.
    "guides/start/index.html": ownedAt("templates/cv/kept/", "<p>my own guide</p>"),
    // The same at a card page's depth, where only the canonical address tells the two apart.
    "templates/cv/draft/index.html": ownedAt("templates/cv/kept/", "<p>my own draft</p>"),
    // The documentation page copied to begin another page of its shape, its canonical corrected:
    // mark and address both match, but a page one directory down is never a card's.
    "handbook/index.html": ownedAt("handbook/", "<p>my own handbook</p>"),
    "documentation/index.html": ownedAt("documentation/"),
    // Under showcase/ at a card page's depth: the catalogue's tree is never the build's to walk.
    "showcase/pdf/probe/index.html": ownedAt("showcase/pdf/probe/"),
  }, (site) => {
    const expected = { "templates/cv/kept/index.html": "" };
    assert.deepEqual(orphanedPages(expected, site), ["oldcategory/family/card/index.html", "templates/cv/gone/index.html"],
      "a page under a category removed outright is an orphan too; a hand-written page, a copied page and anything under showcase/ are not");

    for (const orphan of orphanedPages(expected, site)) removePage(orphan, site);
    assert.equal(existsIn(site, "templates/cv/gone"), false, "the orphan's empty directory stays behind");
    assert.equal(existsIn(site, "oldcategory"), false, "a removed category's empty directories stay behind");
    for (const kept of ["templates/cv/kept/index.html", "assets/notes/index.html", "guides/start/index.html",
      "templates/cv/draft/index.html", "handbook/index.html", "documentation/index.html", "showcase/pdf/probe/index.html"]) {
      assert.equal(existsIn(site, kept), true, `${kept} was deleted`);
    }
  });
});

test("--check reports a missing, a stale and an orphaned page, and changes nothing", () => {
  const pages = { "a/b/fresh/index.html": ownedAt("a/b/fresh/"), "a/b/stale/index.html": ownedAt("a/b/stale/", "new"), "a/b/missing/index.html": ownedAt("a/b/missing/") };
  withSite({
    "a/b/fresh/index.html": pages["a/b/fresh/index.html"],
    "a/b/stale/index.html": ownedAt("a/b/stale/", "old"),
    "a/b/orphan/index.html": ownedAt("a/b/orphan/"),
  }, (site) => {
    const problems = checkSite(pages, site).join("\n");
    assert.match(problems, /web\/a\/b\/missing\/index\.html is missing/);
    assert.match(problems, /web\/a\/b\/stale\/index\.html is not what web-src\/ builds/);
    assert.match(problems, /web\/a\/b\/orphan\/index\.html is a generated page that no card/);
    assert.doesNotMatch(problems, /fresh/);
    // A check that tidied the tree would leave CI a clean one to pass.
    assert.equal(existsIn(site, "a/b/orphan/index.html"), true, "--check deleted the orphan it reported");
    assert.equal(existsIn(site, "a/b/missing/index.html"), false, "--check wrote the page it reported missing");
    assert.match(fs.readFileSync(path.join(site, "a/b/stale/index.html"), "utf8"), /old/, "--check rewrote the stale page");
  });
});

test("a build writes what differs, deletes orphans, and a second build does nothing", () => {
  const pages = { "a/b/fresh/index.html": ownedAt("a/b/fresh/"), "a/b/stale/index.html": ownedAt("a/b/stale/", "new"), "a/b/missing/index.html": ownedAt("a/b/missing/") };
  withSite({
    "a/b/fresh/index.html": pages["a/b/fresh/index.html"],
    "a/b/stale/index.html": ownedAt("a/b/stale/", "old"),
    "a/b/orphan/index.html": ownedAt("a/b/orphan/"),
  }, (site) => {
    const first = writeSite(pages, site);
    assert.deepEqual(first.written.sort(), ["a/b/missing/index.html", "a/b/stale/index.html"]);
    assert.deepEqual(first.deleted, ["a/b/orphan/index.html"]);
    assert.equal(first.unchanged, 1);
    assert.deepEqual(checkSite(pages, site), [], "the build left the tree its own check fails");
    const second = writeSite(pages, site);
    assert.deepEqual({ written: second.written, deleted: second.deleted, unchanged: second.unchanged }, { written: [], deleted: [], unchanged: 3 });
  });
});

test("the build's own --check passes on the committed tree", () => {
  // The command CI's release checks and a contributor run, not only the functions behind it.
  const run = spawnSync(process.execPath, [path.join(root, "scripts", "site", "build.mjs"), "--check"], { encoding: "utf8" });
  assert.equal(run.status, 0, `node scripts/site/build.mjs --check failed:\n${run.stderr}`);
  assert.match(run.stdout, /the committed pages match web-src\//);
});

const decodeText = (text) =>
  text.replace(/&(?:amp|lt|gt|quot|#39);/g, (entity) => ({ "&amp;": "&", "&lt;": "<", "&gt;": ">", "&quot;": '"', "&#39;": "'" })[entity]);

test("a page names its own address, its title and its description, to crawlers and to link previews", () => {
  for (const card of cards) {
    const page = built[pageFile(card.id)];
    const canonical = `${SITE}${pageOf(card.id)}`;
    const meta = (key) => {
      const found = page.match(new RegExp(`<meta (?:name|property)="${key}" content="([^"]*)">`));
      assert.ok(found, `${card.id} has no ${key}`);
      return decodeText(found[1]);
    };
    assert.match(page, new RegExp(`<link rel="canonical" href="${canonical}">`), card.id);
    const title = decodeText(page.match(/<title>([^<]*)<\/title>/)[1]);
    assert.equal(title.startsWith(`${card.title} · `), true, card.id);
    assert.equal(meta("description"), card.description, card.id);
    assert.deepEqual(
      [meta("og:url"), meta("og:title"), meta("og:description"), meta("og:image"), meta("og:image:width"), meta("og:image:height")],
      [canonical, title, card.description, SITE + card.screenshot, String(card.previewWidth), String(card.previewHeight)],
      `${card.id}: a link preview would show another page, title or image`);
    const data = JSON.parse(page.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/)[1]);
    assert.equal(data["@type"], "WebPage");
    assert.equal(data.url, canonical, `${card.id}: the structured data names another address`);
    assert.equal(data.name, card.title);
    assert.equal(data.primaryImageOfPage.url, SITE + card.screenshot);
    assert.equal(data.mainEntity.url, SITE + card.pdf);
  }
});

test("a title or description cannot close the structured data or reach the page as markup", () => {
  const hostile = '</script><script>alert(1)</script> & "quoted" <b>bold</b> <!-- ';
  const first = cards[0];
  const pages = build({ manifest: manifestWith((m) => {
    Object.assign(m.categories[0].groups[0].examples[0], { title: hostile, description: `${hostile} U+2028: .` });
  }) });
  const page = pages[pageFile(first.id)];
  const block = page.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/);
  assert.ok(block, "the page lost its structured data block");
  assert.doesNotMatch(block[1], /</, "a raw < inside the JSON-LD can end the script element");
  assert.equal(JSON.parse(block[1]).name, hostile, "the structured data no longer carries the title as written");
  assert.equal(page.includes("<script>alert(1)"), false, "a card's text reached the page as markup");
  assert.equal(page.includes("<b>bold</b>"), false, "a card's text reached the page as markup");
});

test("the sitemap lists every document's page", () => {
  const locs = new Set([...built["sitemap.xml"].matchAll(/<loc>([^<]+)<\/loc>/g)].map((m) => m[1]));
  for (const card of cards) {
    assert.ok(locs.has(`https://demchaav.github.io/GraphCompose/${pageOf(card.id)}`), `the sitemap does not list ${card.id}`);
  }
});

/** The PNG size a site file states in its header, read independently of the build. */
function pngSizeOf(sitePath) {
  const bytes = fs.readFileSync(path.join(root, "web", sitePath));
  return { width: bytes.readUInt32BE(16), height: bytes.readUInt32BE(20) };
}

test("a page shows every page of its document, each sized from the image it shows", () => {
  for (const card of cards) {
    const page = built[pageFile(card.id)];
    const figures = [...page.matchAll(/<figure class="document-page-image">([\s\S]*?)<\/figure>/g)].map((m) => m[1]);
    const expected = [card.screenshot, ...(card.pages || [])];
    const total = expected.length;
    assert.equal(figures.length, total, `${card.id} shows ${figures.length} of its ${total} pages`);
    figures.forEach((figure, index) => {
      const [, pdfLink, src, width, height, alt] =
        figure.match(/<a href="([^"]+)">\s*<img src="([^"]+)" width="(\d+)" height="(\d+)"\s+alt="([^"]*)"/) || [];
      assert.equal(src, `../../../${expected[index]}`, card.id);
      assert.equal(pdfLink, `../../../${card.pdf}#page=${index + 1}`, `${card.id}: page ${index + 1} opens another page of the PDF`);
      assert.deepEqual({ width: Number(width), height: Number(height) }, pngSizeOf(expected[index]), `${card.id} page ${index + 1}`);
      assert.equal(decodeText(alt), `${card.title}, page ${index + 1} of ${total}`, `${card.id} page ${index + 1}: the image's name`);
      const caption = figure.match(/<figcaption>([^<]*)<\/figcaption>/);
      // "Page 1 of 1" under the only page says nothing a reader needs.
      assert.equal(caption && caption[1], total > 1 ? `Page ${index + 1} of ${total}` : null, `${card.id} page ${index + 1}: its caption`);
    });
    assert.match(page, new RegExp(`<p class="document-facts">${total} page${total === 1 ? "" : "s"}</p>`), `${card.id}: its page count`);
    assert.equal(total, card.pageCount, `${card.id}: the page shows a different number of pages than the catalogue measured`);
  }
});

test("a page lists the other documents of its family, and a family of one lists none", () => {
  let listed = 0;
  let alone = 0;
  for (const card of cards) {
    const { group } = placeOf(card.id);
    const section = built[pageFile(card.id)].match(/<section class="document-related"[\s\S]*?<\/section>/);
    const others = group.examples.filter((other) => other.id !== card.id);
    if (others.length === 0) {
      assert.equal(section, null, `${card.id} is its family's only document, and lists an empty family`);
      alone++;
      continue;
    }
    assert.ok(section, `${card.id} lists none of the ${others.length} other documents of its family`);
    assert.equal(decodeText(section[0].match(/<h2 id="related-title">([^<]*)<\/h2>/)[1]), `More in ${group.label}`);
    const entries = [...section[0].matchAll(/<a href="([^"]+)">\s*<img src="([^"]+)"[^>]*>\s*<span>([^<]*)<\/span>/g)];
    assert.deepEqual(
      entries.map(([, href, src, name]) => [href, src, decodeText(name)]),
      others.map((other) => [`../../../${pageOf(other.id)}`, `../../../${other.thumbnail}`, other.title]),
      `${card.id}: the family list is not every other document of the family, in order`);
    listed++;
  }
  assert.ok(listed > 0 && alone > 0, "fixture: the catalogue has families of one and of several");
});

/**
 * The page's reproduction section, read back into the shape of the panel model — and refused if
 * the section holds anything besides what that shape can say: a note, another link or markup inside
 * a value would otherwise be skipped by the reader and the page could say more than the viewer does.
 */
function reproductionOf(page) {
  const section = page.match(/<section class="document-reproduce"[^>]*>([\s\S]*?)<\/section>/);
  assert.ok(section, "the page has no reproduction section");
  const heading = /<h2 id="reproduce-title">([^<]*)<\/h2>/;
  const item = /<figure class="reproduce-listing">\s*<figcaption class="reproduce-label">([^<]*)<\/figcaption>\s*<pre class="reproduce-code"><code>([^<]*)<\/code><\/pre>\s*<\/figure>|<p class="reproduce-row"><span class="reproduce-label">([^<]*)<\/span> <(span|code) class="reproduce-value">([^<]*)<\/\4><\/p>/g;
  const link = /<a class="button button-secondary button-small" href="([^"]*)">([^<]*)<\/a>/g;
  const items = [];
  for (const [, listingLabel, code, rowLabel, element, value] of section[1].matchAll(item)) {
    items.push(listingLabel !== undefined
      ? { label: decodeText(listingLabel), code: decodeText(code) }
      : element === "code"
        ? { label: decodeText(rowLabel), value: decodeText(value), literal: true }
        : { label: decodeText(rowLabel), value: decodeText(value) });
  }
  const links = [...section[1].matchAll(link)].map(([, href, text]) => ({ text: decodeText(text), href: decodeText(href) }));
  const rest = section[1]
    .replace(heading, "")
    .replace(item, "")
    .replace(/<p class="reproduce-links">([\s\S]*?)<\/p>/, (_, inner) => inner.replace(link, ""))
    .trim();
  assert.equal(rest, "", `the reproduction section holds something the panel model does not say: ${rest.slice(0, 120)}`);
  return { action: decodeText(section[1].match(heading)[1]), items, links };
}

test("a page's reproduction section is the viewer's panel model, item for item", () => {
  const viewerCatalogue = {
    snippets: manifest.snippets,
    get: (id) => {
      const place = placeOf(id);
      return place && { categoryId: place.category.id, groupId: place.group.id, example: cardsById.get(id) };
    },
  };
  for (const card of cards) {
    const model = JSON.parse(JSON.stringify(gallery.panelModel(card, viewerCatalogue, release)));
    assert.deepEqual(reproductionOf(built[pageFile(card.id)]), model,
      `${card.id}: the page tells a reader something the viewer's panel does not`);
  }
});

test("every coordinate on every page names the release", () => {
  let seen = 0;
  for (const name of generatedPages) {
    for (const coordinate of coordinatesOnPage(built[name])) {
      seen++;
      assert.equal(coordinate.version, release.stableVersion, `web/${name} offers ${coordinate.artifact} at ${coordinate.version}`);
      assert.ok(!["graph-compose-fonts", "graph-compose-emoji"].includes(coordinate.artifact),
        `web/${name} offers ${coordinate.artifact}, which is versioned apart from the release`);
    }
  }
  assert.ok(seen >= cards.length * 2, "the pages carry too few coordinates for this case to be reading them");
});

test("every guide or source link the site writes names the release, the licence apart", () => {
  // The panel pinned its links to the release from the start; a footer or a menu still on main put
  // two versions of the same guide on one page. The licence does not vary by release.
  let seen = 0;
  for (const name of ["index.html", ...generatedPages]) {
    for (const [, ref, rest] of built[name].matchAll(/github\.com\/DemchaAV\/GraphCompose\/(?:blob|tree)\/([^/"]+)\/([^"]*)/g)) {
      seen++;
      if (rest === "LICENSE") continue;
      assert.equal(ref, release.releaseTag, `web/${name} links ${rest} at ${ref}`);
    }
  }
  assert.ok(seen > generatedPages.length, "too few repository links read for this case to be checking the pages");
});

const documentation = JSON.parse(fs.readFileSync(path.join(root, "web-src", "data", "documentation.json"), "utf8"));

test("the documentation page links every guide at the release, and every guide is a file in the repository", () => {
  const page = built["documentation/index.html"];
  const linked = [...page.matchAll(/<ul class="docs-guides">([\s\S]*?)<\/ul>/g)]
    .flatMap((list) => [...list[1].matchAll(/<a href="([^"]+)">([^<]*)<\/a>\s*<p>([^<]*)<\/p>/g)])
    .map(([, href, title, summary]) => ({ href: decodeText(href), title: decodeText(title), summary: decodeText(summary) }));
  const guides = documentation.sections.flatMap((section) => section.guides);
  assert.ok(guides.length > 10, "fixture: the documentation data lists the guides");
  assert.deepEqual(linked, guides.map((guide) => ({
    href: `https://github.com/DemchaAV/GraphCompose/blob/${release.releaseTag}/${guide.path}`,
    title: guide.title,
    summary: guide.summary,
  })));
  for (const guide of guides) {
    assert.ok(fs.statSync(path.join(root, guide.path), { throwIfNoEntry: false })?.isFile(), `${guide.path} is not a file`);
  }
  for (const section of documentation.sections) {
    assert.match(page, new RegExp(`<section class="docs-section" id="${section.id}" aria-labelledby="${section.id}-title">`));
  }
  assert.match(page, new RegExp(`<link rel="canonical" href="${SITE}documentation/">`));
  assert.ok(built["sitemap.xml"].includes(`<loc>${SITE}documentation/</loc>`), "the sitemap does not list the documentation page");
});

test("a guide that is not a file in the repository stops the build", () => {
  const naming = (guidePath) => ({ sections: [{ id: "start", title: "Start", guides: [{ title: "Gone", path: guidePath, summary: "Nothing." }] }] });
  assert.throws(() => build({ documentation: naming("docs/no-such-guide.md") }), /the guide "Gone" names docs\/no-such-guide\.md, which is not a file/);
  assert.throws(() => build({ documentation: naming("docs") }), /which is not a file in the repository/, "a directory is not a guide");
  assert.throws(() => build({ documentation: naming("/CHANGELOG.md") }), /which is not a file in the repository/, "a path is repository-relative");
  // A file that does exist, outside the repository: refused for where it is, not for being absent.
  const outside = fs.mkdtempSync(path.join(os.tmpdir(), "site-guide-"));
  try {
    const file = path.join(outside, "guide.md");
    fs.writeFileSync(file, "# a file outside the repository\n");
    const relative = path.relative(root, file).split(path.sep).join("/");
    assert.throws(() => build({ documentation: naming(relative) }), /which is not a file in the repository/,
      `${relative} exists, but not in the repository the page links into`);
  } finally {
    fs.rmSync(outside, { recursive: true, force: true });
  }
  // GitHub is case-sensitive; a Windows or macOS checkout is not, and would find this file.
  assert.throws(() => build({ documentation: naming("DOCS/README.md") }), /which is not a file in the repository/,
    "a guide spelled in another case is a 404 on GitHub");
});

test("the documentation data is refused when a section or a guide is incomplete", () => {
  const guide = { title: "First", path: "docs/first-document.md", summary: "A first document." };
  const section = (fields) => ({ id: "start", title: "Start", guides: [guide], ...fields });
  const refusals = [
    [{ sections: [] }, /no sections/],
    [{}, /no sections/],
    [{ sections: [section({ id: "Start Here" })] }, /section id "Start Here" is missing, repeated or not address-safe/],
    [{ sections: [section({}), section({})] }, /section id "start" is missing, repeated or not address-safe/],
    [{ sections: [section({ title: "" })] }, /the section "start" has no title or no guides/],
    [{ sections: [section({ guides: [] })] }, /the section "start" has no title or no guides/],
    [{ sections: [section({ guides: [{ ...guide, summary: "" }] })] }, /a guide in "start" has no summary/],
    [{ sections: [section({ guides: [{ title: "First", summary: "A first document." }] })] }, /a guide in "start" has no path/],
  ];
  for (const [documentation, message] of refusals) {
    assert.throws(() => build({ documentation }), message, JSON.stringify(documentation));
  }
});

test("a guide's words reach the documentation page as text, never as markup", () => {
  const pages = build({ documentation: { sections: [{
    id: "start", title: "Start & <finish>", intro: "Read \"this\" & <that>",
    guides: [{ title: "Guides & <tips>", path: "docs/first-document.md", summary: "Say \"hi\" <b>now</b>" }],
  }] } });
  const page = pages["documentation/index.html"];
  for (const escaped of ["Start &amp; &lt;finish&gt;", "Read &quot;this&quot; &amp; &lt;that&gt;", "Guides &amp; &lt;tips&gt;",
    "Say &quot;hi&quot; &lt;b&gt;now&lt;/b&gt;"]) {
    assert.ok(page.includes(escaped), `the page does not carry ${escaped}`);
  }
  for (const raw of ["<finish>", "<that>", "<tips>", "<b>now</b>"]) {
    assert.equal(page.includes(raw), false, `${raw} reached the page as markup`);
  }
});

test("a new release moves every link the site pins to one", () => {
  // Checked against a release that is not the committed one, so a tag written into a template or a
  // partial by hand cannot pass for one the build injected.
  const next = { ...release, stableVersion: "9.9.9", releaseTag: "v9.9.9" };
  const pages = build({ release: next });
  let pinned = 0;
  for (const [name, content] of Object.entries(pages)) {
    for (const [, ref, rest] of content.matchAll(/github\.com\/DemchaAV\/GraphCompose\/(?:blob|tree)\/([^/"]+)\/([^"]*)/g)) {
      if (rest === "LICENSE") continue;
      pinned++;
      assert.equal(ref, "v9.9.9", `web/${name} still links ${rest} at ${ref}`);
    }
    assert.equal(content.includes(release.releaseTag + "/"), false, `web/${name} still names ${release.releaseTag} in a link`);
  }
  assert.ok(pinned > cards.length, "too few pinned links read for this case to be checking the pages");
});

test("the menu on every page leads to the documentation page", () => {
  for (const name of ["index.html", ...generatedPages]) {
    const nav = built[name].match(/<nav class="site-nav"[\s\S]*?<\/nav>/);
    assert.ok(nav, `web/${name} has no menu`);
    const link = nav[0].match(/<a href="([^"]*)">Documentation<\/a>/);
    assert.ok(link, `web/${name}'s menu has no Documentation link`);
    assert.equal(new URL(link[1], new URL(name, SITE)).href, `${SITE}documentation/`, `web/${name}'s menu leads elsewhere`);
  }
});

/**
 * Where a link written on a page lands: a site file, or an anchor into the home page. Only links
 * that stay inside the site are returned.
 */
function siteTargets(pageName, html) {
  const targets = [];
  // Resolved where Pages serves the site. From the root of an origin a surplus `../` is absorbed
  // silently; under /GraphCompose/ it leaves the site, which is the 404 a reader would meet.
  const base = new URL(pageName, SITE);
  for (const [, attribute] of html.matchAll(/\s(?:href|src)="([^"]*)"/g)) {
    const value = decodeText(attribute);
    if (/^[a-z][a-z0-9+.-]*:/i.test(value) && !value.startsWith(SITE)) continue;
    const absolute = new URL(value, base);
    const inside = absolute.origin === new URL(SITE).origin && absolute.pathname.startsWith(new URL(SITE).pathname);
    const file = inside ? decodeURIComponent(absolute.pathname.slice(new URL(SITE).pathname.length)) : null;
    targets.push({
      value,
      file: file === null ? `(outside the site: ${absolute.pathname})` : file === "" || file.endsWith("/") ? `${file}index.html` : file,
      hash: decodeURIComponent(absolute.hash.slice(1)),
    });
  }
  return targets;
}

test("every link on every page lands on a file the site publishes, or an anchor the home page has", () => {
  const categories = new Set(manifest.categories.map((category) => category.id));
  const homeIds = new Set([...built["index.html"].matchAll(/\sid="([^"]+)"/g)].map((m) => m[1]));
  let checked = 0;
  for (const name of generatedPages) {
    for (const target of siteTargets(name, built[name])) {
      checked++;
      const published = target.file in built || fs.existsSync(path.join(root, "web", target.file));
      assert.ok(published, `web/${name} links ${target.value}, and the site publishes no ${target.file}`);
      if (target.file === "index.html" && target.hash) {
        const route = gallery.parseRoute(`#${target.hash}`);
        const lands = route
          ? cards.some((card) => {
            const place = placeOf(card.id);
            return place.category.id === route.category && place.group.id === route.group && (!route.id || route.id === card.id);
          })
          : target.hash.endsWith("-section")
            ? categories.has(target.hash.slice(0, -"-section".length))
            : homeIds.has(target.hash);
        assert.ok(lands, `web/${name} links ${target.value}, which lands on nothing on the home page`);
      }
    }
  }
  assert.ok(checked > generatedPages.length * 10, "too few links read for this case to be checking the pages");
});

/** The real manifest with one card changed, for the refusals a page needs. */
function withFirstCard(edit) {
  return manifestWith((m) => edit(m.categories[0].groups[0].examples[0], m));
}

test("an id that would leave its page's directory stops the build before anything is written", () => {
  assert.throws(() => build({ manifest: withFirstCard((card) => { card.id = "../escape"; }) }), /not lowercase words joined by hyphens/);
  assert.throws(() => build({ manifest: manifestWith((m) => { m.categories[0].groups[0].id = "cv/x"; }) }), /not lowercase words/);
});

test("a category that would write its pages into a static tree stops the build", () => {
  assert.throws(() => build({ manifest: manifestWith((m) => { m.categories[0].id = "showcase"; }) }), /would write its pages into web\/showcase/);
  assert.throws(() => build({ manifest: manifestWith((m) => { m.categories[0].id = "documentation"; }) }), /would write its pages into web\/documentation/);
});

test("two cards with one id stop the build", () => {
  assert.throws(
    () => build({ manifest: manifestWith((m) => { m.categories[0].groups[0].examples[1].id = m.categories[0].groups[0].examples[0].id; }) }),
    /two cards have the id/
  );
});

test("a page image that is not published stops the build", () => {
  assert.throws(
    () => build({ manifest: withFirstCard((card) => { card.pages = ["showcase/pages/templates/cv/no-such-page-2.png"]; }) }),
    /no-such-page-2\.png is not a file under web\//
  );
});

test("a card with no description stops the build rather than publishing a page without one", () => {
  assert.throws(() => build({ manifest: withFirstCard((card) => { delete card.description; }) }), /has no description/);
});

if (failures.length > 0) {
  console.error(`\nsite-build: ${failures.length} case(s) failed`);
  process.exit(1);
}
console.log("site-build: all cases passed");
