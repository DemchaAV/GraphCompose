/**
 * knowledge/tools/api-surface/lib/stability-doc.mjs — which `@Beta` markers the
 * stability document has to name, and which of them it does not.
 *
 * Split out of `check-stability-doc.mjs` so the rule can be held to fixtures.
 * The rule is the whole value of that gate: a check that reports everything is
 * as useless as one that reports nothing, and neither is visible from a green
 * CI run.
 */

/**
 * Everything that *originates* a beta marker, across the surfaces given.
 *
 * A surface document records inherited stability the same way as declared, so
 * the two are told apart here: a type is an origin when its package is not
 * beta, and a member is an origin when its type is not.
 *
 * **Only originating markers are required.** A type that is beta because its
 * package is, or a member that is beta because its type is, inherits the status
 * and is covered by the entry naming its origin — demanding a line for each of
 * the fifteen PPTX handlers would produce a document nobody finishes reading.
 * What must be named is every place the marker is actually *written*.
 *
 * @param {Array<{packages?: Array<object>}>} surfaces parsed surface documents
 * @returns {Array<{what: string, kind: "package"|"type"|"member"}>} deduplicated by name
 */
export function originatingBetas(surfaces) {
  const origins = new Map();
  for (const surface of surfaces) {
    for (const pkg of surface.packages ?? []) {
      const packageIsBeta = pkg.stability === "beta";
      if (packageIsBeta) origins.set(pkg.name, { what: pkg.name, kind: "package" });

      for (const type of pkg.types ?? []) {
        const typeIsBeta = type.stability === "beta";
        if (typeIsBeta && !packageIsBeta) origins.set(type.name, { what: type.name, kind: "type" });
        for (const member of type.members ?? []) {
          if (member.stability === "beta" && !typeIsBeta) {
            origins.set(`${type.name}.${member.name}`, {
              what: `${type.name}.${member.name}`,
              kind: "member",
            });
          }
        }
      }
    }
  }
  return [...origins.values()];
}

/**
 * The origins the document does not name, sorted so the report is stable.
 *
 * Named is "the document contains this identifier": the doc writes short forms
 * (`toPptxBytes`, `…pptx.handlers`), so requiring a fully-qualified match would
 * fail on prose that is perfectly clear.
 *
 * @param {Array<{what: string, kind: string}>} origins from {@link originatingBetas}
 * @param {string} doc the stability document's text
 */
export function unnamedOrigins(origins, doc) {
  const shortName = (what) => what.split(".").pop();
  return origins
    .filter((o) => !doc.includes(o.what) && !doc.includes(shortName(o.what)))
    .sort((a, b) => a.what.localeCompare(b.what));
}

/** How many origins of each kind, for the line a passing run prints. */
export function countByKind(origins) {
  return origins.reduce((acc, o) => ({ ...acc, [o.kind]: (acc[o.kind] ?? 0) + 1 }), {});
}
