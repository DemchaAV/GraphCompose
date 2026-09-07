/**
 * knowledge/tools/routing/lib/anchors.mjs — resolve a heading reference the way
 * GitHub does, because that is where the reader opens it.
 *
 * A route hands over exactly one anchor. If that anchor does not resolve in a
 * browser the route is worse than no route: it carries the authority of a
 * generated artifact to a page that scrolls nowhere. So the gate's rule has to
 * be *GitHub's* rule, not an approximation of it.
 *
 * This repository already had that rule, written twice and disagreeing:
 * `DocumentationLinkGuardTest.slug` in Java mirrors GitHub (`replaceAll("\\s",
 * "-")` — one hyphen per whitespace character), while the routing gate collapsed
 * runs (`replace(/\s+/g, "-")`). House style uses spaced em dashes, so the two
 * differ on 121 of the 786 headings under `docs/`: `## Zebra — alternating row
 * fills` is `#zebra--alternating-row-fills` on GitHub and was
 * `#zebra-alternating-row-fills` to the gate. That is failure in *both*
 * directions — CI red on a link that works, green on a link that 404s — which is
 * why this is a module with fixtures rather than four lines inlined in the
 * checker.
 *
 * The Java guard is the reference implementation; every rule here mirrors it
 * deliberately, and `test/anchors.test.mjs` pins the cases where a naive version
 * diverges.
 */

/** A fenced code block. Nothing inside one is a heading. */
const FENCED_BLOCK = /^[ \t]*```[\s\S]*?^[ \t]*```[ \t]*$/gm;

/** An ATX heading line. */
const HEADING = /^#{1,6}[ \t]+(.+?)[ \t]*$/gm;

/** A hand-written anchor: `<a name="x">` or `<a id="x">`. */
const EXPLICIT_ANCHOR = /<a\s+(?:name|id)=["']([^"']+)["']/g;

const HTML_TAG = /<[^>]+>/g;
/** `[text](url)` inside a heading contributes only its text. */
const HEADING_LINK = /\[([^\]]*)\]\([^)]*\)/g;
/**
 * Everything GitHub drops from an anchor.
 *
 * The keep-set was read off GitHub rather than reasoned about, by posting
 * headings to `api.github.com/markdown` and reading the ids it generates. It
 * keeps letters of any script, combining marks, decimal digits, `_`, `-` and the
 * ASCII space — and drops everything else, *including every other kind of
 * whitespace*:
 *
 *   `Zebra<NBSP>alternating rows`   -> `zebraalternating-rows`  (not `zebra-…`)
 *   `Row<EM SPACE>span`             -> `rowspan`
 *   `A<TAB>B`                       -> `ab`
 *   `Item <U+2460> first`           -> `item--first`  (the numeral goes, the spaces stay)
 *   `Café rules` (decomposed)       -> `café-rules`   (the combining mark stays)
 *   `Zebra — alternating row fills` -> `zebra--alternating-row-fills`
 *
 * Two earlier versions of this rule guessed instead. One collapsed whitespace
 * runs, which broke every heading with a spaced em dash. The next hyphenated all
 * Unicode whitespace and kept non-decimal numerals, on the theory that GitHub
 * would — it does neither.
 */
const NOT_IN_ANCHOR = /[^\p{L}\p{M}\p{Nd}_ -]/gu;

/**
 * A repo-relative `path.md#anchor` written in prose.
 *
 * The leading boundary is the whole point. Without it the tail of an absolute
 * URL matches: in `https://github.com/o/r/blob/develop/docs/x.md#y` a match can
 * start right after the colon and yield `//github.com/o/r/blob/develop/docs/x.md`,
 * which then fails `existsSync` and rejects the route for "no such page" — the
 * gate blaming the author for the gate's own regex. `:` is in the lookbehind for
 * exactly that reason.
 *
 * The anchor half accepts the same characters {@link anchorOf} can emit. An
 * ASCII-only `[\w-]+` truncated a citation of a heading in any other script —
 * `#café-rules` became `#caf` — and the gate then rejected the route for a link
 * that opens fine, while the identical reference written in `docs:` passed,
 * because that path never goes through this regex.
 */
const PROSE_REF = /(?<![\w./:-])([\w.-][\w./-]*\.md)#([\p{L}\p{M}\p{Nd}_-]+)/gu;

/**
 * GitHub's heading-to-anchor rule.
 *
 * @param {string} heading the heading text, without the leading `#`s
 * @returns {string} the anchor GitHub would generate
 */
export function anchorOf(heading) {
  let text = heading.trim().replace(HTML_TAG, "");
  text = text.replace(HEADING_LINK, "$1");
  text = text.replace(/`/g, "").toLowerCase();
  text = text.replace(NOT_IN_ANCHOR, "");
  // One hyphen per ASCII space, not per run and not per whitespace character:
  // GitHub does not collapse runs, and every other kind of whitespace has
  // already been removed by the line above rather than hyphenated.
  return text.replace(/ /g, "-");
}

/**
 * Every anchor a Markdown document actually offers.
 *
 * Fenced blocks are stripped first, so a `# comment` inside a shell example is
 * not an anchor — it renders as code, and a route citing it would hand the
 * reader a link that lands nowhere while the gate stayed green.
 *
 * Repeated headings get GitHub's `-1`, `-2` suffixes, so a page with two
 * `## Notes` offers `#notes` and `#notes-1`.
 *
 * @param {string} markdown the document text
 * @returns {Set<string>} anchors that resolve on the rendered page
 */
export function anchorsIn(markdown) {
  const body = markdown.replace(FENCED_BLOCK, "");
  const anchors = new Set();
  const seen = new Map();

  for (const [, heading] of body.matchAll(HEADING)) {
    const slug = anchorOf(heading);
    const occurrence = seen.get(slug) ?? 0;
    seen.set(slug, occurrence + 1);
    anchors.add(occurrence === 0 ? slug : `${slug}-${occurrence}`);
  }
  for (const [, explicit] of body.matchAll(EXPLICIT_ANCHOR)) {
    anchors.add(explicit);
  }
  return anchors;
}

/**
 * Every repo-relative `page.md#anchor` reference written in a prose field.
 *
 * @param {string} prose the field text, or null/undefined
 * @returns {string[]} each reference, as written
 */
export function anchorRefsIn(prose) {
  return [...(prose ?? "").matchAll(PROSE_REF)].map((m) => `${m[1]}#${m[2]}`);
}
