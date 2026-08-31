#!/usr/bin/env node
/**
 * knowledge/tools/claims/check-claims.mjs — hold the documentation to what it
 * claims, and build the reverse index.
 *
 *   node knowledge/tools/claims/check-claims.mjs
 *   node knowledge/tools/claims/check-claims.mjs --check
 *
 * Exit 0 clean · 1 a claim is false, or --check found the index stale · 2 usage.
 *
 * The point is not to police prose. It is to make "which pages does this API
 * change invalidate" a set intersection instead of a search: a symbol lands in
 * the changeset, the index says which pages assert it, and only those pages need
 * looking at. Without it that question is answered by reading everything, which
 * is what the whole pack exists to stop.
 *
 * Enforcement is deliberately asymmetric:
 *
 *   a claimed symbol that does not exist    → ERROR. The page is lying, and it
 *                                             is lying about something a reader
 *                                             will try to compile.
 *   a symbol used on a page but not claimed → suggestion, never a failure.
 *
 * Symmetric enforcement was considered and rejected: it would demand a claim for
 * every incidental type named in every example, which is churn with no
 * correctness gain, and the predictable result is that people stop reading the
 * output. A missing claim costs coverage; a false claim costs trust.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { parseClaims, resolveSymbol } from "./lib/claims.mjs";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(HERE, "..", "..", "..");
const API_DIR = path.join(REPO_ROOT, "knowledge", "api");
const DOCS_ROOT = path.join(REPO_ROOT, "docs");
const INDEX_FILE = path.join(REPO_ROOT, "knowledge", "claims", "index.json");
const PROOFS_FILE = path.join(REPO_ROOT, "knowledge", "proofs", "index.json");

function usage(code = 0) {
  process.stdout.write(
    "usage: node knowledge/tools/claims/check-claims.mjs [--check] [--json]\n\n" +
      "  --check   verify the committed index matches, instead of writing it\n" +
      "  --json    machine-readable report\n\n" +
      "exit: 0 clean | 1 a claim is false or the index is stale | 2 usage\n",
  );
  process.exit(code);
}

/** Types and members from every surface, flattened for lookup. */
function loadSurfaces() {
  if (!fs.existsSync(API_DIR)) {
    process.stderr.write(
      "[check-claims] no surfaces at knowledge/api — generate them first:\n" +
        "  node knowledge/tools/api-surface/extract-api.mjs --from-reactor\n",
    );
    process.exit(1);
  }
  const types = new Map();
  for (const file of fs.readdirSync(API_DIR).filter((f) => f.endsWith(".json") && f !== "excluded.json")) {
    const surface = path.basename(file, ".json");
    const doc = JSON.parse(fs.readFileSync(path.join(API_DIR, file), "utf8"));
    for (const pkg of doc.packages ?? []) {
      for (const type of pkg.types ?? []) {
        types.set(type.name, {
          name: type.name,
          binaryName: type.binaryName,
          surface,
          stability: type.stability ?? "stable",
          methods: (type.members ?? []).filter((m) => m.kind !== "constant"),
          constants: (type.members ?? []).filter((m) => m.kind === "constant").map((m) => m.name),
        });
      }
    }
  }
  return { types };
}

/**
 * Proof targets that can be resolved today.
 *
 * `test:` names a JUnit class and `snippet:` names a `doc-example` id — both
 * already exist in this repository, so both are checkable, and a proof pointing
 * at a renamed test is exactly the rot this mechanism is for. `probe:` and
 * `render:` are accepted but not yet resolved: their registries are built later
 * in this phase, and refusing them now would only push authors towards the two
 * kinds that happen to be finished.
 */
