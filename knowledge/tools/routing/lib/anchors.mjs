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
 * Everything GitHub drops from an anchor: punctuation, but not spaces or
 * hyphens.
 *
 * `\p{M}` is in the keep-set because GitHub keeps combining marks: a decomposed
 * `Café` anchors with its accent intact, and stripping it produced `cafe-` where
 * the rendered page offers `café-`. The Java guard kept marks and dropped
 * non-decimal numerals; this keeps both, which is what the rendered page does.
 */
const NOT_IN_ANCHOR = /[^\p{L}\p{N}\p{M}_\s-]/gu;

/**
 * A repo-relative `path.md#anchor` written in prose.
 *
 * The leading boundary is the whole point. Without it the tail of an absolute
 * URL matches: in `https://github.com/o/r/blob/develop/docs/x.md#y` a match can
 * start right after the colon and yield `//github.com/o/r/blob/develop/docs/x.md`,
 * which then fails `existsSync` and rejects the route for "no such page" — the
 * gate blaming the author for the gate's own regex. `:` is in the lookbehind for
 * exactly that reason.
 */
const PROSE_REF = /(?<![\w./:-])([\w.-][\w./-]*\.md)#([\w-]+)/g;

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
  // One hyphen per whitespace character, not per run: this is the line the two
  // implementations disagreed on, and GitHub does not collapse.
  return text.replace(/\s/g, "-");
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
