#!/usr/bin/env node
/**
 * knowledge/tools/api-surface/check-stability-doc.mjs — does the stability
 * document still name everything that carries `@Beta`?
 *
 *   node knowledge/tools/api-surface/check-stability-doc.mjs
 *
 * Exit 0 every originating `@Beta` is named · 1 something is unnamed · 2 usage.
 *
 * `docs/api-stability.md` is where a reader goes to ask "what is still moving".
 * It answers by naming things — the PPTX packages, `NodeDefinition`, the PPTX
 * convenience methods on `DocumentSession` — and a name that stops being true,
 * or a marker that never acquires one, is invisible until someone reads both the
 * document and the code side by side.
 *
 * Nothing was checking that. The document cites `BetaAnnotationDocumentationTest`
 * as its guard, but that test examines the *annotation type* — its retention,
 * its targets, its own Javadoc. It never looks at what carries the annotation.
 * So four `@Beta` members of the PDF backend went undocumented while every other
 * beta surface was enumerated carefully: not neglect, just an unguarded seam.
 *
 * **Only originating markers are required.** A type that is beta because its
 * package is, or a member that is beta because its type is, inherits the status
 * and is covered by the entry naming its origin — demanding a line for each of
 * the fifteen PPTX handlers would produce a document nobody finishes reading.
 * What must be named is every place the marker is actually *written*.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(HERE, "..", "..", "..");
const API_DIR = path.join(REPO_ROOT, "knowledge", "api");
const DOC = path.join(REPO_ROOT, "docs", "api-stability.md");

if (process.argv.length > 2) {
  process.stdout.write(
    "usage: node knowledge/tools/api-surface/check-stability-doc.mjs\n\n" +
      "exit: 0 every originating @Beta is named | 1 something is unnamed | 2 usage\n",
  );
  process.exit(process.argv[2] === "--help" || process.argv[2] === "-h" ? 0 : 2);
}

if (!fs.existsSync(API_DIR)) {
  process.stderr.write("[stability-doc] no surfaces — run extract-api.mjs --from-reactor first\n");
  process.exit(1);
}

const doc = fs.readFileSync(DOC, "utf8");

/**
 * Everything that *originates* a beta marker.
 *
 * A surface document records inherited stability the same way as declared, so
 * the two are told apart here: a type is an origin when its package is not beta,
 * and a member is an origin when its type is not.
 */
const origins = new Map();
for (const file of fs.readdirSync(API_DIR).filter((f) => f.endsWith(".json") && f !== "excluded.json")) {
  const surface = JSON.parse(fs.readFileSync(path.join(API_DIR, file), "utf8"));
  for (const pkg of surface.packages ?? []) {
    const packageIsBeta = pkg.stability === "beta";
    if (packageIsBeta) origins.set(pkg.name, { what: pkg.name, kind: "package" });

    for (const type of pkg.types ?? []) {
      const typeIsBeta = type.stability === "beta";
      // A type in a beta package inherits it; only a marker written on the type
      // itself is an origin.
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

// Named is "the document contains this identifier": the doc writes short forms
// (`toPptxBytes`, `…pptx.handlers`), so requiring a fully-qualified match would
// fail on prose that is perfectly clear.
const shortName = (what) => what.split(".").pop();
const unnamed = [...origins.values()]
  .filter((o) => !doc.includes(o.what) && !doc.includes(shortName(o.what)))
  .sort((a, b) => a.what.localeCompare(b.what));

if (unnamed.length) {
  process.stderr.write(
    `[stability-doc] ${unnamed.length} thing(s) carry @Beta and are not named in docs/api-stability.md:\n\n` +
      unnamed.map((o) => `    ${o.kind.padEnd(8)} ${o.what}\n`).join("") +
      "\n  A reader asking what is still moving reads that document, so a marker it\n" +
      "  does not mention is a promise nobody made. Name it, or drop the marker.\n",
  );
  process.exit(1);
}

const counts = [...origins.values()].reduce((acc, o) => ({ ...acc, [o.kind]: (acc[o.kind] ?? 0) + 1 }), {});
process.stdout.write(
  `[stability-doc] every originating @Beta is named — ` +
    `${counts.package ?? 0} package(s), ${counts.type ?? 0} type(s), ${counts.member ?? 0} member(s)\n`,
);
