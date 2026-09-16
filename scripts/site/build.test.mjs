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
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { build, render } from "./build.mjs";

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

test("the no-JavaScript index names every document in the catalogue", () => {
  const noscript = built["index.html"].match(/<noscript>([\s\S]*?)<\/noscript>/)[1];
  const linked = [...noscript.matchAll(/<li><a href="([^"]+)"/g)].map((m) => m[1]);
  assert.equal(
    linked.length,
    cards.length,
    "a card in the catalogue reaches no visitor without JavaScript — the point of generating this block"
  );
  assert.deepEqual(new Set(linked), new Set(cards.map((card) => card.pdf)));
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
  assert.match(built["index.html"], new RegExp(`Java &middot; ${release.releaseTag} &middot; MIT`));
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

if (failures.length > 0) {
  console.error(`\nsite-build: ${failures.length} case(s) failed`);
  process.exit(1);
}
console.log("site-build: all cases passed");
