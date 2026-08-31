#!/usr/bin/env node
/**
 * knowledge/tools/api-query/api-query.mjs — ask the surfaces a question
 * instead of reading them.
 *
 *   node knowledge/tools/api-query/api-query.mjs --type ShapeContainerBuilder
 *   node knowledge/tools/api-query/api-query.mjs --type TableBuilder --method column
 *   node knowledge/tools/api-query/api-query.mjs --exists GraphCompose.DocumentBuilder.pageSize
 *   node knowledge/tools/api-query/api-query.mjs --search timeline
 *   node knowledge/tools/api-query/api-query.mjs --constant CENTER_LEFT
 *   node knowledge/tools/api-query/api-query.mjs --surface authoring --package com.demcha.compose.document.dsl
 *
 * The surfaces under `knowledge/api/` are the closed set an author writes
 * against. They are large; the answer to "does this method exist, and what is
 * its signature" is a few lines of JSON. This gives that, so a lookup costs one
 * call rather than a series of greps.
 *
 * `knowledge/api/<surface>.json` is the source — what
 * `knowledge/tools/api-surface/extract-api.mjs` writes from the class files.
 * The Markdown views are rendered from the same JSON, so this reads the
 * structured form rather than re-parsing prose and the two cannot disagree.
 *
 * Adapted from the AI Flow original (`scripts/api-query.mjs`). Dropped with
 * intent: the `--version` / `--project-dir` pack-pinning machinery, which
 * belongs to a consumer holding several GraphCompose lines at once — this repo
 * describes exactly one, the tree it is in. Dropped with it: the Markdown
 * fallback parser, which exists there for packs that predate the extractor.
 * Added: `--surface`, because here the API is split per surface rather than
 * per released version.
 *
 * Exit codes: 0 found · 3 nothing matched (so a caller can branch) · 2 usage.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(HERE, "..", "..", "..");
const API_DIR = path.join(REPO_ROOT, "knowledge", "api");
const TASKS_FILE = path.join(REPO_ROOT, "knowledge", "routing", "tasks.json");

function usage(code = 0) {
  process.stdout.write(
    "usage: node knowledge/tools/api-query/api-query.mjs [--surface <name>] <query>\n\n" +
      "  --type <Type>          everything the surfaces have for a type\n" +
      "  --method <name>        filter to methods whose name matches\n" +
      "  --exists <Type.method> a yes/no answer with the signatures, if any\n" +
      "  --search <term>        types, methods and constants matching a term\n" +
      "  --constant <NAME>      which types declare a constant\n" +
      "  --package <pkg>        the types in a package\n" +
      "  --task <id>            how to do a thing: the path, the alternatives,\n" +
      "                         the constraints, and the one anchor to open\n" +
      "  --tasks                every intent the routing table answers\n" +
      "  --dump                 every surface as one JSON document, on stdout\n\n" +
      "  --surface <name>       restrict to one surface (authoring, templates,\n" +
      "                         backends, testing, extension-spi). Default: all\n" +
      "  --json                 machine-readable (default for --dump)\n\n" +
      "exit: 0 found | 3 nothing matched | 2 usage\n",
  );
  process.exit(code);
}

function parseArgs(argv) {
  const out = {
    surface: null, type: null, method: null, exists: null,
    search: null, constant: null, package: null, dump: false, json: false,
    task: null, tasks: false,
  };
  for (let i = 0; i < argv.length; i += 1) {
    const a = argv[i];
    if (a === "--help" || a === "-h") usage(0);
    else if (a === "--dump") out.dump = true;
    else if (a === "--json") out.json = true;
    else if (a === "--surface") out.surface = argv[++i];
    else if (a === "--type") out.type = argv[++i];
    else if (a === "--method") out.method = argv[++i];
    else if (a === "--exists") out.exists = argv[++i];
    else if (a === "--search") out.search = argv[++i];
    else if (a === "--constant") out.constant = argv[++i];
    else if (a === "--package") out.package = argv[++i];
    else if (a === "--task") out.task = argv[++i];
    else if (a === "--tasks") out.tasks = true;
    else usage(2);
  }
  return out;
}

// ------------------------------------------------------------------ routing ---

/**
 * Answer an intent instead of a symbol.
 *
 * Surfaces say what exists; they cannot say which of three ways is the right
 * one, which is where wrong-API choices actually come from — a skills list in
 * two columns is a row with weights, and nothing in a signature says so.
 *
 * What comes back is deliberately not the guide. It is the decision plus one
 * anchor: restating the prose here would make this a fourth copy of the
 * documentation, which is the outcome keeping prose in `docs/` exists to avoid.
 */
