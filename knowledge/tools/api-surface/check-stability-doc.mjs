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
 * The rule for what must be named lives in `lib/stability-doc.mjs`, where
 * `test/stability-doc.test.mjs` holds it to fixtures. This file is the file
 * plumbing around that rule.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { originatingBetas, unnamedOrigins, countByKind } from "./lib/stability-doc.mjs";

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

// `excluded.json` records what the surfaces leave out rather than what they
// admit, so it carries no packages to read markers from.
const surfaces = fs
  .readdirSync(API_DIR)
  .filter((f) => f.endsWith(".json") && f !== "excluded.json")
  .map((f) => JSON.parse(fs.readFileSync(path.join(API_DIR, f), "utf8")));

const origins = originatingBetas(surfaces);
const unnamed = unnamedOrigins(origins, doc);

if (unnamed.length) {
  process.stderr.write(
    `[stability-doc] ${unnamed.length} thing(s) carry @Beta and are not named in docs/api-stability.md:\n\n` +
      unnamed.map((o) => `    ${o.kind.padEnd(8)} ${o.what}\n`).join("") +
      "\n  A reader asking what is still moving reads that document, so a marker it\n" +
      "  does not mention is a promise nobody made. Name it, or drop the marker.\n",
  );
  process.exit(1);
}

const counts = countByKind(origins);
process.stdout.write(
  `[stability-doc] every originating @Beta is named — ` +
    `${counts.package ?? 0} package(s), ${counts.type ?? 0} type(s), ${counts.member ?? 0} member(s)\n`,
);
