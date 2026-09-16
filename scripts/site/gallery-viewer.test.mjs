/**
 * node scripts/site/gallery-viewer.test.mjs — exit 0 when every case holds.
 *
 * Loads web/gallery-viewer.js the way the page does, as a plain script, and checks what
 * needs no DOM: reading and writing viewer addresses, and navigating the catalogue that
 * web/examples.json describes. The page wiring is checked too: index.html has to load
 * the viewer before examples.js and carry the dialog the viewer drives. The dialog itself
 * is then run against a DOM small enough to live in this file, which is how the history
 * entries, the close, the image token and the focus are checked without a browser.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const web = (name) => fs.readFileSync(path.join(root, "web", name), "utf8");

const sandbox = {};
vm.runInNewContext(web("gallery-viewer.js"), sandbox, { filename: "web/gallery-viewer.js" });
const gallery = sandbox.GraphComposeGallery;
// Values made inside the sandbox belong to another realm; compare them as plain data.
const plain = (value) => JSON.parse(JSON.stringify(value));

/** The catalogue examples.js hands the viewer, built from a manifest the same way. */
function catalogueOf(manifest) {
  const index = new Map();
  const categories = manifest.categories.map((category) => ({
    id: category.id,
    label: category.label,
    groups: category.groups.map((group) => {
      for (const example of group.examples) {
        index.set(example.id, { categoryId: category.id, groupId: group.id, example });
      }
      return { id: group.id, label: group.label, ids: group.examples.map((example) => example.id) };
    }),
  }));
  return { categories, snippets: manifest.snippets || {}, get: (id) => index.get(id) };
}

const manifest = JSON.parse(web("examples.json"));
const catalogue = catalogueOf(manifest);
const idsOf = (categoryId, groupId) =>
  catalogue.categories.find((c) => c.id === categoryId).groups.find((g) => g.id === groupId).ids;

// The release block index.html carries, which is where the panel's coordinates come from. Read
// from the page rather than repeated here: a cut moves that block, and a copy of it in this file
// would keep the suite green while asserting against a release the site no longer shows.
const RELEASE = JSON.parse(
  web("index.html").match(/<script type="application\/json" id="release-context">([\s\S]*?)<\/script>/)[1]
);

let failures = 0;
function check(name, body) {
  try { body(); console.log(`  ok    ${name}`); }
  catch (error) { failures++; console.log(`  FAIL  ${name}\n        ${error.message.split("\n")[0]}`); }
}

check("a family address and a document address are read", () => {
  assert.deepEqual(plain(gallery.parseRoute("#/templates/cv")), { category: "templates", group: "cv", id: null });
  assert.deepEqual(plain(gallery.parseRoute("#/templates/cv/cv-blue-banner-v2")),
    { category: "templates", group: "cv", id: "cv-blue-banner-v2" });
});

check("anything else is not a viewer address", () => {
  for (const hash of ["", "#", "#showcase", "#templates-section", "#/", "#/templates", "#/templates/",
    "#//cv", "#/templates//x", "#/templates/cv/", "#/templates/cv/x/y", "#/templates/cv/%E0%A4%A",
    "/templates/cv", null, 42]) {
    assert.equal(gallery.parseRoute(hash), null, String(hash));
  }
});

check("an address is written back the way it was read, escaping what needs it", () => {
  for (const hash of ["#/templates/cv", "#/features/transforms/transforms", "#/templates/cv/cv-blue-banner-v2"]) {
    assert.equal(gallery.formatRoute(gallery.parseRoute(hash)), hash);
  }
  assert.equal(gallery.formatRoute({ category: "a b", group: "c/d", id: null }), "#/a%20b/c%2Fd");
  assert.deepEqual(plain(gallery.parseRoute("#/a%20b/c%2Fd")), { category: "a b", group: "c/d", id: null });
});

check("every card in the manifest resolves to itself", () => {
  const navigator = gallery.createNavigator(catalogue);
  let cards = 0;
  for (const category of manifest.categories) {
    for (const group of category.groups) {
      group.examples.forEach((example, index) => {
        const view = navigator.resolve({ category: category.id, group: group.id, id: example.id });
        assert.equal(view.status, "ok", example.id);
        assert.equal(view.index, index, example.id);
        cards++;
      });
    }
  }
  assert.ok(cards > 0, "fixture: the manifest lists no cards");
});

check("a family opens on its first document, then on the one it was left on", () => {
  const navigator = gallery.createNavigator(catalogue);
  const ids = idsOf("templates", "cv");
  assert.ok(ids.length > 2, "fixture: templates/cv needs at least three documents");
  assert.equal(navigator.resolve({ category: "templates", group: "cv", id: null }).id, ids[0]);
  navigator.resolve({ category: "templates", group: "cv", id: ids[2] });
  assert.equal(navigator.resolve({ category: "templates", group: "cv", id: null }).id, ids[2]);
});

check("Previous and Next stop at the ends of a family", () => {
  const navigator = gallery.createNavigator(catalogue);
  const ids = idsOf("templates", "cv");
  const first = navigator.resolve({ category: "templates", group: "cv", id: ids[0] });
  assert.equal(navigator.step(first, -1), null);
  assert.equal(navigator.step(first, 1).id, ids[1]);
  const last = navigator.resolve({ category: "templates", group: "cv", id: ids[ids.length - 1] });
  assert.equal(navigator.step(last, 1), null);
  assert.equal(navigator.step(last, -1).id, ids[ids.length - 2]);
});

check("a one-document family has nowhere to go", () => {
  const navigator = gallery.createNavigator(catalogueOf({ categories: [{ id: "c", label: "C", groups: [
    { id: "solo", label: "Solo", examples: [{ id: "only" }] },
    { id: "pair", label: "Pair", examples: [{ id: "a" }, { id: "b" }] }] }] }));
  const view = navigator.resolve({ category: "c", group: "solo", id: null });
  assert.equal(view.id, "only");
  assert.equal(navigator.step(view, 1), null);
  assert.equal(navigator.step(view, -1), null);
});

check("switching family reopens each family where it was left", () => {
  const navigator = gallery.createNavigator(catalogueOf({ categories: [{ id: "c", label: "C", groups: [
    { id: "x", label: "X", examples: [{ id: "x1" }, { id: "x2" }] },
    { id: "y", label: "Y", examples: [{ id: "y1" }, { id: "y2" }] }] }] }));
  const x2 = navigator.resolve({ category: "c", group: "x", id: "x2" });
  const y1 = navigator.switchFamily(x2, "y");
  assert.equal(y1.id, "y1");
  const y2 = navigator.step(y1, 1);
  const back = navigator.switchFamily(y2, "x");
  assert.equal(back.id, "x2");
  assert.deepEqual(plain(back.route), { category: "c", group: "x", id: "x2" });
  assert.equal(navigator.switchFamily(y2, "missing"), null);
});

