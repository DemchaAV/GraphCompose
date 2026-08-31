#!/usr/bin/env node
/**
 * knowledge/tools/changeset/changeset.mjs — what this change invalidated.
 *
 *   node knowledge/tools/changeset/changeset.mjs --base origin/develop
 *   node knowledge/tools/changeset/changeset.mjs --base v2.2.2 --json
 *
 * Exit 0 always (this reports, it does not gate) · 2 usage · 4 git unusable.
 *
 * The question "which documentation does this change invalidate" is answered
 * today by reading the documentation. This answers it by intersection instead:
 * the surfaces say which symbols moved, the claims index says which pages assert
 * them, and only those pages come back.
 *
 * It costs one `git show` per surface and no build at all. That is a direct
 * payoff of committing the generated surfaces: both sides of the comparison are
 * already in git, so the API diff between any two refs is computable without
 * checking either of them out.
 *
 * Behavioural signals are **ranked, not pooled**:
 *
 *   proof  a changed layout snapshot or visual baseline. Geometry drift by
 *          construction — the file *is* the recorded output, so a diff in it is
 *          evidence, not a hint.
 *   hint   a changed example. Examples change for many reasons; worth naming,
 *          never worth treating as evidence.
 *
 * "A test file changed" is deliberately not a signal at all. Tests change for
 * stylistic reasons constantly, and a channel that fires on every refactor is
 * one people learn to ignore.
 *
 * The output is a **bounded** set, and the two ways out of it are explicit
 * rather than accidental: a file the pipeline cannot classify, and a
 * proof-strength change no claim covers. Each becomes a named file to look at.
 * The point is not that a reader may never open anything else — it is that
 * nothing is skipped silently.
 */

import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(HERE, "..", "..", "..");
const SURFACES = ["authoring", "templates", "backends", "testing", "extension-spi"];

function usage(code = 0) {
  process.stdout.write(
    "usage: node knowledge/tools/changeset/changeset.mjs --base <ref> [--head <ref>] [--json]\n\n" +
      "  --base <ref>   what to compare against (a branch, tag or commit)\n" +
      "  --head <ref>   the other side (default: the working tree)\n" +
      "  --json         machine-readable\n\n" +
      "exit: 0 reported | 2 usage | 4 git unusable\n",
  );
  process.exit(code);
}

const argv = process.argv.slice(2);
let base = null;
let head = null;
let asJson = false;
for (let i = 0; i < argv.length; i += 1) {
  const a = argv[i];
  if (a === "--help" || a === "-h") usage(0);
  else if (a === "--base") base = argv[++i];
  else if (a === "--head") head = argv[++i];
  else if (a === "--json") asJson = true;
  else usage(2);
}
if (!base) usage(2);

const git = (...args) => {
  const run = spawnSync("git", ["-C", REPO_ROOT, ...args], { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });
  return run.status === 0 ? run.stdout : null;
};

if (git("rev-parse", "--git-dir") === null) {
  process.stderr.write("[changeset] not a git repository, or git is unavailable\n");
  process.exit(4);
}
if (git("rev-parse", "--verify", `${base}^{commit}`) === null) {
  process.stderr.write(`[changeset] cannot resolve --base "${base}"\n`);
  process.exit(4);
}

// --- the API diff ------------------------------------------------------------

/** Every member of a surface, flattened to `Type.member(erased,params)`. */
function membersOf(json) {
  const out = new Map();
  if (!json) return out;
  for (const pkg of json.packages ?? []) {
    for (const type of pkg.types ?? []) {
      out.set(type.name, { kind: "type", stability: type.stability ?? "stable" });
      for (const member of type.members ?? []) {
        const params = (member.params ?? []).map((p) => p.type).join(",");
        const key = member.kind === "constant" ? `${type.name}.${member.name}` : `${type.name}.${member.name}(${params})`;
        out.set(key, {
          kind: member.kind,
          returns: member.returns ?? null,
          stability: member.stability ?? type.stability ?? "stable",
        });
      }
    }
  }
  return out;
}

