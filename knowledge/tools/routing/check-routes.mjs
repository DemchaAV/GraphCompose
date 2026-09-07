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
 * Points 1-4 are checked here. Point 5 is checked as far as a machine can — every
 * `docs/…#anchor` written inside the `recommendedBecause` prose is resolved the
 * same way the `docs:` list is, so the gated copy and the prose copy of a
 * reference cannot drift apart when a heading is renamed — but whether the
 * recommendation is *right* is a human's call, which is what `confirmedBy`
 * records. A route with `confirmedBy: null` is served with its status attached
 * rather than silently.
 *
 * Point 6 is presence plus provenance. Age alone is not a failure — a route
 * checked two minors ago is old, not wrong — so a route genuinely behind the
 * surfaces is reported in the same status block as an unconfirmed one. Versions
 * are compared by their numeric head, so `2.4.0` and `2.4.0-SNAPSHOT` are one
 * line of development and the release tag does not flag the whole file. What
 * *is* failed is a version that cannot be ordered at all, or one ahead of the
 * surfaces: both look like provenance and are not.
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
import { anchorsIn, anchorRefsIn } from "./lib/anchors.mjs";
import { compareVersions, packVersionOf } from "./lib/pack-version.mjs";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(HERE, "..", "..", "..");
/**
 * The same root as the operating system reports it, so a citation resolved with
 * `realpathSync` is compared against a root that went through the same
 * resolution. Comparing a canonical file path against a root reached through a
 * symlink, a bind mount or a Windows `subst` drive made every citation in the
 * file look mis-cased.
 */
const REPO_REAL = fs.realpathSync.native(REPO_ROOT);
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

const anchorCache = new Map();

/** Anchors a page offers, read once per page. */
function anchorsInFile(file) {
  let anchors = anchorCache.get(file);
  if (!anchors) {
    anchors = anchorsIn(fs.readFileSync(file, "utf8"));
    anchorCache.set(file, anchors);
  }
  return anchors;
}