check("an unknown document keeps its family, and an unknown family resolves to nothing", () => {
  const navigator = gallery.createNavigator(catalogue);
  const unknown = navigator.resolve({ category: "templates", group: "cv", id: "no-such-document" });
  assert.equal(unknown.status, "unknown-id");
  assert.equal(unknown.group.id, "cv");
  assert.equal(unknown.requested, "no-such-document");
  assert.equal(navigator.step(unknown, 1), null);
  assert.equal(navigator.resolve({ category: "templates", group: "no-such-family", id: null }).status, "unknown-group");
  assert.equal(navigator.resolve({ category: "no-such-category", group: "cv", id: null }).status, "unknown-group");
});

check("a document under an outdated address lands in the family it lives in", () => {
  const navigator = gallery.createNavigator(catalogue);
  const invoice = idsOf("templates", "invoice")[0];
  const moved = navigator.resolve({ category: "templates", group: "cv", id: invoice });
  assert.equal(moved.status, "ok");
  assert.deepEqual(plain(moved.route), { category: "templates", group: "invoice", id: invoice });
  assert.equal(navigator.resolve({ category: "gone", group: "gone", id: invoice }).route.group, "invoice");
});

check("a document whose id is also its family's id still resolves", () => {
  const navigator = gallery.createNavigator(catalogue);
  const view = navigator.resolve(gallery.parseRoute("#/features/transforms/transforms"));
  assert.equal(view.status, "ok", "fixture: features/transforms/transforms is not in the manifest");
  assert.equal(view.id, "transforms");
});

check("index.html loads the viewer before examples.js and carries its dialog", () => {
  const page = web("index.html");
  const viewerScript = page.indexOf('<script src="gallery-viewer.js"></script>');
  const pageScript = page.indexOf('<script src="examples.js"></script>');
  assert.ok(viewerScript >= 0, "index.html does not load gallery-viewer.js");
  assert.ok(pageScript > viewerScript, "gallery-viewer.js has to load before examples.js");
  assert.match(page, /<dialog[^>]*\bid="gallery-viewer"/);
  for (const part of ["families", "thumbnails", "stage", "image", "notice", "title", "counter",
    "description", "pdf", "details", "code", "previous", "next"]) {
    assert.ok(page.includes(`data-viewer="${part}"`), `the dialog has no data-viewer="${part}"`);
  }
});

/* ---------------------------------------------------------------------------
 * The dialog half of gallery-viewer.js. It touches only a handful of DOM
 * operations, so a DOM this small runs it: the history entries an open and a
 * move record, what closing hands back, the token that keeps a late preview off
 * a newer document, and the focus a disabled button must not take out of the
 * dialog. None of that is visible to the checks above, which build no viewer.
 * ------------------------------------------------------------------------ */

const ATTRIBUTE_SELECTOR = /^\[([a-z-]+)(?:="([^"]*)")?\]$/;
const attributeName = (key) => "data-" + String(key).replace(/[A-Z]/g, (c) => "-" + c.toLowerCase());

class StubClassList extends Set {
  remove(name) { this.delete(name); }
  contains(name) { return this.has(name); }
}

class StubNode {
  constructor(doc, tag, attributes = {}) {
    this.doc = doc;
    this.tag = tag;
    this.attributes = { ...attributes };
    this.children = [];
    this.parent = null;
    this.listeners = new Map();
    this.classList = new StubClassList();
    this.hidden = false;
    this.disabled = false;
    this.className = "";
    this.type = "";
    this.src = "";
    this.alt = "";
    this.text = "";
    this.tabIndex = 0;
    this.scrolledTo = null;
    // Only the ones the viewer measures; a stub never lays anything out.
    this.scrollWidth = 0;
    this.clientWidth = 0;
    this.scrollLeft = 0;
    this.offsetLeft = 0;
    this.offsetWidth = 0;
    const node = this;
    this.dataset = new Proxy({}, {
      get: (_, key) => node.attributes[attributeName(key)],
      set: (_, key, value) => { node.attributes[attributeName(key)] = String(value); return true; }
    });
  }
  get textContent() { return this.text; }
  set textContent(value) { this.text = String(value); this.children = []; }
  append(...nodes) { for (const node of nodes) { node.parent = this; this.children.push(node); } }
  setAttribute(name, value) { this.attributes[name] = String(value); }
  getAttribute(name) { return this.attributes[name]; }
  removeAttribute(name) { delete this.attributes[name]; if (name === "src") this.src = ""; }
  matches(selector) {
    const parsed = ATTRIBUTE_SELECTOR.exec(selector);
    if (!parsed) throw new Error("this DOM reads attribute selectors only, not: " + selector);
    const value = this.attributes[parsed[1]];
    return value !== undefined && (parsed[2] === undefined || value === parsed[2]);
  }
  descendants() {
    const found = [];
    const walk = (node) => { for (const child of node.children) { found.push(child); walk(child); } };
    walk(this);
    return found;
  }
  querySelector(selector) { return this.descendants().find((node) => node.matches(selector)) || null; }
  querySelectorAll(selector) { return this.descendants().filter((node) => node.matches(selector)); }
  closest(selector) { for (let node = this; node; node = node.parent) if (node.matches(selector)) return node; return null; }
  contains(node) { for (let walk = node; walk; walk = walk.parent) if (walk === this) return true; return false; }
  addEventListener(type, listener) { this.listeners.set(type, (this.listeners.get(type) || []).concat(listener)); }
  focus() { this.doc.activeElement = this; }
  scrollTo(options) { this.scrolledTo = options; this.scrollLeft = options.left; }
}

class StubDialog extends StubNode {
  constructor(doc) { super(doc, "dialog"); this.open = false; }
  showModal() { this.open = true; }
  close() { if (!this.open) return; this.open = false; dispatch(this, { type: "close" }); }
}

/** Runs the listeners of the target and of everything it sits inside, the way an event bubbles. */
function dispatch(target, event) {
  const path = [];
  for (let node = target; node; node = node.parent) path.push(node);
  path.push(target.doc);
  Object.assign(event, {
    target,
    defaultPrevented: false,
    preventDefault() { this.defaultPrevented = true; }
  });
  for (const node of path) {
    for (const listener of node.listeners.get(event.type) || []) listener(event);
  }
}