function answerTask(id) {
  if (!fs.existsSync(TASKS_FILE)) {
    process.stderr.write("[api-query] no routing table at knowledge/routing/tasks.json\n");
    process.exit(1);
  }
  const doc = JSON.parse(fs.readFileSync(TASKS_FILE, "utf8"));
  const route = doc.tasks.find((t) => t.task === id);
  if (!route) {
    const near = doc.tasks
      .map((t) => t.task)
      .filter((t) => t.includes(id) || id.includes(t.split(".").pop()));
    return { found: false, query: { task: id }, didYouMean: near, available: doc.tasks.map((t) => t.task) };
  }
  return { found: true, query: { task: id }, ...route };
}

function renderTask(answer) {
  if (!answer.found) {
    const out = [`No route for "${answer.query.task}".`];
    if (answer.didYouMean.length) out.push(`did you mean: ${answer.didYouMean.join(", ")}`);
    out.push(`known intents: ${answer.available.join(", ")}`);
    return out.join("\n");
  }

  const out = [];
  out.push(`${answer.task} — ${answer.intent}`);
  out.push("");
  out.push(`  use: ${answer.recommended}`);
  out.push(`       ${answer.recommendedBecause}`);

  if (answer.alternatives?.length) {
    out.push("");
    out.push("  instead, when:");
    for (const alt of answer.alternatives) {
      out.push(`    ${alt.id}`);
      out.push(`      when:  ${alt.useWhen}`);
      out.push(`      costs: ${alt.tradeoffs}`);
    }
  }
  if (answer.constraints?.length) {
    out.push("");
    out.push("  constraints:");
    for (const c of answer.constraints) out.push(`    ${c}`);
  }
  if (answer.symbols?.length) {
    out.push("");
    out.push(`  symbols: ${answer.symbols.join(", ")}`);
  }
  if (answer.docs?.length) {
    out.push("");
    out.push(`  read: ${answer.docs.join("  ")}`);
  }
  if (!answer.confirmedBy) {
    out.push("");
    out.push("  (recommendation not yet confirmed by a maintainer)");
  }
  return out.join("\n");
}

// ------------------------------------------------------------------ loading ---

/**
 * Every surface JSON, flattened into one type list.
 *
 * `surface` and `stability` ride on each type because "does it exist" and "is
 * it something I should be calling yet" are the same question asked twice, and
 * an answer that omits the second one reads as a green light.
 */
function loadSurfaces(only) {
  if (!fs.existsSync(API_DIR)) {
    process.stderr.write(
      `[api-query] no surfaces at ${path.relative(REPO_ROOT, API_DIR)}\n` +
        "  Generate them first:\n" +
        "    node knowledge/tools/api-surface/extract-api.mjs --from-reactor\n",
    );
    process.exit(1);
  }

  const files = fs
    .readdirSync(API_DIR)
    .filter((f) => f.endsWith(".json") && f !== "excluded.json")
    .filter((f) => !only || path.basename(f, ".json") === only)
    .sort();

  if (files.length === 0) {
    const available = fs
      .readdirSync(API_DIR)
      .filter((f) => f.endsWith(".json") && f !== "excluded.json")
      .map((f) => path.basename(f, ".json"));
    process.stderr.write(
      only
        ? `[api-query] no surface "${only}". Available: ${available.join(", ") || "(none)"}\n`
        : `[api-query] no surface JSON under ${path.relative(REPO_ROOT, API_DIR)}\n`,
    );
    process.exit(1);
  }

  const types = [];
  const surfaces = [];
  let verifiedAgainst = null;

  for (const file of files) {
    const surface = path.basename(file, ".json");
    surfaces.push(surface);
    const doc = JSON.parse(fs.readFileSync(path.join(API_DIR, file), "utf8"));
    verifiedAgainst ??= doc.verifiedAgainst ?? doc.targetVersion ?? null;

    for (const pkg of doc.packages ?? []) {
      for (const type of pkg.types ?? []) {
        const methods = [];
        const constants = [];
        for (const member of type.members ?? []) {
          if (member.kind === "constant") {
            constants.push(member.name);
            continue;
          }
          const params = (member.params ?? []).map((p) => (p.name ? `${p.type} ${p.name}` : p.type));
          const head =
            member.kind === "constructor"
              ? `new ${member.name}`
              : `${member.typeParameters ? `${member.typeParameters} ` : ""}` +
                `${member.returns ? `${member.returns} ` : ""}${member.name}`;
          methods.push({
            signature: `${head}(${params.join(", ")})`,
            name: member.name,
            returns: member.returns ?? null,
            parameters: params,
            origin: member.origin,
            static: member.static,
            stability: member.stability ?? null,
          });
        }
        types.push({
          name: type.name,
          binaryName: type.binaryName ?? null,
          kind: type.kind,
          package: pkg.name,
          surface,
          stability: type.stability ?? "stable",
          methods,
          constants,
        });
      }
    }
  }

  return {
    surfaces,
    verifiedAgainst,
    typeCount: types.length,
    methodCount: types.reduce((n, t) => n + t.methods.length, 0),
    constantCount: types.reduce((n, t) => n + t.constants.length, 0),
    types,
  };
}