function loadProofTargets() {
  const tests = new Map();
  const walkTests = (dir) => {
    let entries;
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch {
      return;
    }
    for (const entry of entries) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (entry.name === "target" || entry.name === ".git" || entry.name === ".claude") continue;
        walkTests(full);
      } else if (entry.name.endsWith(".java") && full.includes(`${path.sep}src${path.sep}test${path.sep}`)) {
        // Path as well as name: the registry has to say *where* a proof lives,
        // or a consumer holding the bundle can read that a claim is proven and
        // still have no way to look at the thing proving it.
        tests.set(
          entry.name.slice(0, -".java".length),
          path.relative(REPO_ROOT, full).split(path.sep).join("/"),
        );
      }
    }
  };
  walkTests(REPO_ROOT);

  const snippets = new Map();
  const markerRe = /^<!--\s*doc-example:\s*(.+?)\s*-->\s*$/;
  for (const file of docPages()) {
    for (const line of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
      const m = line.match(markerRe);
      if (!m) continue;
      const id = m[1].split(/\s+/).find((t) => t.startsWith("id="));
      if (id) snippets.set(id.slice("id=".length), path.relative(REPO_ROOT, file).split(path.sep).join("/"));
    }
  }

  return { tests, snippets };
}

/** Every tracked public Markdown page. `docs/private/` is gitignored. */
function docPages() {
  const out = [];
  const walk = (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (entry.name === "private") continue;
        walk(full);
      } else if (entry.name.endsWith(".md")) {
        out.push(full);
      }
    }
  };
  walk(DOCS_ROOT);
  return out;
}

/**
 * Symbols a page's Java fences actually call, as candidates for a claim it does
 * not yet make.
 *
 * Read from the fence text rather than from compiled bytecode. Bytecode would be
 * exact, but it is only reachable after `DocumentationSnippetCompileTest` has
 * run, which would make a documentation check depend on a Maven build — and this
 * side of the mechanism is a *suggestion*, where approximation is the design
 * rather than a compromise.
 *
 * The noise a regex would otherwise produce is removed by the surfaces
 * themselves: a match that does not resolve to a real type and member is
 * dropped, so what survives is something the reader can genuinely call. What
 * this cannot see is the whole reason claims are hand-authored — prose
 * assertions, alternatives discussed but not called, limits, backend caveats,
 * and behaviour spread across several calls. It proposes; it never decides.
 */
function usedSymbols(text) {
  const found = new Set();
  const fenceRe = /^```java\s*$/;
  const lines = text.split(/\r?\n/);
  let inFence = false;

  for (const line of lines) {
    if (line.trimEnd() === "```" && inFence) {
      inFence = false;
      continue;
    }
    if (fenceRe.test(line.trim())) {
      inFence = true;
      continue;
    }
    if (!inFence) continue;
    for (const [, symbol] of line.matchAll(/\b([A-Z][A-Za-z0-9]*(?:\.[A-Z][A-Za-z0-9]*)*\.[a-z][A-Za-z0-9]*)\s*\(/g)) {
      found.add(symbol);
    }
  }
  return found;
}

const args = process.argv.slice(2);
if (args.some((a) => a === "--help" || a === "-h")) usage(0);
if (args.some((a) => !["--check", "--json"].includes(a))) usage(2);
const checkOnly = args.includes("--check");
const asJson = args.includes("--json");

const index = loadSurfaces();
const proofTargets = loadProofTargets();

const errors = [];
const allClaims = [];
const suggestions = [];

for (const file of docPages()) {
  const relative = path.relative(REPO_ROOT, file).split(path.sep).join("/");
  const text = fs.readFileSync(file, "utf8");
  const { claims, errors: parseErrors } = parseClaims(text, relative);
  errors.push(...parseErrors);

  // Only pages that already claim something are asked to claim more. Proposing
  // claims for all 80 pages at once would bury the ones that matter, and a page
  // with no claims at all is a coverage decision rather than an oversight.
  if (claims.length) {
    const claimed = new Set(claims.filter((c) => c.kind === "symbol").map((c) => c.value));
    for (const symbol of usedSymbols(text)) {
      if (claimed.has(symbol)) continue;
      if (!resolveSymbol(index, symbol).found) continue;
      suggestions.push({ page: relative, symbol });
    }
  }

  for (const claim of claims) {
    if (claim.proof) {
      const [scheme, id] = [claim.proof.slice(0, claim.proof.indexOf(":")), claim.proof.slice(claim.proof.indexOf(":") + 1)];
      if (scheme === "test" && !proofTargets.tests.has(id)) {
        errors.push({
          file: claim.file,
          line: claim.line,
          heading: claim.heading,
          message: `proof "${claim.proof}" names no test class — it was renamed or removed, so the claim is unheld.`,
        });
        continue;
      }
      if (scheme === "snippet" && !proofTargets.snippets.has(id)) {
        errors.push({
          file: claim.file,
          line: claim.line,
          heading: claim.heading,
          message: `proof "${claim.proof}" names no doc-example id — no compiled snippet holds this claim.`,
        });
        continue;
      }
    }

    if (claim.kind === "symbol") {
      const resolved = resolveSymbol(index, claim.value);
      if (!resolved.found) {
        errors.push({
          file: claim.file,
          line: claim.line,
          heading: claim.heading,
          message:
            `claims symbol "${claim.value}" — ${resolved.reason}. ` +
            "It is not in any surface, so the page describes API that does not exist.",
        });
        continue;
      }
      allClaims.push({
        ...claim,
        surface: resolved.type.surface,
        stability: resolved.type.stability,
        binaryName: resolved.type.binaryName,
      });
      continue;
    }
    allClaims.push(claim);
  }
}