/** A viewer wired to a stub dialog shaped like the one in index.html. */
function viewerHarness(catalogueUnderTest = catalogue, release = RELEASE) {
  const doc = {
    activeElement: null,
    listeners: new Map(),
    addEventListener(type, listener) { doc.listeners.set(type, (doc.listeners.get(type) || []).concat(listener)); },
    createElement: (tag) => new StubNode(doc, tag)
  };
  doc.body = new StubNode(doc, "body");
  const dialog = new StubDialog(doc);
  const add = (parent, tag, attributes) => {
    const node = new StubNode(doc, tag, attributes);
    parent.append(node);
    return node;
  };
  const parts = {
    families: add(dialog, "div", { "data-viewer": "families" }),
    closer: add(dialog, "button", { "data-viewer-close": "" }),
    previous: add(dialog, "button", { "data-viewer": "previous", "data-viewer-step": "-1" }),
    stage: add(dialog, "figure", { "data-viewer": "stage" }),
    next: add(dialog, "button", { "data-viewer": "next", "data-viewer-step": "1" }),
    title: add(dialog, "h2", { "data-viewer": "title" }),
    counter: add(dialog, "p", { "data-viewer": "counter" }),
    description: add(dialog, "p", { "data-viewer": "description" }),
    pdf: add(dialog, "a", { "data-viewer": "pdf" }),
    details: add(dialog, "a", { "data-viewer": "details" }),
    code: add(dialog, "a", { "data-viewer": "code" })
  };
  parts.pages = add(dialog, "div", { "data-viewer": "pages" });
  parts.pagePrevious = add(parts.pages, "button", { "data-viewer": "page-previous", "data-viewer-page": "-1" });
  parts.pageLabel = add(parts.pages, "span", { "data-viewer": "page-label" });
  parts.pageNext = add(parts.pages, "button", { "data-viewer": "page-next", "data-viewer-page": "1" });
  parts.thumbnails = add(dialog, "nav", { "data-viewer": "thumbnails" });
  parts.panelToggle = add(dialog, "button", { "data-viewer": "panel-toggle", "data-viewer-toggle": "" });
  parts.panel = add(dialog, "section", { "data-viewer": "panel" });
  parts.image = add(parts.stage, "img", { "data-viewer": "image" });
  parts.notice = add(parts.stage, "div", { "data-viewer": "notice" });
  parts.image.hidden = true;
  parts.notice.hidden = true;
  // The browser's part: a stage wide enough for the share-of-the-page rule to bite.
  parts.stage.clientWidth = 800;

  const entries = [{ state: null, hash: "#showcase" }];
  let at = 0;
  const location = { get hash() { return entries[at].hash; } };
  const history = {
    get state() { return entries[at].state; },
    pushState(state, _title, url) { entries.length = at + 1; entries.push({ state, hash: url }); at = entries.length - 1; },
    replaceState(state, _title, url) { entries[at] = { state, hash: url }; }
  };
  const images = [];
  class StubImage {
    constructor() { this.onload = null; this.onerror = null; this.src = ""; images.push(this); }
  }

  // A clock the test owns: a timer the viewer sets is visible here and runs when the test
  // says the time has passed, so a window that never closes cannot pass for one that does.
  const timers = new Map();
  let nextTimer = 1;
  const clock = {
    pending: () => [...timers.values()].map((timer) => timer.after),
    pass: () => {
      for (const [id, timer] of [...timers]) {
        timers.delete(id);
        timer.run();
      }
    }
  };

  // The viewer reads these as globals of the realm the script was loaded in.
  const connection = { saveData: false };
  const reducedMotion = { matches: false };
  Object.assign(sandbox, {
    document: doc, history, location, Image: StubImage,
    navigator: { connection },
    matchMedia: () => reducedMotion,
    innerWidth: 1280,
    visualViewport: null,
    setTimeout: (run, after) => { const id = nextTimer++; timers.set(id, { run, after }); return id; },
    clearTimeout: (id) => { timers.delete(id); }
  });
  const closed = [];
  const viewer = gallery.createViewer({
    dialog, catalogue: catalogueUnderTest, release, onClose: (report) => closed.push(report)
  });
  return {
    doc, dialog, parts, entries, history, location, images, closed, viewer, connection, clock, reducedMotion,
    // What a browser would do once the strip has been laid out: a row wider than the strip.
    layoutStrip: () => {
      const buttons = parts.thumbnails.querySelectorAll("[data-viewer-thumb]");
      buttons.forEach((button, index) => {
        button.offsetLeft = index * 62;
        button.offsetWidth = 54;
      });
      parts.thumbnails.clientWidth = 150;
      parts.thumbnails.scrollWidth = buttons.length * 62;
    },
    click: (node) => dispatch(node, { type: "click" }),
    press: (key, extra = {}) => dispatch(doc.body, { type: "keydown", key, isComposing: false, ...extra }),
    pointer: (type, detail) => dispatch(parts.stage, { type, pointerType: "touch", pointerId: 1, ...detail }),
    swipe: ({ from, to, pointerType = "touch", pointerId = 1 }) => {
      dispatch(parts.stage, { type: "pointerdown", pointerType, pointerId, clientX: from[0], clientY: from[1] });
      dispatch(parts.stage, { type: "pointerup", pointerType, pointerId, clientX: to[0], clientY: to[1] });
    },
    familyButtons: () => parts.families.querySelectorAll("[data-viewer-family]"),
    thumbButtons: () => parts.thumbnails.querySelectorAll("[data-viewer-thumb]"),
    // The panel is built out of plain nodes, and this DOM matches attribute selectors only,
    // so what it says is read as the text it holds.
    panelText: () => parts.panel.descendants().map((node) => node.text).filter(Boolean).join("\n"),
    panelLinks: () => parts.panel.descendants().filter((node) => node.tag === "a"),
    // The image the stage is waiting on, which is the one given a load handler; the
    // others were made by the preload and nobody is listening to them.
    pageLoader: () => [...images].reverse().find((image) => typeof image.onload === "function")
  };
}

const exampleOf = (id) => catalogue.get(id).example;