// ---------------------------------------------------------------- answering ---

/** Exact wins; substring is the fallback so a half-remembered name still lands. */
function matches(name, wanted) {
  if (!name) return false;
  return name === wanted || name.toLowerCase().includes(wanted.toLowerCase());
}

function query(index, options) {
  const base = {
    surfaces: index.surfaces,
    verifiedAgainst: index.verifiedAgainst,
  };

  if (options.exists) {
    // A fully-qualified name is accepted as well as Type.method. Splitting on
    // the FIRST dot would turn "com.demcha.compose.document.svg.SvgPath.of"
    // into type "com" and answer "no type com — it does not exist": a
    // confident, authoritative, wrong negative, from the one tool whose whole
    // value is that its "no" can be trusted.
    const parts = options.exists.split(".").filter(Boolean);
    const methodName = parts.length > 1 ? parts[parts.length - 1] : null;
    let typeName = parts.length > 1 ? parts[parts.length - 2] : null;
    if (!typeName || !methodName) {
      process.stderr.write("[api-query] --exists takes Type.method or a fully-qualified name\n");
      process.exit(2);
    }
    // A nested receiver is spelled Outer.Inner.member; prefer the nested type
    // when one exists, so `GraphCompose.DocumentBuilder.pageSize` resolves to
    // DocumentBuilder rather than to whatever else is called that.
    const nested = parts.length > 2 ? `${parts[parts.length - 3]}.${typeName}` : null;
    const type =
      (nested && index.types.find((t) => t.name === nested)) ||
      index.types.find((t) => t.name === typeName) ||
      null;
    if (type) typeName = type.name;

    const overloads = (type?.methods ?? []).filter((m) => m.name === methodName);
    const constant = (type?.constants ?? []).includes(methodName);
    return {
      ...base,
      query: { exists: options.exists },
      found: Boolean(type) && (overloads.length > 0 || constant),
      type: type
        ? { name: type.name, kind: type.kind, package: type.package, surface: type.surface, stability: type.stability }
        : null,
      overloads: overloads.map((m) => (m.stability ? `${m.signature}   [${m.stability}]` : m.signature)),
      isConstant: constant,
      note: type
        ? undefined
        : `No type "${typeName}" in the surfaces — it is not public API for this tree.`,
    };
  }

  if (options.constant) {
    const hits = index.types
      .filter((t) => t.constants.includes(options.constant))
      .map((t) => ({ type: t.name, package: t.package, kind: t.kind, surface: t.surface }));
    return { ...base, query: { constant: options.constant }, found: hits.length > 0, declaredBy: hits };
  }

  if (options.package) {
    const hits = index.types
      .filter((t) => t.package === options.package)
      .map((t) => ({ name: t.name, kind: t.kind, methods: t.methods.length, surface: t.surface, stability: t.stability }));
    return { ...base, query: { package: options.package }, found: hits.length > 0, types: hits };
  }

  if (options.type) {
    const type =
      index.types.find((t) => t.name === options.type) ??
      index.types.find((t) => t.name.toLowerCase() === options.type.toLowerCase());
    if (!type) {
      const near = index.types
        .filter((t) => t.name.toLowerCase().includes(options.type.toLowerCase()))
        .map((t) => t.name)
        .slice(0, 8);
      return {
        ...base,
        query: { type: options.type },
        found: false,
        note: `No type "${options.type}" in the surfaces.`,
        didYouMean: near,
      };
    }
    const methods = options.method
      ? type.methods.filter((m) => matches(m.name, options.method))
      : type.methods;
    return {
      ...base,
      query: { type: options.type, method: options.method ?? undefined },
      found: methods.length > 0 || type.constants.length > 0,
      type: {
        name: type.name,
        binaryName: type.binaryName,
        kind: type.kind,
        package: type.package,
        surface: type.surface,
        stability: type.stability,
      },
      methods: methods.map((m) => (m.stability ? `${m.signature}   [${m.stability}]` : m.signature)),
      constants: type.constants,
    };
  }

  if (options.method) {
    const hits = [];
    for (const type of index.types) {
      for (const method of type.methods) {
        if (matches(method.name, options.method)) {
          hits.push({ type: type.name, surface: type.surface, signature: method.signature });
        }
      }
    }
    return {
      ...base,
      query: { method: options.method },
      found: hits.length > 0,
      matches: hits.slice(0, 60),
      total: hits.length,
    };
  }

  // --search: one term across everything, for when the type name is unknown.
  const term = options.search.toLowerCase();
  const types = index.types.filter((t) => t.name.toLowerCase().includes(term));
  const methods = [];
  const constants = [];
  for (const type of index.types) {
    for (const method of type.methods) {
      if (method.name && method.name.toLowerCase().includes(term)) {
        methods.push({ type: type.name, surface: type.surface, signature: method.signature });
      }
    }
    for (const constant of type.constants) {
      if (constant.toLowerCase().includes(term)) constants.push({ type: type.name, constant });
    }
  }
  return {
    ...base,
    query: { search: options.search },
    found: types.length > 0 || methods.length > 0 || constants.length > 0,
    types: types
      .map((t) => ({ name: t.name, kind: t.kind, package: t.package, surface: t.surface, methods: t.methods.length }))
      .slice(0, 30),
    methods: methods.slice(0, 40),
    constants: constants.slice(0, 40),
    total: { types: types.length, methods: methods.length, constants: constants.length },
  };
}

