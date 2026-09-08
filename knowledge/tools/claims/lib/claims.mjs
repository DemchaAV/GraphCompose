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

/** `Class`, or `Class#method`, both Java identifiers and nothing else. */
const TEST_PROOF_RE = /^[A-Za-z_$][A-Za-z0-9_$]*(?:#[A-Za-z_$][A-Za-z0-9_$]*)?$/;

/**
 * Whether a `test:` proof id is well formed.
 *
 * @param {string} id the part after `test:`
 * @returns {boolean} true for `Class` or `Class#method`
 */
export function isTestProofId(id) {
  return TEST_PROOF_RE.test(id);
}

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

    // A `test:` id is read back as Java identifiers, never as free text. The
    // method half used to reach a RegExp by interpolation, where a `.` or a `|`
    // in it silently widened what counted as a match; rejecting the shape here
    // means nothing downstream has to sanitise it.
    if (attributes.proof?.startsWith("test:") && !isTestProofId(attributes.proof.slice("test:".length))) {
      errors.push({
        ...at,
        message:
          `proof "${attributes.proof}" is not a test reference — expected test:Class or ` +
          "test:Class#method with Java identifiers.",
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

/**
 * Java source with every comment, string, character and text-block body
 * replaced by spaces of the same length.
 *
 * Offsets are preserved so a match in the result points at the same place in
 * the original. Blanking rather than deleting is what lets the caller read the
 * characters *before* a match and still be looking at real code.
 *
 * @param {string} source Java source
 * @returns {string} the same length, with only code left
 */
export function stripCommentsAndLiterals(source) {
  const out = source.split("");
  const blank = (from, to) => {
    for (let k = from; k < to && k < out.length; k++) {
      if (out[k] !== "\n" && out[k] !== "\r") out[k] = " ";
    }
  };

  for (let i = 0; i < source.length; ) {
    const two = source.slice(i, i + 2);
    if (two === "//") {
      let end = source.indexOf("\n", i);
      if (end === -1) end = source.length;
      blank(i, end);
      i = end;
    } else if (two === "/*") {
      let end = source.indexOf("*/", i + 2);
      end = end === -1 ? source.length : end + 2;
      blank(i, end);
      i = end;
    } else if (source.startsWith('"""', i)) {
      let end = source.indexOf('"""', i + 3);
      end = end === -1 ? source.length : end + 3;
      blank(i, end);
      i = end;
    } else if (source[i] === '"' || source[i] === "'") {
      const quote = source[i];
      let j = i + 1;
      while (j < source.length && source[j] !== quote) {
        if (source[j] === "\\") j++;
        if (source[j] === "\n") break;
        j++;
      }
      const end = Math.min(j + 1, source.length);
      blank(i, end);
      i = end;
    } else {
      i++;
    }
  }
  return out.join("");
}

/**
 * Annotations that mark a method JUnit will actually run.
 */
const TEST_ANNOTATIONS = new Set([
  "Test", "ParameterizedTest", "RepeatedTest", "TestFactory", "TestTemplate",
]);

/**
 * Skips a balanced bracket pair starting at {@code open}.
 *
 * @param {string} code comment- and literal-free Java source
 * @param {number} open index of the opening bracket
 * @param {string} openCh the opening bracket character
 * @param {string} closeCh the matching closing bracket character
 * @returns {number} index just past the matching close, or the source length
 */
function skipPair(code, open, openCh, closeCh) {
  let depth = 0;
  for (let i = open; i < code.length; i++) {
    if (code[i] === openCh) depth++;
    else if (code[i] === closeCh && --depth === 0) return i + 1;
  }
  return code.length;
}

/** Advances past whitespace. */
function skipSpace(code, at) {
  let i = at;
  while (i < code.length && /\s/.test(code[i])) i++;
  return i;
}

/**
 * The annotation names in the run beginning at {@code at}, without requiring the
 * run to resolve to a method — a run on a type resolves to none.
 *
 * @param {string} code comment- and literal-free Java source
 * @param {number} at index of the first `@` in the run
 * @returns {string[]} the annotation simple names, in source order
 */
function readRunAnnotations(code, at) {
  const names = [];
  let i = at;
  while (i < code.length && code[i] === "@") {
    const m = /^@\s*([A-Za-z_$][\w$]*)/.exec(code.slice(i));
    if (!m) break;
    names.push(m[1]);
    i = skipSpace(code, i + m[0].length);
    if (code[i] === "(") i = skipPair(code, i, "(", ")");
    i = skipSpace(code, i);
  }
  return names;
}

/**
 * Reads the method name of the declaration that begins at {@code at} — a
 * position just past a leading annotation.
 *
 * Only annotations, modifiers, a type-parameter list and a return type can stand
 * between an annotation and the name, and none of them is an expression. That is
 * what makes this readable without parsing Java: scanning FORWARD from an anchor
 * meets a fixed grammar, where scanning backward from `name(` meets arbitrary
 * code that happens to end in the same characters.
 *
 * @param {string} code comment- and literal-free Java source
 * @param {number} at index just past the anchoring annotation
 * @returns {{name: string, annotations: string[]}|null} the declaration, or null
 */
function readDeclaration(code, at) {
  const annotations = [];
  let i = skipSpace(code, at);
  let last = null;

  while (i < code.length) {
    const c = code[i];
    if (c === "@") {
      const m = /^@\s*([A-Za-z_$][\w$]*)/.exec(code.slice(i));
      if (!m) return null;
      annotations.push(m[1]);
      i = skipSpace(code, i + m[0].length);
      if (code[i] === "(") i = skipPair(code, i, "(", ")");
      i = skipSpace(code, i);
      continue;
    }
    if (c === "<") {
      // A type-parameter list, or the type arguments of the return type.
      i = skipSpace(code, skipPair(code, i, "<", ">"));
      continue;
    }
    if (c === "[" || c === "]" || c === ".") {
      i = skipSpace(code, i + 1);
      continue;
    }
    if (/[A-Za-z_$]/.test(c)) {
      const m = /^[A-Za-z_$][\w$]*/.exec(code.slice(i));
      last = m[0];
      i = skipSpace(code, i + m[0].length);
      if (code[i] === "(") return { name: last, annotations };
      continue;
    }
    // A field, a nested type, or anything else this anchor did not introduce.
    return null;
  }
  return null;
}

/**
 * Whether Java source declares a JUnit test method of this name that would
 * actually run.
 *
 * A `proof=test:Class#method` marker asserts that a named test holds a claim, so
 * the question is not "does some method of that name exist" — it is "is that
 * test there and enabled". Searching the file for `method\s*\(` — which this
 * replaced — answered neither: a mention in a comment, in a string, or at a call
 * site all counted, so a claim whose proof had been deleted went on reading as
 * proven.
 *
 * Anchoring on the test annotation and reading FORWARD is what makes the answer
 * decidable. The reverse — finding `name(` and asking what precedes it — cannot
 * be done by scanning: `Map<K, V> name(` and `a < b ? c > name(1)` are the same
 * characters, and `?` is both a wildcard and a ternary. Between an annotation and
 * the name there are no expressions at all.
 *
 * A `@Disabled` test proves nothing and is refused. The name is compared by
 * string equality against an identifier the scan read, so a name carrying a regex
 * metacharacter simply matches nothing — it reaches no pattern to be one in.
 *
 * @param {string} source Java source
 * @param {string} method the method name
 * @returns {boolean} true when the source declares it as a live test
 */
export function declaresTestMethod(source, method) {
  const code = stripCommentsAndLiterals(source);
  let found = false;

  for (const m of code.matchAll(/@s*[A-Za-z_$][w$]*/g)) {
    const declared = readDeclaration(code, m.index);
    const disabled = declared === null || declared.annotations.includes("Disabled");

    // A run that carries @Disabled but resolves to no method annotates a TYPE,
    // and a disabled class runs none of its tests. Refusing the whole file is
    // coarser than JUnit — a disabled nested class takes the outer ones with it —
    // and it is the safe direction: the cost is a claim that has to name another
    // test, against a claim that reads as proven by something that never runs.
    if (declared === null) {
      if (readRunAnnotations(code, m.index).includes("Disabled")) return false;
      continue;
    }
    if (declared.name !== method) continue;
    if (!declared.annotations.some((a) => TEST_ANNOTATIONS.has(a))) continue;
    if (disabled) return false;
    found = true;
  }
  return found;
}