check("the panel stays out of the document's way until a reader asks for it", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "templates", group: "cv", id: "cv-blue-banner-v2" }, { history: "push" });
  // Collapsed to begin with. Open by default it took 42% of the viewport against the stage's
  // 21%, and an 892x1262 page rendered at 104x147 — not a document anyone can judge.
  assert.equal(page.parts.panel.hidden, true);
  assert.equal(page.parts.panelToggle.hidden, false);
  assert.equal(page.parts.panelToggle.getAttribute("aria-expanded"), "false");
  assert.match(page.parts.panelToggle.text, /Use this template/);

  page.click(page.parts.panelToggle);
  assert.equal(page.parts.panel.hidden, false);
  assert.equal(page.parts.panelToggle.getAttribute("aria-expanded"), "true");

  // And it stays open: paging through a family must not re-collapse it on every document.
  page.click(page.parts.next);
  assert.equal(page.parts.panel.hidden, false);
  assert.equal(page.parts.panelToggle.getAttribute("aria-expanded"), "true");
});

check("a document of several pages says which one is on screen", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "flagships", group: "default", id: "feature-catalog" }, { history: "push" });
  const total = exampleOf("feature-catalog").pages.length + 1;
  assert.ok(total > 2, "feature-catalog is the long document this case is about");
  assert.equal(page.parts.pages.hidden, false);
  assert.equal(page.parts.pageLabel.text, "Page 1 of " + total);
  assert.equal(page.parts.pagePrevious.disabled, true, "there is nothing before the first page");
  assert.equal(page.parts.pageNext.disabled, false);
});

check("paging forward shows the next page of the same document", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "flagships", group: "default", id: "feature-catalog" }, { history: "push" });
  const example = exampleOf("feature-catalog");
  page.click(page.parts.pageNext);
  assert.equal(page.parts.pageLabel.text, "Page 2 of " + (example.pages.length + 1));
  assert.equal(page.parts.pagePrevious.disabled, false);
  // Page 1 is the preview; page 2 is the first of the published page images.
  assert.equal(page.pageLoader().src, example.pages[0]);
  // The document did not change: paging is movement inside one document.
  assert.equal(page.location.hash, "#/flagships/default/feature-catalog");
});

check("a document of one page is offered no page controls at all", () => {
  const page = viewerHarness();
  let single = null;
  for (const category of catalogue.categories) {
    for (const group of category.groups) {
      for (const id of group.ids) {
        if (!exampleOf(id).pages) { single = { category: category.id, group: group.id, id }; break; }
      }
      if (single) break;
    }
    if (single) break;
  }
  assert.ok(single, "the catalogue holds a single-page document for this case");
  page.viewer.open(single, { history: "push" });
  assert.equal(page.parts.pages.hidden, true);
});

check("moving to another document starts it at its own first page", () => {
  const page = viewerHarness();
  const ids = idsOf("flagships", "default");
  const at = ids.indexOf("feature-catalog");
  page.viewer.open({ category: "flagships", group: "default", id: "feature-catalog" }, { history: "push" });
  page.click(page.parts.pageNext);
  assert.equal(page.parts.pageLabel.text.startsWith("Page 2"), true);

  page.click(page.parts.next);
  // The page index belongs to the document, not to the viewer: without the reset in show()
  // the next document would open on page 2, or on a page it does not have.
  assert.equal(page.pageLoader().src, exampleOf(ids[at + 1]).screenshot);
});

check("moving to a document of one page takes focus out of the page row before hiding it", () => {
  const page = viewerHarness();
  const ids = idsOf("flagships", "default");
  const at = ids.indexOf("feature-catalog");
  page.viewer.open({ category: "flagships", group: "default", id: "feature-catalog" }, { history: "push" });
  assert.equal(page.parts.pages.hidden, false);

  page.parts.pageNext.focus();
  assert.equal(page.doc.activeElement, page.parts.pageNext);

  const neighbour = ids.slice(at + 1).find((id) => !exampleOf(id).pages);
  assert.ok(neighbour, "fixture: a single-page document follows feature-catalog in this family");
  page.viewer.open({ category: "flagships", group: "default", id: neighbour }, { history: "replace" });

  assert.equal(page.parts.pages.hidden, true);
  assert.equal(page.parts.pages.contains(page.doc.activeElement), false,
    "a focused button inside a hidden row holds focus in name only: the browser drops it to the "
    + "page behind the modal, and the reader loses the ring and their place in the tab order");
});

check("a swipe moves between documents, not between pages", () => {
  const page = viewerHarness();
  const ids = idsOf("flagships", "default");
  const at = ids.indexOf("feature-catalog");
  page.viewer.open({ category: "flagships", group: "default", id: "feature-catalog" }, { history: "push" });
  page.swipe({ from: [600, 300], to: [200, 300] });
  assert.equal(page.location.hash, "#/flagships/default/" + ids[at + 1],
    "the gesture that moves documents must keep moving documents once a document has pages");
});

check("a preset card's panel names the preset, its model and the coordinates of the release shown", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "templates", group: "cv", id: "cv-blue-banner-v2" }, { history: "push" });
  page.click(page.parts.panelToggle);
  const said = page.panelText();
  assert.equal(page.parts.panel.hidden, false);
  assert.match(page.parts.panelToggle.text, /Use this template/);
  assert.match(said, /com\.demcha\.compose\.document\.templates\.cv\.presets\.BlueBanner/);
  assert.match(said, /com\.demcha\.compose\.document\.templates\.cv\.data\.CvDocument/);
  assert.match(said, /io\.github\.demchaav/);
  // This document is drawn in a bundled face, so the pair it is registered with would
  // compile for a reader and then throw at the first glyph: it is given the aggregate.
  assert.equal(exampleOf("cv-blue-banner-v2").needsBundledFonts, true);
  assert.match(said, /graph-compose-bundle/);
  assert.match(said, /Why the aggregate/);
  assert.ok(said.includes(RELEASE.stableVersion), "the coordinates name the release the page was published for");
  const targets = page.panelLinks().map((link) => link.href);
  assert.ok(targets.some((href) => href.includes("/blob/" + RELEASE.releaseTag + "/examples/src/main/java/")),
    "the runnable source is linked at that release, not at whatever develop holds today");
  assert.ok(targets.some((href) => href.endsWith("/docs/templates/v2-layered/quickstart.md")),
    "the CV family's guide is the one the docs index names as its starting point");
});