function surfaceAt(ref, surface) {
  const text = ref === null
    ? (fs.existsSync(path.join(REPO_ROOT, "knowledge", "api", `${surface}.json`))
        ? fs.readFileSync(path.join(REPO_ROOT, "knowledge", "api", `${surface}.json`), "utf8")
        : null)
    : git("show", `${ref}:knowledge/api/${surface}.json`);
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

const api = { added: [], removed: [], changed: [] };
const surfacesMissingAtBase = [];

for (const surface of SURFACES) {
  const before = membersOf(surfaceAt(base, surface));
  const afterDoc = surfaceAt(head ?? null, surface);
  const after = membersOf(afterDoc);

  if (before.size === 0 && after.size > 0) surfacesMissingAtBase.push(surface);

  for (const [key, value] of after) {
    if (!before.has(key)) {
      api.added.push({ symbol: key, surface, stability: value.stability });
      continue;
    }
    const was = before.get(key);
    if (was.returns !== value.returns || was.stability !== value.stability) {
      api.changed.push({
        symbol: key,
        surface,
        ...(was.returns !== value.returns ? { returns: [was.returns, value.returns] } : {}),
        ...(was.stability !== value.stability ? { stability: [was.stability, value.stability] } : {}),
      });
    }
  }
  for (const key of before.keys()) {
    if (!after.has(key)) api.removed.push({ symbol: key, surface, stability: before.get(key).stability });
  }
}

// --- behavioural signals -----------------------------------------------------

const SIGNALS = [
  {
    strength: "proof",
    what: "layout snapshot",
    // The file IS the recorded geometry, so a diff in it is drift by
    // construction rather than an indication of it.
    test: (f) => /(^|\/)layout-snapshots\/.*\.json$/.test(f),
  },
  {
    strength: "proof",
    what: "visual baseline",
    test: (f) => /(^|\/)visual-baselines\//.test(f) || /^baselines\//.test(f),
  },
  {
    strength: "hint",
    what: "example",
    test: (f) => /^examples\/src\/main\//.test(f),
  },
];

const range = head ? `${base}...${head}` : base;
const nameStatus = git("diff", "--name-status", range) ?? "";
const changedFiles = nameStatus
  .split(/\r?\n/)
  .filter(Boolean)
  .map((line) => {
    const [status, ...rest] = line.split(/\t/);
    return { status: status[0], file: rest[rest.length - 1] };
  });

const behavior = [];
for (const { status, file } of changedFiles) {
  for (const signal of SIGNALS) {
    if (signal.test(file)) {
      behavior.push({ strength: signal.strength, what: signal.what, file, status });
      break;
    }
  }
}

// --- intersect with what the documentation claims ----------------------------

const readJson = (p) => (fs.existsSync(p) ? JSON.parse(fs.readFileSync(p, "utf8")) : null);
const claims = readJson(path.join(REPO_ROOT, "knowledge", "claims", "index.json"))?.claims ?? {
  symbol: {}, capability: {}, behavior: {},
};
const routes = readJson(path.join(REPO_ROOT, "knowledge", "routing", "tasks.json"))?.tasks ?? [];

/** `Type.member(params)` and `Type.member` both answer to a claim of `Type.member`. */
const claimKeysFor = (symbol) => {
  const bare = symbol.replace(/\(.*$/, "");
  return [symbol, bare].filter((k) => claims.symbol[k]);
};

const pages = new Map();
const noteAffected = (page, why) => {
  const entry = pages.get(page.page) ?? { page: page.page, heading: page.heading, why: [] };
  entry.why.push(why);
  pages.set(page.page, entry);
};

const movedSymbols = [
  ...api.removed.map((e) => ({ ...e, how: "removed" })),
  ...api.changed.map((e) => ({ ...e, how: "changed" })),
  ...api.added.map((e) => ({ ...e, how: "added" })),
];

for (const moved of movedSymbols) {
  for (const key of claimKeysFor(moved.symbol)) {
    for (const holder of claims.symbol[key]) {
      noteAffected(holder, `${moved.how}: ${moved.symbol}`);
    }
  }
}

const affectedTasks = [];
for (const route of routes) {
  const hits = (route.symbols ?? []).filter((s) =>
    movedSymbols.some((m) => m.symbol === s || m.symbol.startsWith(`${s}(`)),
  );
  if (hits.length) affectedTasks.push({ task: route.task, symbols: hits, docs: route.docs });
}

// --- escalations -------------------------------------------------------------

const escalations = [];

// A surface that did not exist at the base is not a diff, it is a new file the
// comparison cannot speak about. Reporting its whole contents as "added" would
// bury a real change under hundreds of lines.
for (const surface of surfacesMissingAtBase) {
  escalations.push({
    what: `surface "${surface}" does not exist at ${base}`,
    why: "nothing to compare against — read it rather than trusting the diff",
  });
}

// A recorded output moved and no page claims anything about it. Either the
// behaviour is undocumented, or it is documented without a claim; both are
// answered by opening the file rather than by assuming nothing broke.
for (const signal of behavior.filter((b) => b.strength === "proof")) {
  const covered = Object.values(claims.behavior).some((holders) =>
    holders.some((h) => h.proof && signal.file.includes(path.basename(h.proof.split(":")[1] ?? "", ".java"))),
  );
  if (!covered) {
    escalations.push({
      what: `${signal.what} changed: ${signal.file}`,
      why: "recorded output moved and no claim covers it — targeted review of this file",
    });
  }
}

// --- report ------------------------------------------------------------------

const changeset = {
  schemaVersion: 1,
  base,
  head: head ?? "(working tree)",
  api: {
    added: api.added.sort((a, b) => a.symbol.localeCompare(b.symbol)),
    removed: api.removed.sort((a, b) => a.symbol.localeCompare(b.symbol)),
    changed: api.changed.sort((a, b) => a.symbol.localeCompare(b.symbol)),
  },
  behavior,
  affectedPages: [...pages.values()].sort((a, b) => a.page.localeCompare(b.page)),
  affectedTasks,
  escalations,
};

const outFile = path.join(REPO_ROOT, "target", "knowledge", "changeset.json");
fs.mkdirSync(path.dirname(outFile), { recursive: true });
fs.writeFileSync(outFile, `${JSON.stringify(changeset, null, 2)}\n`, "utf8");

if (asJson) {
  process.stdout.write(`${JSON.stringify(changeset, null, 2)}\n`);
  process.exit(0);
}

const out = [];
out.push(`changeset  ${base} -> ${changeset.head}`);
out.push("");

const total = api.added.length + api.removed.length + api.changed.length;
if (total === 0) {
  out.push("  API: unchanged");
} else {
  out.push(`  API: +${api.added.length} -${api.removed.length} ~${api.changed.length}`);
  for (const e of api.removed.slice(0, 10)) out.push(`    - ${e.symbol}  (${e.surface})`);
  for (const e of api.changed.slice(0, 10)) {
    const how = e.returns ? `${e.returns[0]} -> ${e.returns[1]}` : `${e.stability[0]} -> ${e.stability[1]}`;
    out.push(`    ~ ${e.symbol}  ${how}`);
  }
  for (const e of api.added.slice(0, 10)) out.push(`    + ${e.symbol}  (${e.surface})`);
  if (total > 30) out.push(`    … ${total - 30} more, see target/knowledge/changeset.json`);
}

if (behavior.length) {
  out.push("");
  out.push("  behaviour:");
  for (const b of behavior.slice(0, 12)) out.push(`    ${b.strength.padEnd(5)} ${b.what}: ${b.file}`);
}

out.push("");
if (changeset.affectedPages.length) {
  out.push(`  pages to review (${changeset.affectedPages.length}):`);
  for (const p of changeset.affectedPages) {
    out.push(`    ${p.page}${p.heading ? `  (${p.heading})` : ""}`);
    for (const why of [...new Set(p.why)]) out.push(`      ${why}`);
  }
} else {
  out.push("  pages to review: none — no changed symbol is claimed by any page");
}

if (affectedTasks.length) {
  out.push("");
  out.push("  routes to re-check:");
  for (const t of affectedTasks) out.push(`    ${t.task}  (${t.symbols.join(", ")})`);
}

if (escalations.length) {
  out.push("");
  out.push(`  escalations (${escalations.length}) — the pipeline could not bound these:`);
  for (const e of escalations) {
    out.push(`    ${e.what}`);
    out.push(`      ${e.why}`);
  }
}

out.push("");
out.push("  target/knowledge/changeset.json");
process.stdout.write(`${out.join("\n")}\n`);