// ------------------------------------------------------------------ output ---

function render(answer) {
  const out = [];
  const beta = (s) => (s && s !== "stable" ? `  [${s}]` : "");

  if (answer.note) out.push(answer.note);

  if (answer.type) {
    out.push(`${answer.type.kind} ${answer.type.binaryName ?? answer.type.name}${beta(answer.type.stability)}`);
    out.push(`  surface: ${answer.type.surface}`);
  }
  if (answer.overloads?.length) {
    out.push("  overloads:");
    for (const o of answer.overloads) out.push(`    ${o}`);
  }
  if (answer.isConstant) out.push("  (declared as a constant)");
  if (answer.methods?.length) {
    out.push(`  methods (${answer.methods.length}):`);
    for (const m of answer.methods) {
      out.push(typeof m === "string" ? `    ${m}` : `    ${m.type}.${m.signature}   (${m.surface})`);
    }
  }
  if (answer.constants?.length) {
    const list = answer.constants.map((c) => (typeof c === "string" ? c : `${c.type}.${c.constant}`));
    out.push(`  constants: ${list.join(", ")}`);
  }
  if (answer.types?.length) {
    out.push(`types (${answer.types.length}):`);
    for (const t of answer.types) out.push(`  ${t.kind} ${t.name}${beta(t.stability)}  (${t.surface})`);
  }
  if (answer.declaredBy?.length) {
    out.push("declared by:");
    for (const d of answer.declaredBy) out.push(`  ${d.package}.${d.type}  (${d.surface})`);
  }
  if (answer.matches?.length) {
    out.push(`matches (${answer.total}):`);
    for (const m of answer.matches) out.push(`  ${m.type}.${m.signature}  (${m.surface})`);
  }
  if (answer.didYouMean?.length) out.push(`did you mean: ${answer.didYouMean.join(", ")}`);
  if (out.length === 0) out.push("no match");

  return out.join("\n");
}

// -------------------------------------------------------------------- main ---

const args = parseArgs(process.argv.slice(2));

// Routing is answered from its own file and needs no surfaces loaded.
if (args.tasks) {
  const doc = JSON.parse(fs.readFileSync(TASKS_FILE, "utf8"));
  process.stdout.write(
    args.json
      ? `${JSON.stringify(doc.tasks.map((t) => ({ task: t.task, intent: t.intent })), null, 2)}\n`
      : `${doc.tasks.map((t) => `${t.task}\n  ${t.intent}`).join("\n")}\n`,
  );
  process.exit(0);
}

if (args.task) {
  const answer = answerTask(args.task);
  process.stdout.write(args.json ? `${JSON.stringify(answer, null, 2)}\n` : `${renderTask(answer)}\n`);
  process.exit(answer.found ? 0 : 3);
}

const index = loadSurfaces(args.surface);

if (args.dump) {
  process.stdout.write(`${JSON.stringify(index, null, 2)}\n`);
  process.exit(0);
}

if (!args.type && !args.method && !args.exists && !args.search && !args.constant && !args.package) {
  usage(2);
}

const answer = query(index, args);

process.stdout.write(args.json ? `${JSON.stringify(answer, null, 2)}\n` : `${render(answer)}\n`);
process.exit(answer.found ? 0 : 3);