check("a document that embeds no face of its own is given the pair, not the aggregate", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "templates", group: "invoice", id: "invoice-modern-v2" }, { history: "push" });
  page.click(page.parts.panelToggle);
  const said = page.panelText();
  // The invoice theme is Helvetica, which every PDF reader already has. If this card ever
  // embeds a face, it stops being the example of a card that needs nothing extra.
  assert.equal(exampleOf("invoice-modern-v2").needsBundledFonts, false);
  assert.match(said, /graph-compose-templates/);
  assert.doesNotMatch(said, /graph-compose-bundle/);
  assert.doesNotMatch(said, /Why the aggregate/);
  const targets = page.panelLinks().map((link) => link.href);
  assert.ok(targets.some((href) => href.endsWith("/docs/templates/business-templates.md")),
    "the invoice family's guide is the business templates page");
});

check("an example GenerateAllExamples drives is not offered as one to run", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "features", group: "tables", id: "table-advanced" }, { history: "push" });
  page.click(page.parts.panelToggle);
  const said = page.panelText();
  assert.equal(exampleOf("table-advanced").runnable, false);
  assert.doesNotMatch(said, /exec:java/,
    "its class has no main, and exec:java would answer with 'doesn't contain a main method'");
  assert.match(said, /Rendered by/);
});

check("a card that renders a deck asks a reader for the deck backend", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "flagships", group: "default", id: "twin-output" }, { history: "push" });
  page.click(page.parts.panelToggle);
  // The PPTX backend is discovered by format, so nothing in the source names it and only the
  // render says it is missing — which is why the card has to carry it.
  assert.match(page.panelText(), /graph-compose-render-pptx/);
});

check("the aggregate stands in for the engine pair without swallowing a backend", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "features", group: "text", id: "letter-spacing" }, { history: "push" });
  page.click(page.parts.panelToggle);
  const said = page.panelText();
  assert.equal(exampleOf("letter-spacing").needsBundledFonts, true);
  assert.match(said, /graph-compose-bundle/);
  assert.match(said, /graph-compose-render-docx/, "the aggregate carries no DOCX backend");
  assert.match(said, /graph-compose-render-pptx/, "nor a PPTX one");
  assert.doesNotMatch(said, /<artifactId>graph-compose<\/artifactId>/,
    "the engine and templates are what the aggregate replaces");
});

check("a card that builds no preset is offered as a runnable example, and claims no preset", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "templates", group: "coverletter", id: "cover-letter" }, { history: "push" });
  page.click(page.parts.panelToggle);
  const said = page.panelText();
  assert.match(page.parts.panelToggle.text, /Run this example/);
  assert.doesNotMatch(page.parts.panelToggle.text, /Use this template/);
  assert.doesNotMatch(said, /Preset/);
  assert.match(said, /exec:java -Dexec\.mainClass=com\.demcha\.examples\./);
});

check("with no release context the panel names no coordinates rather than guessing a version", () => {
  const page = viewerHarness(catalogue, null);
  page.viewer.open({ category: "templates", group: "cv", id: "cv-blue-banner-v2" }, { history: "push" });
  page.click(page.parts.panelToggle);
  const said = page.panelText();
  assert.doesNotMatch(said, /undefined/);
  assert.doesNotMatch(said, /<version>/);
  assert.ok(page.panelLinks().every((link) => !link.href.includes("/blob/v")),
    "with no release to pin to, a link falls back to the address the catalogue already carries");
});

check("the panel shows the family's compiled block, as the manifest carries it", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "templates", group: "cv", id: "cv-blue-banner-v2" }, { history: "push" });
  page.click(page.parts.panelToggle);
  const code = manifest.snippets.cv.code;
  assert.ok(code && code.length > 0, "the manifest carries a CV snippet for the panel to show");
  assert.ok(page.panelText().includes(code), "the block is shown as it stands, not paraphrased");
});

check("a family with no compiled block shows no snippet rather than another family's", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "templates", group: "coverletter", id: "cover-letter" }, { history: "push" });
  page.click(page.parts.panelToggle);
  assert.equal(manifest.snippets.coverletter, undefined);
  assert.ok(!page.panelText().includes(manifest.snippets.cv.code));
});

check("a view showing no document empties the panel and hides it", () => {
  const page = viewerHarness();
  page.viewer.open({ category: "templates", group: "cv", id: "no-such-document" }, { history: "push" });
  assert.equal(page.parts.panel.hidden, true);
  assert.equal(page.panelText(), "");
});

check("a document's page is the three segments of its viewer address, as a path", () => {
  assert.equal(gallery.pagePath({ category: "templates", group: "cv", id: "cv-blue-banner-v2" }),
    "templates/cv/cv-blue-banner-v2/");
  // The site build writes each page at this path and the viewer links it from here, so one
  // formatter is what keeps a link and the page it names in the same place.
  for (const category of catalogue.categories) {
    for (const group of category.groups) {
      for (const id of group.ids) {
        const route = { category: category.id, group: group.id, id };
        assert.equal(gallery.pagePath(route), gallery.formatRoute(route).slice("#/".length) + "/", id);
      }
    }
  }
});

check("the viewer links the page of the document shown, and no page while it shows none", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  page.viewer.open({ category: "templates", group: "cv", id: ids[0] }, { history: "push" });
  assert.equal(page.parts.details.hidden, false);
  assert.equal(page.parts.details.href, "templates/cv/" + ids[0] + "/");
  page.click(page.parts.next);
  assert.equal(page.parts.details.href, "templates/cv/" + ids[1] + "/", "the link follows the document shown");
  page.viewer.open({ category: "templates", group: "cv", id: "no-such-document" }, { history: "replace" });
  assert.equal(page.parts.details.hidden, true, "a view of no document has no page to link");
});

check("the panel the viewer draws is the panel model, item for item", () => {
  // The document pages are rendered from panelModel too, so what the dialog draws has to be the
  // model and nothing besides — a renderer that dropped or reordered a kind of item would make the
  // two disagree while every other case here stayed green.
  for (const route of [
    { category: "templates", group: "cv", id: "cv-blue-banner-v2" },
    { category: "templates", group: "invoice", id: "invoice-modern-v2" },
    { category: "features", group: "tables", id: "table-advanced" },
    { category: "flagships", group: "default", id: "twin-output" }
  ]) {
    const page = viewerHarness();
    page.viewer.open(route, { history: "push" });
    page.click(page.parts.panelToggle);
    const model = plain(gallery.panelModel(exampleOf(route.id), catalogue, RELEASE));
    assert.equal(page.parts.panelToggle.text, model.action, route.id);
    const drawn = page.parts.panel.descendants().map((node) => node.text).filter(Boolean);
    const modelled = model.items.flatMap((item) => [item.label, "code" in item ? item.code : item.value])
      .concat(model.links.map((link) => link.text));
    assert.deepEqual(drawn, modelled, route.id);
    assert.deepEqual(page.panelLinks().map((link) => link.href), model.links.map((link) => link.href), route.id);
    // A class name or a path is set as code, a sentence as text, the way the model marks them.
    assert.deepEqual(
      page.parts.panel.descendants().filter((node) => node.className === "gallery-viewer-panel-value").map((node) => node.tag),
      model.items.filter((item) => !("code" in item)).map((item) => (item.literal ? "code" : "span")),
      route.id);
  }
});