// --- the reverse index -------------------------------------------------------

const byKind = { symbol: {}, capability: {}, behavior: {} };
for (const claim of allClaims) {
  const bucket = byKind[claim.kind];
  (bucket[claim.value] ??= []).push({
    page: claim.file,
    line: claim.line,
    heading: claim.heading,
    ...(claim.proof ? { proof: claim.proof } : {}),
    ...(claim.surface ? { surface: claim.surface, stability: claim.stability } : {}),
  });
}
for (const bucket of Object.values(byKind)) {
  for (const key of Object.keys(bucket)) {
    bucket[key].sort((a, b) => a.page.localeCompare(b.page) || a.line - b.line);
  }
}

const document = {
  schemaVersion: 1,
  note:
    "Generated from claim markers in docs/. Given a symbol, capability or " +
    "behaviour, this says which pages assert it — so an API change resolves to " +
    "a page set by intersection rather than by reading everything.",
  generator: "knowledge/tools/claims/check-claims.mjs",
  counts: {
    pagesWithClaims: new Set(allClaims.map((c) => c.file)).size,
    symbol: Object.keys(byKind.symbol).length,
    capability: Object.keys(byKind.capability).length,
    behavior: Object.keys(byKind.behavior).length,
  },
  claims: {
    symbol: Object.fromEntries(Object.entries(byKind.symbol).sort()),
    capability: Object.fromEntries(Object.entries(byKind.capability).sort()),
    behavior: Object.fromEntries(Object.entries(byKind.behavior).sort()),
  },
};

/**
 * What backs the claims, and where it lives.
 *
 * The claims index says a page asserts something and names its proof; on its own
 * that is a promissory note. This resolves the note: for every proof a claim
 * cites, the kind, the file, and everything it holds up.
 *
 * Derived rather than authored, so it cannot drift: a claim whose proof does not
 * resolve has already failed above, and a proof nothing cites never appears. It
 * is deliberately not a list of every test in the repository — that would be a
 * directory listing, and the question this answers is "what is holding this
 * claim up", not "what tests exist".
 *
 * `probe:` collapsed into `test:` on contact with reality. The plan had probes
 * as a separate harness, but a contract probe *is* a JUnit test — it runs in the
 * reactor gate and needs no harness of its own, so a second scheme would have
 * been two names for one mechanism.
 */
const proofs = {};
for (const claim of allClaims) {
  if (!claim.proof) continue;
  const scheme = claim.proof.slice(0, claim.proof.indexOf(":"));
  const id = claim.proof.slice(claim.proof.indexOf(":") + 1);
  const entry = (proofs[claim.proof] ??= {
    kind: scheme,
    ...(scheme === "test" ? { path: proofTargets.tests.get(id) ?? null } : {}),
    ...(scheme === "snippet" ? { page: proofTargets.snippets.get(id) ?? null } : {}),
    ...(scheme === "probe" || scheme === "render" ? { unresolved: "no registry for this scheme yet" } : {}),
    holds: [],
  });
  entry.holds.push({
    kind: claim.kind,
    value: claim.value,
    page: claim.file,
    line: claim.line,
    ...(claim.heading ? { heading: claim.heading } : {}),
  });
}
for (const entry of Object.values(proofs)) {
  entry.holds.sort((a, b) => a.page.localeCompare(b.page) || a.line - b.line);
}

