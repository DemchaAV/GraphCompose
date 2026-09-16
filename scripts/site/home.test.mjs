/**
 * node scripts/site/home.test.mjs — exit 0 when every case holds.
 *
 * Runs web/home.js the way the page does, as a plain script, against the hero exactly as the build
 * wrote it: the hero's markup is parsed out of web/index.html into a DOM small enough to live in
 * this file, so a hook renamed, or an element moved out of the figure it has to sit in, fails here
 * instead of leaving a switch that silently never appears. Checked: the switch shows only when it
 * can do something; the link into the viewer waits for the viewer; choosing a document changes its
 * caption and both links together with its picture, once the picture is ready; a preview that
 * arrives after a newer choice is dropped; and a click on no option changes nothing.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const read = (name) => fs.readFileSync(path.join(root, "web", name), "utf8");
const script = read("home.js");
const page = read("index.html");

const failures = [];
async function test(name, fn) {
  try {
    await fn();
    console.log(`  ok    ${name}`);
  } catch (error) {
    failures.push(name);
    console.log(`  FAIL  ${name}`);
    console.log(`        ${error.message.split("\n").join("\n        ")}`);
  }
}

const ENTITIES = { "&amp;": "&", "&lt;": "<", "&gt;": ">", "&quot;": '"', "&#39;": "'" };
const decodeEntities = (text) => text.replace(/&(?:amp|lt|gt|quot|#39);/g, (entity) => ENTITIES[entity]);

/** An element with only what home.js touches. Selectors are [attribute] hooks and nothing else. */
class Element {
  constructor(tag, attributes = {}) {
    this.tagName = tag.toUpperCase();
    this.attributes = { ...attributes };
    this.children = [];
    this.parent = null;
    this.hidden = "hidden" in attributes;
    this.textContent = "";
    this.listeners = {};
    this.dataset = {};
    for (const [name, value] of Object.entries(attributes)) {
      if (name.startsWith("data-")) {
        this.dataset[name.slice(5).replace(/-(\w)/g, (_, letter) => letter.toUpperCase())] = value;
      }
    }
    for (const reflected of ["src", "href", "alt"]) {
      if (reflected in attributes) this[reflected] = attributes[reflected];
    }
  }
  append(child) {
    child.parent = this;
    this.children.push(child);
  }
  setAttribute(name, value) {
    this.attributes[name] = String(value);
  }
  getAttribute(name) {
    return name in this.attributes ? this.attributes[name] : null;
  }
  removeAttribute(name) {
    delete this.attributes[name];
  }
  *descendants() {
    for (const child of this.children) {
      yield child;
      yield* child.descendants();
    }
  }
  matches(selector) {
    const hook = /^\[([\w-]+)\]$/.exec(selector);
    if (!hook) throw new Error(`the stub DOM reads only [attribute] selectors, and home.js asked for ${selector}`);
    return hook[1] in this.attributes;
  }
  querySelectorAll(selector) {
    return [...this.descendants()].filter((element) => element.matches(selector));
  }
  querySelector(selector) {
    return this.querySelectorAll(selector)[0] || null;
  }
  closest(selector) {
    for (let element = this; element; element = element.parent) {
      if (element.tagName !== "BODY" && element.matches(selector)) return element;
    }
    return null;
  }
  contains(other) {
    for (let element = other; element; element = element.parent) {
      if (element === this) return true;
    }
    return false;
  }
  addEventListener(type, listener) {
    (this.listeners[type] ||= []).push(listener);
  }
}

/** The hero's markup as built: from `<div class="hero-visual">` to the tag that closes it. */
function heroMarkup(html) {
  const start = html.indexOf('<div class="hero-visual">');
  assert.ok(start >= 0, "web/index.html has no hero-visual block");
  const tags = /<(\/?)([a-zA-Z][\w-]*)\b[^>]*>/g;
  tags.lastIndex = start;
  let depth = 0;
  for (let match; (match = tags.exec(html)); ) {
    if (match[2].toLowerCase() !== "div") continue;
    depth += match[1] ? -1 : 1;
    if (depth === 0) return html.slice(start, tags.lastIndex);
  }
  throw new Error("the hero-visual block is never closed");
}

const VOID = new Set(["img", "br", "input", "hr", "source"]);