check("the panel model runs with no DOM at all", () => {
  // The site build loads this script into a bare context to render the document pages.
  const bare = {};
  vm.runInNewContext(web("gallery-viewer.js"), bare, { filename: "web/gallery-viewer.js" });
  const model = plain(bare.GraphComposeGallery.panelModel(exampleOf("cv-blue-banner-v2"), catalogue, RELEASE));
  assert.equal(model.action, "Use this template");
  assert.ok(model.items.length > 0 && model.links.length > 0);
});

check("the family's block is called the document's own only on the card of the preset it composes", () => {
  const labelOf = (id) => {
    const home = catalogue.get(id);
    const block = catalogue.snippets[home.groupId].code;
    const listing = gallery.panelModel(home.example, catalogue, RELEASE).items.find((item) => item.code === block);
    assert.ok(listing, "fixture: the panel of " + id + " shows its family's block");
    return listing.label;
  };
  assert.match(manifest.snippets.cv.code, /\bBoxedSections\.create\(/, "fixture: the CV block composes BoxedSections");
  assert.equal(labelOf("cv-boxed-sections-v2"), "Compose it");
  assert.equal(labelOf("cv-blue-banner-v2"), "From the docs",
    "the CV block builds BoxedSections; labelled 'Compose it' on Blue Banner it promises a document it does not build");
  assert.equal(labelOf("invoice-modern-v2"), "Compose it");
  assert.equal(labelOf("invoice-classic-v2"), "From the docs");
  assert.equal(labelOf("project-proposal-cinematic"), "From the docs", "a card that builds no preset");

  // Matched as a whole name: a preset called Sections does not compose BoxedSections.
  const card = { id: "sections", kind: "PRESET", presetClass: "com.example.presets.Sections" };
  const fixture = { snippets: { cv: { code: manifest.snippets.cv.code } }, get: () => ({ groupId: "cv", example: card }) };
  assert.equal(gallery.panelModel(card, fixture, RELEASE).items.find((item) => "code" in item).label, "From the docs");
});

check("a listing that is not on the family guide's page links the page it is on", () => {
  // The CV family starts at the quickstart, but its listing is published on using-templates.md:
  // a reader told the code comes from the docs has to be able to reach the page that holds it.
  const linksOf = (id) => plain(gallery.panelModel(exampleOf(id), catalogue, RELEASE).links);
  assert.notEqual(manifest.snippets.cv.source, "docs/templates/v2-layered/quickstart.md",
    "fixture: the CV listing is published somewhere other than the family guide");
  assert.ok(linksOf("cv-blue-banner-v2").some((link) => link.text === "Snippet source"
    && link.href.endsWith("/blob/" + RELEASE.releaseTag + "/" + manifest.snippets.cv.source)));
  // The invoice listing lives on the family guide itself, which is already linked.
  assert.equal(manifest.snippets.invoice.source, "docs/templates/business-templates.md");
  assert.ok(!linksOf("invoice-classic-v2").some((link) => link.text === "Snippet source"));
  assert.ok(!linksOf("table-advanced").some((link) => link.text === "Snippet source"), "a family with no listing links none");
});

check("a family named like a property every object has is given no guide of another's", () => {
  // Family ids are looked up in plain objects, where "constructor" names the object's own
  // prototype; read as a guide, it published a link to the text of a function.
  const card = {
    id: "only", title: "Only", kind: "FEATURE", runnable: true, requiredArtifacts: ["graph-compose"],
    sourcePath: "examples/src/main/java/com/demcha/examples/Only.java", pdf: "showcase/pdf/features/constructor/only.pdf"
  };
  const fixture = catalogueOf({ categories: [{ id: "features", label: "F", groups: [
    { id: "constructor", label: "C", examples: [card] }] }] });
  const model = plain(gallery.panelModel(card, fixture, RELEASE));
  assert.deepEqual(model.links.map((link) => link.text), ["Example source"]);
});

check("opening records one history entry, and moving inside the viewer records none", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  page.viewer.open({ category: "templates", group: "cv", id: ids[0] }, { history: "push" });
  assert.equal(page.entries.length, 2);
  assert.equal(page.location.hash, "#/templates/cv/" + ids[0]);
  assert.equal(page.history.state.galleryViewer, true);
  page.click(page.parts.next);
  page.click(page.parts.next);
  page.click(page.familyButtons().find((button) => button.dataset.viewerFamily !== "cv"));
  assert.equal(page.entries.length, 2, "Next and a family switch replace the entry, they do not add one");
  assert.equal(page.history.state.galleryViewer, true, "the entry stays the viewer's own");
});

check("closing hands back the document shown and the opener, and unlocks the page", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  const opener = page.doc.createElement("button");
  page.viewer.open({ category: "templates", group: "cv", id: ids[1] }, { history: "push", opener });
  assert.equal(page.doc.body.classList.contains("viewer-open"), true);
  page.click(page.parts.closer);
  assert.equal(page.viewer.isOpen(), false);
  assert.equal(page.doc.body.classList.contains("viewer-open"), false);
  assert.equal(page.closed.length, 1);
  assert.equal(page.closed[0].view.id, ids[1]);
  assert.equal(page.closed[0].opener, opener);
  assert.equal(page.closed[0].byHistory, false);
  page.viewer.open({ category: "templates", group: "cv", id: ids[0] }, { history: "push" });
  page.viewer.close({ byHistory: true });
  assert.equal(page.closed[1].byHistory, true, "a close the address drove says so, so the page leaves history alone");
});

check("a preview that arrives late never lands under a newer document", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  page.viewer.open({ category: "templates", group: "cv", id: ids[0] }, { history: "push" });
  const slow = page.pageLoader();
  page.click(page.parts.next);
  const quick = page.pageLoader();
  assert.notEqual(slow, quick, "fixture: each document asks for its own page image");
  quick.onload();
  slow.onload();
  assert.equal(page.parts.image.src, quick.src);
  assert.equal(page.parts.title.textContent, exampleOf(ids[1]).title);
});

