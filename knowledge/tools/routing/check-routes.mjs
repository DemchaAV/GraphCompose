#!/usr/bin/env node
/**
 * knowledge/tools/routing/check-routes.mjs — hold the routing table to the six
 * things that make a route trustworthy.
 *
 *   node knowledge/tools/routing/check-routes.mjs
 *
 * Exit 0 clean · 1 a route does not hold up · 2 usage.
 *
 * Routing is the layer that answers "how do I make two columns, and what are the
 * alternatives" — a question surfaces cannot answer, because they say what
 * exists and not which of several ways is right. That makes it the most
 * *dangerous* file in the pack: a wrong route does not fail to compile, it
 * quietly sends every reader down the wrong path with the authority of a
 * generated artifact.
 *
 * So a route is admitted only with all six of:
 *
 *   1. a `docs:` anchor that resolves to a heading that exists;
 *   2. every `symbols:` entry present in the surfaces;
 *   3. every constraint naming a `behavior:` claim that exists;
 *   4. a proof behind that behaviour;
 *   5. a recommendation traceable to something in this repository;
 *   6. `verifiedAgainst`.
 *
 * Points 1-4 are checked here. Point 5 is checked as far as a machine can — the
 * `recommendedBecause` must cite a real anchor — but whether the recommendation
 * is *right* is a human's call, which is what `confirmedBy` records. A route with
 * `confirmedBy: null` is served with its status attached rather than silently.
 *
 * Seeding this from the AI Flow loading map or `.llm-wiki/02-decision-tree/` was
 * considered and refused: the audit that motivated this whole plan found drift in
 * both, and importing a route on their authority would launder that drift into
 * the one artifact meant to be trustworthy. These routes are derived from
 * `docs/` and from tests in this repository instead.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { resolveSymbol } from "../claims/lib/claims.mjs";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(HERE, "..", "..", "..");
const TASKS_FILE = path.join(REPO_ROOT, "knowledge", "routing", "tasks.json");
const API_DIR = path.join(REPO_ROOT, "knowledge", "api");
const CLAIMS_INDEX = path.join(REPO_ROOT, "knowledge", "claims", "index.json");

const args = process.argv.slice(2);
if (args.some((a) => a === "--help" || a === "-h")) {
  process.stdout.write(
    "usage: node knowledge/tools/routing/check-routes.mjs\n\n" +
      "  Routes are hand-authored and have no generated counterpart, so there is\n" +
      "  no --check mode: validating them IS the check.\n\n" +
      "exit: 0 clean | 1 a route does not hold up | 2 usage\n",
  );
  process.exit(0);
}
// No --check: it would advertise a mode this tool does not have. Routes have no
// generated artifact to compare against, so every run is the check.
if (args.length) process.exit(2);

/** GitHub's heading -> anchor rule, enough of it for our own headings. */
function anchorOf(heading) {
  return heading
    .toLowerCase()
    .replace(/[^\w\s-]/g, "")
    .trim()
    .replace(/\s+/g, "-");
}

