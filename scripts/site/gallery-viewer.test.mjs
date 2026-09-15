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
  return { categories, get: (id) => index.get(id) };
}

const manifest = JSON.parse(web("examples.json"));
const catalogue = catalogueOf(manifest);
const idsOf = (categoryId, groupId) =>
  catalogue.categories.find((c) => c.id === categoryId).groups.find((g) => g.id === groupId).ids;

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
  for (const part of ["families", "stage", "image", "notice", "title", "counter", "description",
    "pdf", "code", "previous", "next"]) {
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
function viewerHarness(catalogueUnderTest = catalogue) {
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
    code: add(dialog, "a", { "data-viewer": "code" })
  };
  parts.image = add(parts.stage, "img", { "data-viewer": "image" });
  parts.notice = add(parts.stage, "div", { "data-viewer": "notice" });
  parts.image.hidden = true;
  parts.notice.hidden = true;

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

  // The viewer reads these as globals of the realm the script was loaded in.
  Object.assign(sandbox, { document: doc, history, location, Image: StubImage });
  const closed = [];
  const viewer = gallery.createViewer({ dialog, catalogue: catalogueUnderTest, onClose: (report) => closed.push(report) });
  return {
    doc, dialog, parts, entries, history, location, images, closed, viewer,
    click: (node) => dispatch(node, { type: "click" }),
    press: (key, extra = {}) => dispatch(doc.body, { type: "keydown", key, isComposing: false, ...extra }),
    familyButtons: () => parts.families.querySelectorAll("[data-viewer-family]")
  };
}

const exampleOf = (id) => catalogue.get(id).example;

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
  const slow = page.images[page.images.length - 1];
  page.click(page.parts.next);
  const quick = page.images[page.images.length - 1];
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

if (failures > 0) {
  console.log(`gallery-viewer: ${failures} case(s) failed`);
  process.exit(1);
}
console.log("gallery-viewer: all cases passed");