check("at the end of a family the keys still move, and focus stays inside the dialog", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  page.viewer.open({ category: "templates", group: "cv", id: ids[ids.length - 2] }, { history: "push" });
  page.parts.next.focus();
  page.click(page.parts.next);
  assert.equal(page.parts.counter.textContent, ids.length + " / " + ids.length);
  assert.equal(page.parts.next.disabled, true);
  assert.notEqual(page.doc.activeElement, page.parts.next, "a disabled button cannot keep focus");
  assert.equal(page.dialog.contains(page.doc.activeElement), true, "focus must not fall back to the page");
  // Pressed from outside the dialog: the keys are the document's while the viewer is open.
  page.press("ArrowLeft");
  assert.equal(page.parts.counter.textContent, ids.length - 1 + " / " + ids.length);
  page.press("ArrowLeft", { altKey: true });
  assert.equal(page.parts.counter.textContent, ids.length - 1 + " / " + ids.length, "Alt+Left is the browser's Back");
});

check("a one-document family disables both steps and leaves focus on Close", () => {
  const page = viewerHarness(catalogueOf({ categories: [{ id: "c", label: "C", groups: [
    { id: "solo", label: "Solo", examples: [{ id: "only", title: "Only", screenshot: "only.png" }] }] }] }));
  page.parts.next.focus();
  page.viewer.open({ category: "c", group: "solo", id: null }, { history: "push" });
  assert.equal(page.parts.previous.disabled, true);
  assert.equal(page.parts.next.disabled, true);
  assert.equal(page.doc.activeElement, page.parts.closer);
});

check("every document of the family gets a thumbnail, and the one shown is marked", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  page.viewer.open({ category: "templates", group: "cv", id: ids[2] }, { history: "push" });
  const thumbs = page.thumbButtons();
  assert.deepEqual(thumbs.map((button) => button.dataset.viewerThumb), ids);
  assert.equal(thumbs[2].getAttribute("aria-current"), "true");
  assert.equal(thumbs.filter((button) => button.getAttribute("aria-current") === "true").length, 1);
  assert.equal(thumbs[2].getAttribute("aria-label"), exampleOf(ids[2]).title,
    "a thumbnail is an image of a page: its name has to be said, not left to a tooltip");
  assert.ok(exampleOf(ids[0]).thumbnail.includes("/thumbnails/"),
    "fixture: the catalogue carries no strip-sized files for the strip to read");
  assert.deepEqual(thumbs.map((button) => button.children[0].src), ids.map((id) => exampleOf(id).thumbnail),
    "the strip reads the catalogue's strip-sized file for each document, not the page preview");
  page.click(page.familyButtons().find((button) => button.dataset.viewerFamily === "invoice"));
  assert.deepEqual(page.thumbButtons().map((button) => button.dataset.viewerThumb), idsOf("templates", "invoice"),
    "the strip belongs to the family shown, so it is rebuilt when the family changes");
});

check("a thumbnail shows its document without recording a history entry", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  page.viewer.open({ category: "templates", group: "cv", id: ids[0] }, { history: "push" });
  const entries = page.entries.length;
  page.click(page.thumbButtons()[4]);
  assert.equal(page.parts.counter.textContent, "5 / " + ids.length);
  assert.equal(page.location.hash, "#/templates/cv/" + ids[4]);
  assert.equal(page.entries.length, entries, "a thumbnail replaces the entry, as Previous and Next do");
});

check("the strip brings the document shown into view, and stops animating when asked", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  page.viewer.open({ category: "templates", group: "cv", id: ids[0] }, { history: "push" });
  page.layoutStrip();
  page.click(page.parts.next);
  const scrolled = page.parts.thumbnails.scrolledTo;
  assert.ok(scrolled, "the thumbnail of the document shown was never brought into view");
  assert.equal(scrolled.left, 14, "centred: 62px along a 150px strip, 54px wide");
  assert.equal(scrolled.behavior, "smooth");
  page.reducedMotion.matches = true;
  page.click(page.parts.next);
  assert.equal(page.parts.thumbnails.scrolledTo.left, 76);
  assert.equal(page.parts.thumbnails.scrolledTo.behavior, "auto",
    "the CSS rule for reduced motion cannot reach a scroll made from a script");
});

check("a reader saving data gets no strip at all", () => {
  const page = viewerHarness();
  page.connection.saveData = true;
  const ids = idsOf("templates", "cv");
  page.viewer.open({ category: "templates", group: "cv", id: ids[1] }, { history: "push" });
  assert.equal(page.parts.thumbnails.hidden, true);
  assert.equal(page.thumbButtons().length, 0,
    "strip-sized or not, a row of one image per document is not what that mode is for");
  assert.equal(page.images.length, 1, "only the page being read is fetched");
});

check("the strip is one tab stop, on the document shown", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  page.viewer.open({ category: "templates", group: "cv", id: ids[3] }, { history: "push" });
  const tabbable = page.thumbButtons().filter((button) => button.tabIndex === 0);
  assert.equal(tabbable.length, 1, "a reader must not tab through the whole family to reach the links");
  assert.equal(tabbable[0].dataset.viewerThumb, ids[3]);
  page.click(page.parts.next);
  assert.deepEqual(page.thumbButtons().filter((button) => button.tabIndex === 0)
    .map((button) => button.dataset.viewerThumb), [ids[4]], "the stop follows the document shown");
});

check("clicking the thumbnail of the document already shown asks for nothing", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  page.viewer.open({ category: "templates", group: "cv", id: ids[2] }, { history: "push" });
  const fetched = page.images.length;
  page.click(page.thumbButtons()[2]);
  assert.equal(page.images.length, fetched, "the page on screen was fetched and decoded a second time");
  assert.equal(page.parts.counter.textContent, "3 / " + ids.length);
});