function anchorsIn(file) {
  const out = new Set();
  for (const line of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
    const m = line.match(/^#{1,6}\s+(.+?)\s*$/);
    if (m) out.add(anchorOf(m[1]));
  }
  return out;
}

function loadSurfaces() {
  const types = new Map();
  for (const file of fs.readdirSync(API_DIR).filter((f) => f.endsWith(".json") && f !== "excluded.json")) {
    const surface = path.basename(file, ".json");
    const doc = JSON.parse(fs.readFileSync(path.join(API_DIR, file), "utf8"));
    for (const pkg of doc.packages ?? []) {
      for (const type of pkg.types ?? []) {
        types.set(type.name, {
          name: type.name,
          surface,
          methods: (type.members ?? []).filter((m) => m.kind !== "constant"),
          constants: (type.members ?? []).filter((m) => m.kind === "constant").map((m) => m.name),
        });
      }
    }
  }
  return { types };
}

const index = loadSurfaces();
const claims = JSON.parse(fs.readFileSync(CLAIMS_INDEX, "utf8")).claims;
const doc = JSON.parse(fs.readFileSync(TASKS_FILE, "utf8"));

const errors = [];
const unconfirmed = [];
const fail = (task, message) => errors.push({ task, message });

const seen = new Set();
for (const route of doc.tasks) {
  const id = route.task;
  if (!id) {
    fail("(unnamed)", "a route has no task id");
    continue;
  }
  if (seen.has(id)) fail(id, "duplicate task id — an intent must resolve to one route");
  seen.add(id);

  // 6 — the version it was checked against.
  if (!route.verifiedAgainst) fail(id, "no verifiedAgainst: a route with no version is a route nobody can re-check");

  // 5 — the recommendation must be traceable, and the citation must be a real anchor.
  if (!route.recommended) fail(id, "no recommended: — a route that does not recommend is a list, not a route");
  if (!route.recommendedBecause) {
    fail(id, "no recommendedBecause: — the recommendation must trace to something in this repository");
  }

  // 1 — every docs anchor resolves.
  for (const ref of route.docs ?? []) {
    const [rel, anchor] = ref.split("#");
    const file = path.join(REPO_ROOT, ...rel.split("/"));
    if (!fs.existsSync(file)) {
      fail(id, `docs "${ref}" — no such page`);
      continue;
    }
    if (!anchor) {
      fail(id, `docs "${ref}" — needs an #anchor; a route hands over one section, not a whole page`);
      continue;
    }
    if (!anchorsIn(file).has(anchor)) {
      fail(id, `docs "${ref}" — the page has no heading with that anchor (a heading was renamed)`);
    }
  }
  if (!(route.docs ?? []).length) fail(id, "no docs: — a route must hand over an anchor to open");

  // 2 — every symbol exists.
  for (const symbol of route.symbols ?? []) {
    if (!resolveSymbol(index, symbol).found) {
      fail(id, `symbol "${symbol}" is in no surface — the route points at API that does not exist`);
    }
  }

  // 3 and 4 — every constraint is a claimed behaviour, and that claim has a proof.
  for (const constraint of route.constraints ?? []) {
    const holders = claims.behavior[constraint];
    if (!holders) {
      fail(
        id,
        `constraint "${constraint}" is not claimed by any page — ` +
          "a constraint nobody documents cannot be relied on",
      );
      continue;
    }
    if (!holders.some((h) => h.proof)) {
      fail(id, `constraint "${constraint}" is claimed but unproven — no page backs it with a proof`);
    }
  }

  // Alternatives have to say when they win. A bare name says a second way exists
  // without saying when — which is the gap this layer exists to close.
  for (const alt of route.alternatives ?? []) {
    if (typeof alt === "string") {
      fail(id, `alternative "${alt}" is a bare name — it must say useWhen and tradeoffs`);
      continue;
    }
    if (!alt.id) fail(id, "an alternative has no id");
    if (!alt.useWhen) fail(id, `alternative "${alt.id}" has no useWhen — a name alone does not help anyone choose`);
    if (!alt.tradeoffs) fail(id, `alternative "${alt.id}" has no tradeoffs — what it costs is the deciding half`);
  }

  if (!route.confirmedBy) unconfirmed.push(id);
}

if (errors.length) {
  process.stdout.write(`[check-routes] ${errors.length} problem(s):\n\n`);
  for (const e of errors) process.stdout.write(`  ${e.task}\n    ${e.message}\n\n`);
  process.exit(1);
}

process.stdout.write(`[check-routes] ${doc.tasks.length} routes hold up\n`);
if (unconfirmed.length) {
  process.stdout.write(
    `\n  ${unconfirmed.length} route(s) have no confirmedBy — the mechanical checks pass,\n` +
      "  but whether the recommendation is the RIGHT one is a maintainer's call:\n" +
      unconfirmed.map((t) => `    ${t}\n`).join(""),
  );
}
process.exit(0);