// A claim that asserts behaviour and cites nothing cannot appear here at all —
// the parser refuses it — so the gap this counts is the opposite one: pages
// making symbol or capability claims with no proof attached. That is coverage
// to grow, not an error, so it is reported as a number rather than a failure.
const unproven = allClaims.filter((c) => !c.proof).length;

const proofsDocument = {
  schemaVersion: 1,
  note:
    "Every proof a documentation claim cites, resolved to where it lives and " +
    "what it holds up. Generated from the claim markers in docs/; a proof that " +
    "nothing cites does not appear, and a claim whose proof does not resolve " +
    "fails the check rather than landing here.",
  generator: "knowledge/tools/claims/check-claims.mjs",
  counts: {
    proofs: Object.keys(proofs).length,
    claimsHeld: allClaims.filter((c) => c.proof).length,
    claimsWithoutProof: unproven,
  },
  proofs: Object.fromEntries(Object.entries(proofs).sort()),
};

const proofsText = `${JSON.stringify(proofsDocument, null, 2)}\n`;

const text = `${JSON.stringify(document, null, 2)}\n`;

if (errors.length) {
  if (asJson) {
    process.stdout.write(`${JSON.stringify({ errors, counts: document.counts }, null, 2)}\n`);
  } else {
    process.stdout.write(`[check-claims] ${errors.length} false claim(s):\n\n`);
    for (const e of errors) {
      process.stdout.write(`  ${e.file}:${e.line}${e.heading ? `  (${e.heading})` : ""}\n    ${e.message}\n\n`);
    }
    process.stdout.write(
      "  A claim is a promise the page makes to a reader. Fix the page, or fix\n" +
        "  the claim — do not delete the marker to silence this.\n",
    );
  }
  process.exit(1);
}

if (checkOnly) {
  const stale = [
    [INDEX_FILE, text],
    [PROOFS_FILE, proofsText],
  ].filter(([file, expected]) => {
    const actual = fs.existsSync(file) ? fs.readFileSync(file, "utf8").replace(/\r\n/g, "\n") : null;
    return actual !== expected;
  });
  if (stale.length) {
    process.stdout.write(
      `[check-claims] out of date: ${stale.map(([f]) => path.relative(REPO_ROOT, f).split(path.sep).join("/")).join(", ")}\n` +
        "  regenerate: node knowledge/tools/claims/check-claims.mjs\n",
    );
    process.exit(1);
  }
  process.stdout.write(
    `[check-claims] ${allClaims.length} claims, ${proofsDocument.counts.proofs} proofs, both current\n`,
  );
  process.exit(0);
}

fs.mkdirSync(path.dirname(INDEX_FILE), { recursive: true });
fs.writeFileSync(INDEX_FILE, text, "utf8");
fs.mkdirSync(path.dirname(PROOFS_FILE), { recursive: true });
fs.writeFileSync(PROOFS_FILE, proofsText, "utf8");

if (asJson) {
  process.stdout.write(`${JSON.stringify(document.counts, null, 2)}\n`);
} else {
  process.stdout.write(
    `[check-claims] ${allClaims.length} claims on ${document.counts.pagesWithClaims} pages ` +
      `-> knowledge/claims/index.json\n` +
      `  ${proofsDocument.counts.proofs} proofs holding ${proofsDocument.counts.claimsHeld} of them ` +
      `-> knowledge/proofs/index.json\n` +
      `  symbol ${document.counts.symbol} · capability ${document.counts.capability} · ` +
      `behavior ${document.counts.behavior}\n`,
  );
  if (suggestions.length) {
    process.stdout.write(
      `\n  ${suggestions.length} symbol(s) called by a claiming page but not claimed.\n` +
        "  Not a failure — a claim is a deliberate promise, not a transcript of\n" +
        "  every call an example happens to make. Add the ones the page is really\n" +
        "  about:\n",
    );
    for (const s of suggestions.slice(0, 20)) {
      process.stdout.write(`    ${s.page}  <!-- claim: symbol=${s.symbol} -->\n`);
    }
    if (suggestions.length > 20) {
      process.stdout.write(`    … and ${suggestions.length - 20} more\n`);
    }
  }
}
