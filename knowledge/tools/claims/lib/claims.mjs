/**
 * knowledge/tools/claims/lib/claims.mjs — read what the documentation claims.
 *
 * A claim is an HTML comment sitting beside the prose that makes it:
 *
 *   <!-- claim: symbol=RowBuilder.weights proof=snippet:two-column-row -->
 *   <!-- claim: capability=layout.two-columns -->
 *   <!-- claim: behavior=row.rejects-nested-horizontal-row proof=probe:nested-row -->
 *
 * Three storage options were on the table and this is the third.
 *
 * *Front-matter* was rejected on evidence: none of the 80 tracked public pages
 * under `docs/` carries any, and the repository ships no Jekyll or Pages config,
 * so those pages are read directly on GitHub — where YAML front-matter renders
 * as a horizontal rule followed by literal `key: value` text at the top of every
 * page. Damaging 80 published pages for readers so that a tool can find its
 * metadata is the same trade as compiling the library with `-parameters` to
 * serve a docs generator, and it was refused for the same reason.
 *
 * *An external `knowledge/claims/*.yaml` keyed by page path plus anchor* was
 * rejected because the key is fragile in the one way that matters: editing a
 * heading silently breaks the link between a claim and the sentence it is about,
 * and nothing fails. It would also need a YAML parser, and this repository ships
 * no dependencies — `zip.mjs` was hand-written rather than add one.
 *
 * A marker has neither problem. It is invisible to a reader, it travels with the
 * paragraph when the page is reorganised, and it costs one regex — which is
 * exactly how `DocumentationSnippetCompileTest` already finds `doc-example`, so
 * this is the convention the repository established rather than a new one.
 *
 * Claim kinds, and why there are three:
 *
 * - `symbol`   — this page tells you to call this. Checkable against the
 *                surfaces, and the only kind that can be mechanically refuted.
 * - `capability` — this page is how you achieve this intent. Not checkable on
 *                its own; it is what the routing layer resolves an intent to.
 * - `behavior` — this page asserts the engine does something. Not visible in any
 *                signature: a thrown exception, a nesting restriction, a
 *                backend's silent fallback. Only a probe can hold it.
 */

const CLAIM_RE = /^<!--\s*claim:\s*(.+?)\s*-->\s*$/;

const KINDS = ["symbol", "capability", "behavior"];

/** `symbol=A.b proof=snippet:c` → `{symbol: "A.b", proof: "snippet:c"}`. */
function parseAttributes(text) {
  const out = {};
  for (const token of text.split(/\s+/)) {
    const eq = token.indexOf("=");
    if (eq === -1) continue;
    out[token.slice(0, eq)] = token.slice(eq + 1);
  }
  return out;
}

/**
 * Every claim in one Markdown document.
 *
 * @param {string} text the document
 * @param {string} file its repo-relative path, for the report
 * @returns {{claims: Array, errors: Array}}
 */
export function parseClaims(text, file) {
  const claims = [];
  const errors = [];
  const lines = text.split(/\r?\n/);

  // The nearest heading above a claim, so a report can say where in the page it
  // is without the claim itself having to carry a fragile anchor.
  let heading = null;

  lines.forEach((line, i) => {
    const headingMatch = line.match(/^#{1,6}\s+(.+?)\s*$/);
    if (headingMatch) {
      heading = headingMatch[1];
      return;
    }

    const match = line.match(CLAIM_RE);
    if (!match) return;

    const at = { file, line: i + 1, heading };
    const attributes = parseAttributes(match[1]);
    const kinds = KINDS.filter((k) => k in attributes);

    if (kinds.length === 0) {
      errors.push({ ...at, message: `claim has no ${KINDS.join("/")} key: ${match[1]}` });
      return;
    }
    if (kinds.length > 1) {
      // One marker, one assertion. Two kinds in one marker reads as a single
      // fact and is really two, which the reverse index would then conflate.
      errors.push({ ...at, message: `claim mixes ${kinds.join(" and ")} — use one marker each` });
      return;
    }

    const kind = kinds[0];
    const value = attributes[kind];
    if (!value) {
      errors.push({ ...at, message: `${kind}= is empty` });
      return;
    }

    if (attributes.proof && !/^(snippet|probe|render|test):\S+$/.test(attributes.proof)) {
      errors.push({
        ...at,
        message: `proof must be snippet:|probe:|render:|test:<id>, got "${attributes.proof}"`,
      });
      return;
    }

    // A behaviour nobody can reproduce is an opinion. Signatures are checkable
    // against the surfaces and intents are resolved by routing, but a claim that
    // the engine *does* something has nothing holding it up but a probe.
    if (kind === "behavior" && !attributes.proof) {
      errors.push({ ...at, message: `behavior claim "${value}" needs a proof=` });
      return;
    }

    claims.push({ kind, value, proof: attributes.proof ?? null, ...at });
  });

  return { claims, errors };
}

/**
 * Resolve `Type.member` — or a bare `Type` — against the extracted surfaces.
 *
 * Nested receivers are spelled `Outer.Inner.member`, so the type is matched
 * longest-first: splitting on the first dot would look up `GraphCompose` for
 * `GraphCompose.DocumentBuilder.pageSize` and answer confidently about the
 * wrong receiver, which is the exact defect this whole pack replaced.
 */
export function resolveSymbol(index, symbol) {
  const parts = symbol.split(".").filter(Boolean);

  for (let take = Math.min(parts.length, 3); take >= 1; take -= 1) {
    for (let start = 0; start + take <= parts.length; start += 1) {
      const typeName = parts.slice(start, start + take).join(".");
      const type = index.types.get(typeName);
      if (!type) continue;

      const rest = parts.slice(start + take);
      if (rest.length === 0) return { found: true, type, member: null };
      if (rest.length > 1) continue;

      const member = rest[0];
      const overloads = type.methods.filter((m) => m.name === member);
      if (overloads.length) return { found: true, type, member, overloads };
      if (type.constants.includes(member)) return { found: true, type, member, constant: true };

      return { found: false, type, member, reason: `no member "${member}" on ${type.name}` };
    }
  }

  return { found: false, type: null, reason: `no type in "${symbol}"` };
}