check("the pages either side are fetched ahead, and none of them in data-saver mode", () => {
  const ids = idsOf("templates", "cv");
  for (const id of [ids[0], ids[1], ids[2], ids[3]]) {
    assert.ok(exampleOf(id).screenshot, "fixture: every CV in this check needs a page image");
  }
  const page = viewerHarness();
  page.viewer.open({ category: "templates", group: "cv", id: ids[1] }, { history: "push" });
  const asked = page.images.map((image) => image.src);
  assert.ok(asked.includes(exampleOf(ids[0]).screenshot), "the page before is not fetched ahead");
  assert.ok(asked.includes(exampleOf(ids[2]).screenshot), "the page after is not fetched ahead");
  assert.ok(!asked.includes(exampleOf(ids[3]).screenshot), "only the neighbours are fetched ahead");

  const saving = viewerHarness();
  saving.connection.saveData = true;
  saving.viewer.open({ category: "templates", group: "cv", id: ids[1] }, { history: "push" });
  const askedWhileSaving = saving.images.map((image) => image.src);
  assert.ok(!askedWhileSaving.includes(exampleOf(ids[0]).screenshot));
  assert.ok(!askedWhileSaving.includes(exampleOf(ids[2]).screenshot));
  assert.ok(askedWhileSaving.includes(exampleOf(ids[1]).screenshot), "the page shown is still loaded");
});

check("the page after the one on screen is fetched ahead, and not in data-saver mode", () => {
  const page = viewerHarness();
  const example = exampleOf("feature-catalog");
  assert.ok(example.pages && example.pages.length > 1, "fixture: this document has pages to warm");

  page.viewer.open({ category: "flagships", group: "default", id: "feature-catalog" }, { history: "push" });
  assert.ok(page.images.map((image) => image.src).includes(example.pages[0]),
    "page 2 is the one fetch paging cannot avoid");

  page.click(page.parts.pageNext);
  assert.ok(page.images.map((image) => image.src).includes(example.pages[1]),
    "paging forward has to keep warming, or it stays ahead of the reader for exactly one step");

  const saving = viewerHarness();
  saving.connection.saveData = true;
  saving.viewer.open({ category: "flagships", group: "default", id: "feature-catalog" }, { history: "push" });
  assert.ok(!saving.images.map((image) => image.src).includes(example.pages[0]),
    "a reader who asked to save data is sent no page they have not asked for");
});

check("a drag across the page moves a document; a short, vertical or edge drag does not", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  const at = () => page.parts.counter.textContent;
  page.viewer.open({ category: "templates", group: "cv", id: ids[1] }, { history: "push" });
  assert.equal(at(), "2 / " + ids.length);
  page.swipe({ from: [600, 300], to: [440, 320] });
  assert.equal(at(), "3 / " + ids.length, "a drag to the left moves forward");
  page.swipe({ from: [440, 300], to: [600, 320] });
  assert.equal(at(), "2 / " + ids.length, "a drag to the right moves back");
  page.swipe({ from: [600, 300], to: [570, 300] });
  assert.equal(at(), "2 / " + ids.length, "a 30px drag is not a swipe");
  page.swipe({ from: [600, 300], to: [440, 480] });
  assert.equal(at(), "2 / " + ids.length, "a drag more vertical than horizontal is a scroll");
  page.swipe({ from: [10, 300], to: [200, 300] });
  assert.equal(at(), "2 / " + ids.length, "a drag from the screen edge belongs to the browser");
});

check("a drag has to cross a share of the page, not only clear the 48px floor", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  const at = () => page.parts.counter.textContent;
  page.viewer.open({ category: "templates", group: "cv", id: ids[1] }, { history: "push" });
  page.swipe({ from: [600, 300], to: [500, 300] });
  assert.equal(at(), "2 / " + ids.length, "100px is over the floor but under a 15% share of an 800px page");
  page.swipe({ from: [600, 300], to: [470, 300] });
  assert.equal(at(), "3 / " + ids.length, "130px clears the share");
});

check("a second finger, a pinch, a cancel or a mouse is never a swipe", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  const at = () => page.parts.counter.textContent;
  page.viewer.open({ category: "templates", group: "cv", id: ids[1] }, { history: "push" });

  page.pointer("pointerdown", { pointerId: 1, clientX: 600, clientY: 300 });
  page.pointer("pointerdown", { pointerId: 2, clientX: 620, clientY: 300 });
  page.pointer("pointerup", { pointerId: 1, clientX: 440, clientY: 300 });
  assert.equal(at(), "2 / " + ids.length, "a second finger is a pinch, not a swipe");

  page.pointer("pointerdown", { pointerId: 1, clientX: 600, clientY: 300 });
  page.pointer("pointercancel", { pointerId: 1 });
  page.pointer("pointerup", { pointerId: 1, clientX: 440, clientY: 300 });
  assert.equal(at(), "2 / " + ids.length, "a cancelled pointer leaves no gesture behind");

  sandbox.visualViewport = { scale: 2 };
  page.swipe({ from: [600, 300], to: [440, 300] });
  sandbox.visualViewport = null;
  assert.equal(at(), "2 / " + ids.length, "a reader who has zoomed in is panning the page");

  page.pointer("pointerdown", { pointerId: 1, clientX: 600, clientY: 300 });
  dispatch(page.parts.next, { type: "pointerdown", pointerType: "touch", pointerId: 2, clientX: 700, clientY: 300 });
  page.pointer("pointerup", { pointerId: 1, clientX: 440, clientY: 300 });
  assert.equal(at(), "2 / " + ids.length, "a second finger on an arrow laid over the page ends it too");

  page.swipe({ from: [600, 300], to: [440, 300], pointerType: "mouse" });
  assert.equal(at(), "2 / " + ids.length, "a mouse drag is not a swipe");
});

check("the click the browser sends after a swipe presses nothing, and the window shuts itself", () => {
  const page = viewerHarness();
  const ids = idsOf("templates", "cv");
  const at = () => page.parts.counter.textContent;
  page.viewer.open({ category: "templates", group: "cv", id: ids[1] }, { history: "push" });
  page.swipe({ from: [600, 300], to: [440, 300] });
  assert.equal(at(), "3 / " + ids.length);
  assert.deepEqual(page.clock.pending(), [350],
    "the window has to shut on a timer: on some engines the click never comes to close it");
  page.click(page.parts.next);
  assert.equal(at(), "3 / " + ids.length, "the click the drag leaves behind is swallowed");
  page.click(page.parts.next);
  assert.equal(at(), "4 / " + ids.length, "the next real press still works");

  // A drag whose click never arrives must not leave the window open over the next tap.
  page.swipe({ from: [600, 300], to: [440, 300] });
  assert.equal(at(), "5 / " + ids.length);
  page.clock.pass();
  page.click(page.parts.next);
  assert.equal(at(), "6 / " + ids.length, "a tap after the window is the reader's, not the drag's");
});

if (failures > 0) {
  console.log(`gallery-viewer: ${failures} case(s) failed`);
  process.exit(1);
}
console.log("gallery-viewer: all cases passed");