function loadSurfaces() {
  const types = new Map();
  // Sorted, so the pack version is not whichever file the filesystem happened to
  // yield first: an inconsistent pack would otherwise pass or fail by directory
  // iteration order, which differs between this machine and the runner.
  const surfaceDocs = [];
  const files = fs
    .readdirSync(API_DIR)
    .filter((f) => f.endsWith(".json") && f !== "excluded.json")
    .sort();
  for (const file of files) {
    const surface = path.basename(file, ".json");
    const doc = JSON.parse(fs.readFileSync(path.join(API_DIR, file), "utf8"));
    surfaceDocs.push(doc);
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
  return { types, packVersion: packVersionOf(surfaceDocs) };
}

const index = loadSurfaces();
const claims = JSON.parse(fs.readFileSync(CLAIMS_INDEX, "utf8")).claims;
const doc = JSON.parse(fs.readFileSync(TASKS_FILE, "utf8"));

const errors = [];
const unconfirmed = [];
const stale = [];
const fail = (task, message) => errors.push({ task, message });

/**
 * Resolve one `page.md#anchor`, wherever it was written. One implementation, so
 * a rule added to the anchor check cannot land in the `docs:` copy and miss the
 * prose copy — which is the shape of the drift this check exists to catch.
 */
function checkAnchorRef(id, ref, where) {
  const [rel, anchor] = ref.split("#");
  if (!rel) {
    fail(id, `${where} "${ref}" — names an anchor but no page`);
    return;
  }
  // Collapse "./" and "//" before anything compares this to a real path: those
  // are valid spellings of the same page, and comparing the raw string to
  // canonical output reported them as a casing mismatch when nothing was
  // mis-cased.
  const normalized = path.posix.normalize(rel);
  if (normalized.startsWith("../")) {
    fail(id, `${where} "${ref}" — points outside the repository`);
    return;
  }
  const file = path.join(REPO_REAL, ...normalized.split("/"));
  // statSync rather than existsSync: a reference whose path half is a directory
  // ("docs/recipes#zebra") exists, passes the anchor guard below because it does
  // carry a fragment, and then dies inside readFileSync with an EISDIR stack
  // trace naming no route at all.
  let stat;
  try {
    stat = fs.statSync(file);
  } catch {
    fail(id, `${where} "${ref}" — no such page`);
    return;
  }
  if (!stat.isFile()) {
    fail(id, `${where} "${ref}" — that is a directory, not a page`);
    return;
  }
  // existsSync is case-insensitive on Windows and case-sensitive on the runner,
  // so a mis-cased citation passes here and fails on CI with "no such page",
  // which reads as a missing file rather than as the casing it is. Both sides of
  // the comparison are realpath'd: resolving only the file compared a canonical
  // path against a repo root reached through a symlink or a Windows subst drive,
  // and reported every citation in the file as mis-cased.
  const real = fs.realpathSync.native(file);
  if (real !== REPO_REAL && !real.startsWith(REPO_REAL + path.sep)) {
    fail(id, `${where} "${ref}" — resolves outside the repository`);
    return;
  }
  const onDisk = path.relative(REPO_REAL, real).split(path.sep).join("/");
  if (onDisk !== normalized) {
    fail(id, `${where} "${ref}" — the page on disk is "${onDisk}"; the citation's casing does not match`);
    return;
  }
  if (!anchor) {
    fail(id, `${where} "${ref}" — needs an #anchor; a route hands over one section, not a whole page`);
    return;
  }
  if (!anchorsInFile(file).has(anchor)) {
    fail(id, `${where} "${ref}" — the page has no heading with that anchor (a heading was renamed)`);
  }
}

/**
 * Every prose field `api-query` prints verbatim. All of it is read by whoever
 * asks for the route, so an anchor in any of it has to resolve — checking only
 * `recommendedBecause` would leave a dangling link one field to the left.
 */
function servedProse(route) {
  const fields = [route.intent, route.recommendedBecause];
  for (const alt of route.alternatives ?? []) {
    if (typeof alt === "object" && alt) fields.push(alt.useWhen, alt.tradeoffs);
  }
  return fields;
}

// Fail closed. Without a pack version every route's age is unknowable, and a
// check that quietly skips itself reports exactly what a passing one reports.
if (!index.packVersion) {
  fail(
    "(pack)",
    "no verifiedAgainst on any surface in knowledge/api — the age of every route is unknowable, " +
      "so this check cannot run; regenerate the surfaces",
  );
}

const seen = new Set();
for (const route of doc.tasks) {
  const id = route.task;
  if (!id) {
    fail("(unnamed)", "a route has no task id");
    continue;
  }
  if (seen.has(id)) fail(id, "duplicate task id — an intent must resolve to one route");
  seen.add(id);

  // 6 — the version it was checked against, and where it sits against the pack.
  if (!route.verifiedAgainst) {
    fail(id, "no verifiedAgainst: a route with no version is a route nobody can re-check");
  } else if (index.packVersion) {
    // Only when there is something to order against; the pack-level failure
    // above already names the cause, and repeating it per route buries it.
    const order = compareVersions(route.verifiedAgainst, index.packVersion);
    if (order === null) {
      // A version nobody can order is worse than an old one: it looks checked
      // and cannot be compared, so it is a failure rather than a note.
      fail(
        id,
        `verifiedAgainst "${route.verifiedAgainst}" is not a version this can order against ` +
          `the pack's "${index.packVersion}" — a typo reads as provenance`,
      );
    } else if (order < 0) {
      // Not a failure: a route checked two minors ago is old, not wrong. But
      // served unmarked it carries the authority of one checked today, so it is
      // reported rather than passed over. Only a genuinely lower version counts
      // — 2.4.0 and 2.4.0-SNAPSHOT are the same line, so the release tag does
      // not flag the whole file.
      stale.push({ task: id, at: route.verifiedAgainst });
    } else if (order > 0) {
      fail(
        id,
        `verifiedAgainst "${route.verifiedAgainst}" is ahead of the pack's "${index.packVersion}" — ` +
          "a route cannot have been checked against surfaces that do not exist yet",
      );
    }
  }

  // 5 — the recommendation must be traceable, and the citation must be a real anchor.
  if (!route.recommended) fail(id, "no recommended: — a route that does not recommend is a list, not a route");
  if (!route.recommendedBecause) {
    fail(id, "no recommendedBecause: — the recommendation must trace to something in this repository");
  }
  // Prose is what an agent actually reads when it asks for the route, so every
  // anchor in it is held to the same check as `docs:` — otherwise renaming a
  // heading fixes the gated copy and leaves the quoted one dangling.
  for (const prose of servedProse(route)) {
    for (const ref of anchorRefsIn(prose)) checkAnchorRef(id, ref, "prose cites");
  }

  // 1 — every docs anchor resolves.
  for (const ref of route.docs ?? []) checkAnchorRef(id, ref, "docs");
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

  // `confirmedBy` is the one gate a machine cannot close, so the machine at
  // least refuses a value that only looks like one: whitespace, or anything
  // that is not a name, would read as confirmed while recording nobody.
  if (route.confirmedBy === undefined) {
    fail(id, "no confirmedBy field — use null for unconfirmed rather than omitting it");
  } else if (route.confirmedBy === null) {
    unconfirmed.push(id);
  } else if (typeof route.confirmedBy !== "string" || route.confirmedBy.trim() === "") {
    fail(id, `confirmedBy is ${JSON.stringify(route.confirmedBy)} — it must name a person, or be null`);
  }
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
if (stale.length) {
  process.stdout.write(
    `\n  ${stale.length} route(s) were last checked against an older pack than ${index.packVersion} —\n` +
      "  still served, but nothing has re-read them against the current surfaces:\n" +
      stale.map((s) => `    ${s.task} (${s.at})\n`).join(""),
  );
}
process.exit(0);
