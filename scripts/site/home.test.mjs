/**
 * node scripts/site/home.test.mjs — exit 0 when every case holds.
 *
 * Runs web/home.js the way the page does, as a plain script, against the hero exactly as the build
 * wrote it: the hero's markup is parsed out of web/index.html into a DOM small enough to live in
 * this file, so a hook renamed, or an element moved out of the figure it has to sit in, fails here
 * instead of leaving a switch that silently never appears. Checked: the switch shows only when it
 * can do something; both links work before any script runs; choosing a document changes its
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
    page: body.querySelector("[data-hero-page]"),
    options: () => group.querySelectorAll("[data-hero-option]"),
  };
}

/** Runs home.js over a hero, with previews whose readiness the test decides. */
function run(dom) {
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
  const document = {
    querySelector: (selector) => dom.body.querySelector(selector),
  };
  vm.runInNewContext(script, { document, Image, Promise }, { filename: "web/home.js" });
  return {
    ...dom,
    images,
    click: (target) => (dom.group.listeners.click || []).forEach((listener) => listener({ target })),
  };
}

const settle = () => new Promise((resolve) => setImmediate(resolve));
const pressedStates = (hero) => hero.options().map((option) => option.getAttribute("aria-pressed"));

await test("the built hero carries every hook home.js reads, and the page loads home.js", () => {
  for (const hook of ["data-hero", "data-hero-image", "data-hero-title", "data-hero-pdf", "data-hero-page",
    "data-hero-switch", "data-hero-option"]) {
    // As a whole attribute: "data-hero" is also how every other hook begins.
    assert.match(page, new RegExp(`\\s${hook}(?=[\\s>=])`), `web/index.html has no ${hook} attribute`);
  }
  assert.match(page, /<script src="home\.js"><\/script>/, "index.html does not load home.js");
  assert.ok(heroDom().options().length > 1, "the hero offers fewer than two documents, so the switch cases test nothing");
});

await test("both links under the document work before any script has run", () => {
  // Nothing here runs home.js: this is the hero a reader without JavaScript gets.
  const hero = heroDom();
  const first = hero.options()[0].dataset;
  assert.equal(hero.pdf.hidden, false);
  assert.equal(hero.page.hidden, false, "the link to the document's page is the one a reader without JavaScript can follow");
  assert.equal(hero.pdf.href, first.pdf);
  assert.equal(hero.page.href, first.page, "the page link names the document on screen");
});

await test("the switch shows once the script runs", () => {
  const hero = run(heroDom());
  assert.equal(hero.group.hidden, false,
    "the switch never appeared: home.js found no figure, image, title or link where the built page puts them");
});

await test("choosing a document changes only which option is pressed until its preview is ready", async () => {
  const hero = run(heroDom());
  const before = { src: hero.image.src, title: hero.title.textContent, pdf: hero.pdf.href, page: hero.page.href };
  const chosen = hero.options()[2];
  hero.click(chosen);
  await settle();
  assert.deepEqual(pressedStates(hero), hero.options().map((option) => String(option === chosen)));
  assert.deepEqual({ src: hero.image.src, title: hero.title.textContent, pdf: hero.pdf.href, page: hero.page.href }, before,
    "the page described the new document before its picture was ready");
  assert.equal(hero.figure.getAttribute("aria-busy"), "true");
});

await test("once the preview is ready, the picture, its size, the caption and both links change together", async () => {
  const hero = run(heroDom());
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
  assert.equal(hero.page.href, data.page);
  assert.equal(hero.figure.getAttribute("aria-busy"), null);
});

await test("a preview that becomes ready after a newer choice is dropped", async () => {
  const hero = run(heroDom());
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
  assert.equal(hero.page.href, third.dataset.page);
});

await test("a preview that fails still brings the caption along", async () => {
  const hero = run(heroDom());
  const chosen = hero.options()[1];
  hero.click(chosen);
  hero.images.at(-1).reject(new Error("offline"));
  await settle();
  assert.equal(hero.title.textContent, chosen.dataset.title);
  assert.equal(hero.pdf.href, chosen.dataset.pdf);
  assert.equal(hero.page.href, chosen.dataset.page);
});

await test("a click that lands on no option changes nothing", async () => {
  const hero = run(heroDom());
  const pressed = pressedStates(hero);
  const src = hero.image.src;
  hero.click(hero.group);
  await settle();
  assert.equal(hero.images.length, 0, "a click on no document started loading one");
  assert.equal(hero.image.src, src);
  assert.deepEqual(pressedStates(hero), pressed);
});

await test("a single document gets no switch", () => {
  const hero = run(heroDom({ keep: 1 }));
  assert.equal(hero.group.hidden, true, "a switch with one position switches nothing");
});

await test("a page without the hero is left alone", () => {
  const document = { querySelector: () => null };
  assert.doesNotThrow(() => vm.runInNewContext(script, { document }, { filename: "web/home.js" }));
});

if (failures.length > 0) {
  console.error(`\nhome: ${failures.length} case(s) failed`);
  process.exit(1);
}
console.log("home: all cases passed");