/** Parses the generated fragment into Elements, refusing markup that does not nest. */
function parse(fragment) {
  const body = new Element("body");
  const stack = [body];
  const tokens = /<(\/?)([a-zA-Z][\w-]*)((?:\s+[\w-]+(?:="[^"]*")?)*)\s*\/?>|([^<]+)/g;
  for (let match; (match = tokens.exec(fragment)); ) {
    const [, closing, tag, attributeText, text] = match;
    const top = stack[stack.length - 1];
    if (text !== undefined) {
      if (text.trim()) top.textContent += decodeEntities(text);
      continue;
    }
    if (closing) {
      const opened = stack.pop();
      assert.equal(opened.tagName, tag.toUpperCase(), `</${tag}> closes a <${opened.tagName.toLowerCase()}>`);
      continue;
    }
    const attributes = {};
    for (const [, name, value] of attributeText.matchAll(/([\w-]+)(?:="([^"]*)")?/g)) {
      attributes[name] = value === undefined ? "" : decodeEntities(value);
    }
    const element = new Element(tag, attributes);
    top.append(element);
    if (!VOID.has(tag.toLowerCase())) stack.push(element);
  }
  return body;
}

/** A fresh hero parsed from the built page, optionally keeping only the first `keep` options. */
function heroDom({ keep } = {}) {
  const body = parse(heroMarkup(page));
  const group = body.querySelector("[data-hero-switch]");
  if (keep !== undefined) group.children = group.children.slice(0, keep);
  return {
    body,
    group,
    figure: body.querySelector("[data-hero]"),
    image: body.querySelector("[data-hero-image]"),
    title: body.querySelector("[data-hero-title]"),
    pdf: body.querySelector("[data-hero-pdf]"),
    open: body.querySelector("[data-hero-open]"),
    options: () => group.querySelectorAll("[data-hero-option]"),
  };
}

/** Runs home.js over a hero, with previews whose readiness the test decides. */
function run(dom, { viewerReady = false } = {}) {
  const images = [];
  class Image {
    constructor() {
      images.push(this);
    }
    decode() {
      return new Promise((resolve, reject) => {
        this.resolve = resolve;
        this.reject = reject;
      });
    }
  }
  const listeners = {};
  const document = {
    documentElement: { dataset: viewerReady ? { galleryViewer: "ready" } : {} },
    querySelector: (selector) => dom.body.querySelector(selector),
    addEventListener: (type, listener) => (listeners[type] ||= []).push(listener),
  };
  vm.runInNewContext(script, { document, Image, Promise }, { filename: "web/home.js" });
  return {
    ...dom,
    images,
    announceViewer: () => (listeners["gallery-viewer-ready"] || []).forEach((listener) => listener({})),
    click: (target) => (dom.group.listeners.click || []).forEach((listener) => listener({ target })),
  };
}

const settle = () => new Promise((resolve) => setImmediate(resolve));
const pressedStates = (hero) => hero.options().map((option) => option.getAttribute("aria-pressed"));

await test("the built hero carries every hook home.js reads, and the page loads home.js", () => {
  for (const hook of ["data-hero", "data-hero-image", "data-hero-title", "data-hero-pdf", "data-hero-open",
    "data-hero-switch", "data-hero-option"]) {
    // As a whole attribute: "data-hero" is also how every other hook begins.
    assert.match(page, new RegExp(`\\s${hook}(?=[\\s>=])`), `web/index.html has no ${hook} attribute`);
  }
  assert.match(page, /<script src="home\.js"><\/script>/, "index.html does not load home.js");
  assert.ok(heroDom().options().length > 1, "the hero offers fewer than two documents, so the switch cases test nothing");
});

await test("the event home.js waits for is the one examples.js sends once the viewer exists", () => {
  // The cases below announce the viewer themselves, so without this a renamed or dropped event in
  // examples.js would leave every case green and the link hidden on the live page for good.
  const listened = /addEventListener\('([\w-]+)', revealViewerLink/.exec(script);
  assert.ok(listened, "home.js no longer waits for an event before showing the link into the viewer");
  const examples = read("examples.js");
  assert.ok(examples.includes(`new CustomEvent('${listened[1]}')`),
    `examples.js never sends '${listened[1]}', so the link into the viewer would never appear`);
  assert.match(examples, /dataset\.galleryViewer = 'ready'/,
    "examples.js no longer marks the viewer ready, so a home.js that ran after it would never show the link");
  assert.match(script, /dataset\.galleryViewer === 'ready'/);
});

await test("before the viewer exists the switch shows, and the link into the viewer stays hidden", () => {
  const hero = run(heroDom());
  assert.equal(hero.group.hidden, false,
    "the switch never appeared: home.js found no figure, image, title or link where the built page puts them");
  assert.equal(hero.open.hidden, true, "a link into a viewer that is not there changes the address and opens nothing");
});

await test("the link into the viewer appears when the viewer announces itself", () => {
  const hero = run(heroDom());
  hero.announceViewer();
  assert.equal(hero.open.hidden, false);
});

await test("a viewer that is already up when home.js runs gets its link at once", () => {
  const hero = run(heroDom(), { viewerReady: true });
  assert.equal(hero.open.hidden, false);
});

await test("choosing a document changes only which option is pressed until its preview is ready", async () => {
  const hero = run(heroDom(), { viewerReady: true });
  const before = { src: hero.image.src, title: hero.title.textContent, pdf: hero.pdf.href, open: hero.open.href };
  const chosen = hero.options()[2];
  hero.click(chosen);
  await settle();
  assert.deepEqual(pressedStates(hero), hero.options().map((option) => String(option === chosen)));
  assert.deepEqual({ src: hero.image.src, title: hero.title.textContent, pdf: hero.pdf.href, open: hero.open.href }, before,
    "the page described the new document before its picture was ready");
  assert.equal(hero.figure.getAttribute("aria-busy"), "true");
});

await test("once the preview is ready, the picture, its size, the caption and both links change together", async () => {
  const hero = run(heroDom(), { viewerReady: true });
  const chosen = hero.options()[2];
  hero.click(chosen);
  hero.images.at(-1).resolve();
  await settle();
  const data = chosen.dataset;
  assert.equal(hero.image.src, data.screenshot);
  assert.equal(hero.image.getAttribute("width"), data.width);
  assert.equal(hero.image.getAttribute("height"), data.height);
  assert.equal(hero.image.alt, `${data.title}, first page`);
  assert.equal(hero.title.textContent, data.title);
  assert.equal(hero.pdf.href, data.pdf);
  assert.equal(hero.open.href, data.route);
  assert.equal(hero.figure.getAttribute("aria-busy"), null);
});

await test("a preview that becomes ready after a newer choice is dropped", async () => {
  const hero = run(heroDom(), { viewerReady: true });
  const [, second, third] = hero.options();
  hero.click(second);
  hero.click(third);
  const [secondPreview, thirdPreview] = hero.images.slice(-2);
  thirdPreview.resolve();
  await settle();
  secondPreview.resolve();
  await settle();
  assert.equal(hero.title.textContent, third.dataset.title, "a slow preview of an earlier choice replaced the later one");
  assert.equal(hero.pdf.href, third.dataset.pdf);
});

await test("a preview that fails still brings the caption along", async () => {
  const hero = run(heroDom(), { viewerReady: true });
  const chosen = hero.options()[1];
  hero.click(chosen);
  hero.images.at(-1).reject(new Error("offline"));
  await settle();
  assert.equal(hero.title.textContent, chosen.dataset.title);
  assert.equal(hero.pdf.href, chosen.dataset.pdf);
});

await test("a click that lands on no option changes nothing", async () => {
  const hero = run(heroDom(), { viewerReady: true });
  const pressed = pressedStates(hero);
  const src = hero.image.src;
  hero.click(hero.group);
  await settle();
  assert.equal(hero.images.length, 0, "a click on no document started loading one");
  assert.equal(hero.image.src, src);
  assert.deepEqual(pressedStates(hero), pressed);
});

await test("a single document gets no switch", () => {
  const hero = run(heroDom({ keep: 1 }), { viewerReady: true });
  assert.equal(hero.group.hidden, true, "a switch with one position switches nothing");
});

await test("a page without the hero is left alone", () => {
  const document = { querySelector: () => null, documentElement: { dataset: {} }, addEventListener() {} };
  assert.doesNotThrow(() => vm.runInNewContext(script, { document }, { filename: "web/home.js" }));
});

if (failures.length > 0) {
  console.error(`\nhome: ${failures.length} case(s) failed`);
  process.exit(1);
}
console.log("home: all cases passed");
