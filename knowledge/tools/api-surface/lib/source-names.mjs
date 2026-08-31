/**
 * tools/api-surface/lib/source-names.mjs — parameter names, and only that.
 *
 * Bytecode is the authority on what exists; it just does not carry parameter
 * names. `DocumentBuilder margin(float, float, float, float)` is a worse
 * allow-list entry than `margin(float top, float right, float bottom, float
 * left)`, and the difference is four names a reader cannot guess.
 *
 * So the sources jar for the same pinned coordinates is parsed for names alone.
 * Nothing here can add a member: a signature the source declares and the class
 * file does not is a signature that does not exist at runtime, and a signature
 * the class file has and the source does not is Lombok-generated — which is the
 * whole reason the extractor moved to bytecode. That second case is recorded as
 * `origin: "generated"` rather than dropped, and it is what the regression test
 * watches.
 */

import { simplifyType } from "./javap.mjs";

const SOURCE_METHOD = /\bpublic\b(?!\s+(?:class|interface|record|enum|@))([^;{=()]*?)\(([^{;]*?)\)/gs;

function stripComments(source) {
  return source.replace(/\/\*[\s\S]*?\*\//g, "").replace(/\/\/[^\n]*/g, "");
}

/** Split on commas at depth 0 so `Map<K, V> x` stays one parameter. */
function splitParams(text) {
  const out = [];
  let depth = 0;
  let current = "";
  for (const ch of text) {
    if (ch === "<" || ch === "(") depth += 1;
    else if (ch === ">" || ch === ")") depth -= 1;
    if (ch === "," && depth === 0) {
      out.push(current);
      current = "";
      continue;
    }
    current += ch;
  }
  if (current.trim()) out.push(current);
  return out.map((s) => s.trim()).filter(Boolean);
}

/**
 * The shape a parameter type is compared by. Annotations, `final`, varargs
 * spelling and package qualification are all things the two sides spell
 * differently for the same type.
 */
export function normalizeParamType(type) {
  return (
    simplifyType(
      type
        .replace(/@\w+(\([^)]*\))?/g, "")
        .replace(/\bfinal\b/g, "")
        .replace(/\.\.\./g, "[]")
        .replace(/\s+/g, " ")
        .trim(),
    )
      // A nested type is `CardWidget.Style` in bytecode and plain `Style` in the
      // source that declares it, so the two never matched and eighteen widget
      // methods came out unnamed and labelled generated. Comparing on the last
      // segment settles it; two overloads differing only by which type encloses
      // a same-named parameter would collide, and none does.
      .replace(/\b[A-Z]\w*\./g, "")
      .replace(/\s+/g, "")
  );
}

const keyOf = (typeName, member, paramTypes) =>
  `${typeName}#${member}#${paramTypes.map(normalizeParamType).join(",")}`;

/**
 * Index every public method declaration in a sources jar by
 * (type, member, parameter types) → parameter names.
 *
 * @param {{names: string[], read(name: string): Buffer}} sourcesJar
 * @param {(entryName: string) => boolean} accept
 */
export function indexParameterNames(sourcesJar, accept = () => true) {
  const index = new Map();

  for (const entry of sourcesJar.names) {
    if (!entry.endsWith(".java") || entry.endsWith("package-info.java")) continue;
    if (!accept(entry)) continue;

    const typeName = entry.slice(entry.lastIndexOf("/") + 1, -".java".length);
    const source = stripComments(sourcesJar.read(entry).toString("utf8"));

    for (const match of source.matchAll(SOURCE_METHOD)) {
      const head = match[1].replace(/\s+/g, " ").trim();
      const params = splitParams(match[2]);
      const headTokens = head.split(" ").filter(Boolean);
      const member = headTokens[headTokens.length - 1];
      if (!member || !/^[A-Za-z_]\w*$/.test(member)) continue;

      const types = [];
      const names = [];
      for (const param of params) {
        // "final Map<K, V> lookup" → type "Map<K, V>", name "lookup"
        const cleaned = param.replace(/@\w+(\([^)]*\))?/g, "").trim();
        const split = cleaned.lastIndexOf(" ");
        if (split === -1) {
          types.push(cleaned);
          names.push(null);
          continue;
        }
        types.push(cleaned.slice(0, split).trim());
        names.push(cleaned.slice(split + 1).trim().replace(/\[\]$/, ""));
      }
      if (names.some((n) => n === null)) continue;

      // Nested types are declared inside the outer type's file, so indexing by
      // the file's type name already reaches them.
      index.set(keyOf(typeName, member, types), names);
    }
  }

  return index;
}

/**
 * Fill in parameter names on one member, and say where it came from.
 *
 * @returns {"source"|"generated"} whether the source declares this signature
 */
export function applyParameterNames(index, simpleTypeName, member) {
  const paramTypes = (member.params ?? []).map((p) => p.type);
  const name = member.kind === "constructor" ? simpleTypeName.split(".").pop() : member.name;

  // Matched only against the declaring type. A same-signature method on some
  // other type would supply a plausible-looking name for a parameter it knows
  // nothing about — `centerText(String text)` where the real field is
  // `centerText`. An unnamed parameter is honest; a borrowed name is not.
  const names = index.get(keyOf(simpleTypeName.split(".")[0], name, paramTypes));
  if (!names) return "generated";

  member.params.forEach((param, i) => {
    if (names[i]) param.name = names[i];
  });
  return "source";
}
