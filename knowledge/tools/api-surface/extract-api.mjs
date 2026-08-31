#!/usr/bin/env node
/**
 * tools/api-surface/extract-api.mjs — build the authoring allow-list from the
 * pinned GraphCompose artifact.
 *
 *   node tools/api-surface/extract-api.mjs --version 2.2.0
 *   node tools/api-surface/extract-api.mjs --version 2.2.0 --check
 *
 * The chain is: pinned artifact → this extractor → `api-surface.json` (the
 * canonical machine-readable representation) → `00-api-surface.md` (generated
 * from the JSON) → `scripts/api-query.mjs` (the deterministic query). One
 * source of truth, two renderings of it, neither hand-edited.
 *
 * It replaces a regex parser that read GraphCompose's Java **source**, which
 * could not see anything Lombok generates. That was not a cosmetic gap:
 * `DocumentHeaderFooter`, `DocumentMetadata`, `DocumentWatermark` and
 * `DocumentProtection` are Lombok value types whose entire construction path is
 * generated, so the allow-list listed `DocumentMetadata (class)` with no
 * members at all. Under the first invariant — "a symbol absent here does not
 * exist" — an agent reading that correctly concluded the type was
 * unconstructible, and page headers and footers were unreachable.
 *
 * Reading the class file also gives an exact definition of "generated": a
 * member the bytecode has and the source does not. Those are marked
 * `origin: "generated"` in the JSON and counted, so the regression test can
 * assert they never silently vanish again.
 *
 * --check regenerates into memory and compares with what is on disk, exiting 1
 * on any drift. That is what CI runs; it needs the artifact resolvable, so it
 * is skipped where it is not.
 *
 * Exit: 0 written (or --check clean) · 1 --check found drift · 2 usage
 *       4 the artifact could not be resolved
 */

import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

import { findJavap, readTypes, simplifyType } from "./lib/javap.mjs";
import { indexParameterNames, applyParameterNames } from "./lib/source-names.mjs";
import { openJar } from "./lib/zip.mjs";
import { openDir, isDirectory, reactorVersion } from "./lib/tree.mjs";
import { buildProvenance } from "./lib/provenance.mjs";
import { renderMarkdown } from "./lib/render-markdown.mjs";
import { readAnnotations, memberKeyForMember } from "./lib/annotations.mjs";
import {
  admit,
  stability,
  BETA,
  memberStability,
  SURFACES,
  inImplementationRoot,
  UNREFERENCED_REASON,
} from "./lib/surfaces.mjs";

// This file sits at knowledge/tools/api-surface/, so the repository root is
// three levels up. It was two in the AI Flow layout the extractor came from,
// where the same script lives at tools/api-surface/ — left unchanged on arrival
// to keep that copy byte-identical, and corrected here because this is the phase
// that owns the output layout.
const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..", "..");

const GROUP = "io.github.demchaav";
/**
 * Which artifacts carry authoring surface. The `graph-compose` artifact itself
 * is an aggregator whose jar is four kilobytes of nothing — pointing the
 * extractor at it produces an empty allow-list that looks like a clean run.
 */
const ARTIFACTS = [
  "graph-compose-core",
  "graph-compose-templates",
  "graph-compose-render-pdf",
  "graph-compose-render-docx",
  "graph-compose-render-pptx",
  "graph-compose-testing",
];

/**
 * Everything the classifier is allowed to look at.
 *
 * This is deliberately NOT the surface: it is the candidate set. Which of these
 * types are API, and which surface each belongs to, is decided in
 * `lib/surfaces.mjs` — here we only refuse to consider classes that are not
 * GraphCompose's at all. A type inside this root that matches no rule there is a
 * hard error, which is the point: the 2.0 module split dropped three modules out
 * of the allow-list precisely because the scope was a hand-written package list
 * that nobody remembered to extend.
 */
const CANDIDATE_ROOT = "com.demcha.compose";

/**
 * Bumped when the extraction or classification changes shape. It rides in the
 * tracked manifest so a surface can be told apart from one produced by an
 * older generator, which the GraphCompose version alone cannot do: the same
 * 2.2.1 read by two generators is two different documents.
 */
const GENERATOR_VERSION = "2.0.0";

/** Which reactor module builds which artifact, for `--from-reactor`. */
const MODULES = {
  "graph-compose-core": "core",
  "graph-compose-templates": "templates",
  "graph-compose-render-pdf": "render-pdf",
  "graph-compose-render-docx": "render-docx",
  "graph-compose-render-pptx": "render-pptx",
  "graph-compose-testing": "testing",
};

function usage(code = 0) {
  process.stdout.write(
    "usage: node knowledge/tools/api-surface/extract-api.mjs <input> [options]\n\n" +
      "input, exactly one:\n" +
      "  --from-reactor        read this working tree's */target/classes\n" +
      "  --from-release <x.y.z>  read a published release, resolved through Maven\n" +
      "  --version <x.y.z>     alias for --from-release\n\n" +
      "options:\n" +
      "  --out <dir>         write surfaces here (default: knowledge/api)\n" +
      "  --pack <dir>        deprecated alias for --out\n" +
      "  --m2 <dir>          local Maven repository (default: ~/.m2/repository)\n" +
      "  --offline           never invoke Maven; fail if the artifact is not cached\n" +
      "  --check             compare against what is on disk instead of writing\n" +
      "  --json              print a summary as JSON\n\n" +
      "exit: 0 ok | 1 --check found drift | 2 usage | 4 input unresolvable\n" +
      "     5 a public type matched no classification rule\n",
  );
  process.exit(code);
}

function parseArgs(argv) {
  const out = {
    version: null, reactor: false, out: null,
    m2: null, offline: false, check: false, json: false,
  };
  for (let i = 0; i < argv.length; i += 1) {
    const a = argv[i];
    if (a === "--help" || a === "-h") usage(0);
    else if (a === "--check") out.check = true;
    else if (a === "--json") out.json = true;
    else if (a === "--offline") out.offline = true;
    else if (a === "--from-reactor") out.reactor = true;
    else if (a === "--from-release" || a === "--version") out.version = argv[++i];
    else if (a === "--out" || a === "--pack") out.out = argv[++i];
    else if (a === "--m2") out.m2 = argv[++i];
    else usage(2);
  }
  // The two inputs describe different things — a release and a working tree —
  // and silently preferring one would make a CI gate check the wrong artifact.
  if (out.reactor && out.version) {
    process.stderr.write("[extract-api] --from-reactor and --from-release are exclusive\n");
    process.exit(2);
  }
  return out;
}

// --- artifact resolution -----------------------------------------------------

function artifactPath(m2, artifact, version, classifier) {
  const suffix = classifier ? `-${classifier}` : "";
  return path.join(
    m2,
    ...GROUP.split("."),
    artifact,
    version,
    `${artifact}-${version}${suffix}.jar`,
  );
}

function fetchArtifact(artifact, version, classifier) {
  const coords = `${GROUP}:${artifact}:${version}${classifier ? `:jar:${classifier}` : ""}`;
  const args = ["-q", "-B", "dependency:get", `-Dartifact=${coords}`];
  const run =
    process.platform === "win32"
      ? spawnSync("cmd.exe", ["/d", "/s", "/c", "mvn", ...args], { encoding: "utf8" })
      : spawnSync("mvn", args, { encoding: "utf8" });
  return run.status === 0;
}

/**
 * Resolve one artifact's binary jar, and its sources jar when available.
 * Sources are optional: without them the surface is complete but its parameters
 * are unnamed, which is a worse allow-list, not a wrong one.
 */
function resolveArtifact(artifact, version, { m2, offline }) {
  const binary = artifactPath(m2, artifact, version);
  if (!fs.existsSync(binary)) {
    if (offline || !fetchArtifact(artifact, version)) return null;
  }
  if (!fs.existsSync(binary)) return null;

  const sources = artifactPath(m2, artifact, version, "sources");
  if (!fs.existsSync(sources) && !offline) fetchArtifact(artifact, version, "sources");

  return { artifact, binary, sources: fs.existsSync(sources) ? sources : null };
}

/**
 * Resolve every artifact from the working tree instead of from Maven.
 *
 * `target/classes` stands in for the binary jar and `src/main/java` for the
 * sources jar; `openDir` presents both through the interface `openJar` returns,
 * so nothing downstream changes. A module that has not been built is fatal
 * rather than skipped: a surface silently missing a module is precisely the
 * failure this whole pack exists to prevent.
 */
function resolveReactor(repoRoot) {
  const resolved = [];
  const missing = [];
  for (const [artifact, module] of Object.entries(MODULES)) {
    const binary = path.join(repoRoot, module, "target", "classes");
    if (!isDirectory(binary)) {
      missing.push(`${module}/target/classes`);
      continue;
    }
    const sources = path.join(repoRoot, module, "src", "main", "java");
    resolved.push({
      artifact,
      binary,
      sources: isDirectory(sources) ? sources : null,
      fromDirectory: true,
    });
  }
  if (missing.length) {
    process.stderr.write(
      `[extract-api] not built: ${missing.join(", ")}\n` +
        "  Build the reactor slice first, then re-run:\n" +
        "    ./mvnw -q -DskipTests install -pl :graph-compose-core,:graph-compose-templates," +
        ":graph-compose-render-pdf,:graph-compose-render-docx,:graph-compose-render-pptx," +
        ":graph-compose-testing -am\n",
    );
    process.exit(4);
  }
  return resolved;
}

/** A jar or a directory, behind one interface. */
const openArchive = (target) => (isDirectory(target) ? openDir(target) : openJar(target));

// --- surface assembly --------------------------------------------------------

const inCandidateScope = (binaryName) =>
  binaryName === CANDIDATE_ROOT || binaryName.startsWith(`${CANDIDATE_ROOT}.`);

/** Class entries in the jar the classifier should consider. */
function candidateClassNames(jar) {
  return jar.names
    .filter((entry) => entry.endsWith(".class") && !entry.endsWith("package-info.class"))
    .map((entry) => entry.slice(0, -".class".length).replace(/\//g, "."))
    // A `Foo$1` is an anonymous class and a `Foo$Bar` may be a nested type that
    // matters (every Lombok builder is one), so only the anonymous form is cut.
    .filter((name) => !/\$\d/.test(name))
    .filter((name) => inCandidateScope(name.replace(/\$.*$/, "")))
    .sort();
}

/**
 * Package-level annotations, read from the `package-info` classes the type list
 * deliberately leaves out.
 *
 * These are where the boundary is actually drawn:
 * `com.demcha.compose.document.layout` is package-`@Internal` and holds 125
 * classes, so this one lookup excludes more of the jar than every other rule
 * combined.
 */
function readPackageAnnotations(jar) {
  const byPackage = new Map();
  for (const entry of jar.names) {
    if (!entry.endsWith("package-info.class")) continue;
    const pkg = entry.slice(0, -"/package-info.class".length).replace(/\//g, ".");
    if (!inCandidateScope(pkg)) continue;
    try {
      byPackage.set(pkg, readAnnotations(jar.read(entry)).type);
    } catch (error) {
      // Absent is fine — most packages have no package-info at all, and the
      // caller defaults to []. Present-but-unreadable is not: the reader throws
      // on an unknown constant-pool tag precisely because it cannot know what it
      // is looking at, and swallowing that turns "I could not tell" into "no
      // annotations here". For a package carrying @Internal that silently
      // publishes every type inside it, which is the one outcome this pack
      // exists to prevent.
      throw new Error(
        `cannot read package annotations for ${pkg} (${entry}): ${error.message}
` +
          "  The file exists but could not be parsed. Refusing to treat it as unannotated.",
      );
    }
  }
  return byPackage;
}

const OBJECT_OVERRIDES = new Set(["toString", "hashCode", "equals", "clone", "finalize"]);
const ENUM_MECHANICS = new Set(["values", "valueOf"]);

/** Members worth listing: what a person composing a document would call. */
function isInteresting(member, typeKind) {
  if (member.kind === "field") return false;
  if (member.kind === "constant") return true;
  if (OBJECT_OVERRIDES.has(member.name)) return false;
  if (typeKind === "enum" && ENUM_MECHANICS.has(member.name)) return false;
  if (member.name === "main") return false;
  return true;
}

/** Every type name a member mentions, as it is spelled in the surface. */
function referencedTypeNames(type) {
  const text = type.members
    .map((m) =>
      m.kind === "constant"
        ? m.type
        : [m.typeParameters, m.returns, ...m.params.map((p) => p.type)].filter(Boolean).join(" "),
    )
    .join(" ");
  return new Set(text.match(/[A-Z][\w.]*/g) ?? []);
}

/**
 * Nested types are kept only when the authoring surface can actually reach
 * them.
 *
 * Every Lombok `@Builder` produces one — `DocumentHeaderFooter.DocumentHeaderFooterBuilder`
 * is the entire construction path and must be listed. But a jar also holds
 * public nested implementation detail (`DocumentSession.RenderingContextImpl`,
 * `DocumentSession.InvalidatingNodeRegistry`) that nothing returns and nobody
 * should call. Listing those would pad the closed set an agent has to hold with
 * types that are not authoring surface at all.
 *
 * "Reachable" is transitive: a builder returned by a method, and anything that
 * builder in turn returns or takes.
 */
function reachableNestedTypes(all) {
  const nested = new Map(all.filter((t) => t.name.includes(".")).map((t) => [t.name, t]));
  const kept = new Set();
  let frontier = all.filter((t) => !t.name.includes("."));

  while (frontier.length) {
    const next = [];
    for (const type of frontier) {
      for (const referenced of referencedTypeNames(type)) {
        if (!nested.has(referenced) || kept.has(referenced)) continue;
        kept.add(referenced);
        next.push(nested.get(referenced));
      }
    }
    frontier = next;
  }
  return kept;
}

function buildSurface({ version, artifacts, javap }) {
  const types = [];

  for (const resolved of artifacts) {
    const jar = openArchive(resolved.binary);
    const classNames = candidateClassNames(jar);
    if (!classNames.length) continue;

    const packageAnnotations = readPackageAnnotations(jar);
    const raw = readTypes({ javap, classpath: resolved.binary, classNames });

    // javap is run in batches, and a class it cannot read does not stop the
    // batch: it reports on stderr and returns the rest, so the surface comes out
    // silently one type smaller and the run still exits 0. A quietly shrinking
    // allow-list is the exact shape of the drift this pack exists to end, so
    // every candidate that went in must come back out.
    const returned = new Set(raw.map((t) => t.binaryName));
    const dropped = classNames.filter((name) => !returned.has(name));
    if (dropped.length) {
      throw new Error(
        `javap returned nothing for ${dropped.length} class(es) of ${resolved.artifact}:
` +
          dropped.map((n) => `    ${n}
`).join("") +
          "  They would have vanished from the surface without failing the run.",
      );
    }

    const names = resolved.sources
      ? indexParameterNames(openArchive(resolved.sources), (entry) =>
          inCandidateScope(entry.slice(0, -".java".length).replace(/\//g, ".")),
        )
      : new Map();

    for (const type of raw) {
      // A jar holds package-private and annotation types too. Neither is
      // authoring surface: one cannot be named from another package, the other
      // cannot be called at all.
      if (!type.isPublic || type.kind === "annotation") continue;
      const simple = simplifyType(type.binaryName);
      const pkg = type.binaryName.replace(/\$.*$/, "").replace(/\.[^.]+$/, "");

      // Same asymmetry as for package-info: a class the archive does not hold
      // under that name has no annotations to read, but one it holds and cannot
      // parse must stop the run. A parse failure that degrades to "unannotated"
      // would let an @Internal type into the surface as public API, and the
      // whole value of this file is that its "no" can be trusted.
      const entryName = `${type.binaryName.replace(/\./g, "/")}.class`;
      let annotations = { type: [], methods: new Map(), ambiguous: [] };
      if (jar.names.includes(entryName)) {
        try {
          annotations = readAnnotations(jar.read(entryName));
        } catch (error) {
          throw new Error(
            `cannot read annotations for ${type.binaryName} (${entryName}): ${error.message}
` +
              "  The class exists but could not be parsed. Refusing to treat it as unannotated.",
          );
        }
      }

      const members = type.members
        .filter((member) => isInteresting(member, type.kind))
        .map((member) => {
          const origin = resolved.sources ? applyParameterNames(names, simple, member) : "unknown";
          return {
            kind: member.kind,
            name: member.name,
            static: Boolean(member.static),
            origin,
            annotations:
              member.kind === "constant"
                ? []
                : annotations.methods.get(
                    // Through the shared helper, never memberKey directly: a
                    // constructor is `<init>` in the class file and the simple
                    // type name in javap's rendering, and building the key here
                    // is how that difference went unnoticed.
                    memberKeyForMember({
                      kind: member.kind,
                      name: member.name,
                      params: member.params.map((prm) => simplifyType(prm.type)),
                    }),
                  ) ?? [],
            ...(member.kind === "constant"
              ? { type: simplifyType(member.type) }
              : {
                  typeParameters: member.typeParameters ? simplifyType(member.typeParameters) : null,
                  returns: member.returns === null ? null : simplifyType(member.returns),
                  params: member.params.map((prm) => ({
                    type: simplifyType(prm.type),
                    name: prm.name,
                  })),
                }),
          };
        });

      types.push({
        name: simple,
        binaryName: type.binaryName,
        package: pkg,
        kind: type.kind,
        modifiers: type.modifiers,
        artifact: resolved.artifact,
        annotations: annotations.type,
        packageAnnotations: packageAnnotations.get(pkg) ?? [],
        ambiguous: annotations.ambiguous,
        members,
      });
    }
  }

  const reachable = reachableNestedTypes(types);

  // A nested type is normally kept only when the surface can reach it, which
  // drops public nested implementation detail nothing returns. But an
  // annotation is an explicit act of publication and outranks that heuristic:
  // `DocumentPaint.LinearAxis` and `.RadialCircle` are `@Beta` records
  // implementing `DocumentPaint`, and because `DocumentPaint.linear(...)` is
  // declared to return the *interface*, no signature mentions them. They were
  // vanishing into neither a surface nor the exclusion list — the one outcome
  // this classifier is not allowed to produce.
  const keepNested = (t) => reachable.has(t.name) || t.annotations.length > 0;
  const candidates = types.filter((t) => !t.name.includes(".") || keepNested(t));

  const unreachableNested = types
    .filter((t) => t.name.includes(".") && !keepNested(t))
    .map((t) => ({
      binaryName: t.binaryName,
      package: t.package,
      kind: t.kind,
      artifact: t.artifact,
      reason: "nested type no admitted signature mentions",
    }));

  // --- classify -------------------------------------------------------------
  const bySurface = new Map(SURFACES.map((surface) => [surface, []]));
  const excluded = [...unreachableNested];
  let unclassified = [];
  const ambiguous = [];

  for (const type of candidates) {
    if (type.ambiguous.length) {
      ambiguous.push(`${type.binaryName}: ${type.ambiguous.join(", ")}`);
    }

    const verdict = admit({
      binaryName: type.binaryName,
      packageName: type.package,
      annotations: type.annotations,
      packageAnnotations: type.packageAnnotations,
    });

    if (!verdict.admitted) {
      if (verdict.reason === null) {
        unclassified.push(type.binaryName);
      } else {
        excluded.push({
          binaryName: type.binaryName,
          package: type.package,
          kind: type.kind,
          artifact: type.artifact,
          reason: verdict.reason,
        });
      }
      continue;
    }

    const typeStability = stability(type);
    const members = [];
    for (const member of type.members) {
      const memberVerdict = memberStability(member.annotations, typeStability);
      if (memberVerdict === null) {
        excluded.push({
          binaryName: `${type.binaryName}#${member.name}`,
          package: type.package,
          kind: member.kind,
          artifact: type.artifact,
          reason: "member @Internal",
        });
        continue;
      }
      const { annotations: _drop, ...rest } = member;
      members.push(memberVerdict === "stable" ? rest : { ...rest, stability: memberVerdict });
    }

    bySurface.get(verdict.surface).push({
      name: type.name,
      binaryName: type.binaryName,
      package: type.package,
      packageStability: type.packageAnnotations.includes(BETA) ? "beta" : "stable",
      kind: type.kind,
      modifiers: type.modifiers,
      artifact: type.artifact,
      ...(typeStability === "stable" ? {} : { stability: typeStability }),
      members,
    });
  }

  // --- second pass: types the admitted surface actually hands you ----------
  //
  // `engine.components.*` is not engine internals despite its name: it holds the
  // shared value types the public API makes you construct — `TextStyle`,
  // `DocumentMetadata`, `HeaderFooterConfig`, `WatermarkConfig`,
  // `PdfProtectionConfig` and their Lombok builders. A hand-written package list
  // left every one of them out, so the allow-list described methods that take a
  // `TextStyle` while denying that `TextStyle` existed.
  //
  // Rather than sort those packages by hand, admit what the surface reaches:
  // if an admitted member mentions a type, an author needs it, and hiding it
  // makes the closed set a lie. If nothing public mentions it, it is internal by
  // construction. Transitive, because a builder returned by an admitted method
  // hands back types of its own.
  if (unclassified.length) {
    const stillOut = new Map(
      candidates
        .filter((t) => unclassified.includes(t.binaryName))
        .map((t) => [t.name, t]),
    );
    const admittedTypes = [...bySurface.values()].flat();
    let frontier = admittedTypes;
    const pulled = new Set();

    while (frontier.length) {
      const next = [];
      for (const type of frontier) {
        for (const referenced of referencedTypeNames(type)) {
          const found = stillOut.get(referenced);
          if (!found || pulled.has(referenced)) continue;
          pulled.add(referenced);
          next.push(found);
        }
      }
      frontier = next;
    }

    for (const type of pulled.size ? [...pulled].map((n) => stillOut.get(n)) : []) {
      const typeStability = stability(type);
      const { annotations: _a, packageAnnotations: _p, ambiguous: _m, members: rawMembers, ...head } = type;
      const members = [];
      for (const member of rawMembers) {
        const memberVerdict = memberStability(member.annotations, typeStability);
        if (memberVerdict === null) continue;
        const { annotations: _drop, ...rest } = member;
        members.push(memberVerdict === "stable" ? rest : { ...rest, stability: memberVerdict });
      }
      bySurface.get("authoring").push({
        ...head,
        ...(typeStability === "stable" ? {} : { stability: typeStability }),
        reachedVia: "referenced by admitted API",
        members,
      });
    }

    unclassified = unclassified.filter((n) => !pulled.has(simplifyType(n)));
  }

  // What the surface never mentions, inside a package declared implementation,
  // is excluded with a reason rather than failing the run. Outside those roots
  // it still fails: an unruled package is the thing this whole mechanism exists
  // to notice.
  const stillUnclassified = [];
  for (const binaryName of unclassified) {
    const type = candidates.find((t) => t.binaryName === binaryName);
    if (type && inImplementationRoot(type.package)) {
      excluded.push({
        binaryName: type.binaryName,
        package: type.package,
        kind: type.kind,
        artifact: type.artifact,
        reason: UNREFERENCED_REASON,
      });
      continue;
    }
    stillUnclassified.push(binaryName);
  }
  unclassified = stillUnclassified;

  return { version, bySurface, excluded, unclassified, ambiguous };
}

/** One surface document, in the shape `api-query` and the Markdown view expect. */
function surfaceDocument({ surface, version, types, artifacts }) {
  const counts = { types: 0, methods: 0, constants: 0, generated: 0 };
  for (const type of types) {
    counts.types += 1;
    counts.methods += type.members.filter((m) => m.kind !== "constant").length;
    counts.constants += type.members.filter((m) => m.kind === "constant").length;
    counts.generated += type.members.filter((m) => m.origin === "generated").length;
  }

  const sorted = [...types].sort(
    (a, b) => a.package.localeCompare(b.package) || a.name.localeCompare(b.name),
  );

  const packages = [];
  for (const type of sorted) {
    let bucket = packages[packages.length - 1];
    if (!bucket || bucket.name !== type.package) {
      bucket = {
        name: type.package,
        // Recorded rather than inferred. A consumer cannot tell a beta *package*
        // from a package whose one admitted type happens to be beta, and
        // `document.layout` is exactly that: everything in it is excluded except
        // `NodeDefinition`, which is beta.
        ...(type.packageStability === "beta" ? { stability: "beta" } : {}),
        types: [],
      };
      packages.push(bucket);
    }
    const { package: _drop, packageStability: _drop2, ...rest } = type;
    bucket.types.push(rest);
  }

  return {
    schemaVersion: 2,
    targetLibrary: "GraphCompose",
    surface,
    targetVersion: `${version.split(".").slice(0, 2).join(".")}.x`,
    verifiedAgainst: version,
    generator: "knowledge/tools/api-surface/extract-api.mjs",
    generatedFrom: artifacts.map((a) => `${GROUP}:${a.artifact}:${version}`),
    parameterNamesFrom: artifacts.filter((a) => a.sources).map((a) => `${a.artifact}:sources`),
    counts,
    packages,
  };
}

// --- run ---------------------------------------------------------------------

const args = parseArgs(process.argv.slice(2));
if (!args.reactor && !args.version) usage(2);

const outDir = args.out ?? path.join(repoRoot, "knowledge", "api");
const m2 = args.m2 ?? path.join(os.homedir(), ".m2", "repository");
const mode = args.reactor ? "reactor" : "release";

let version;
let resolved;

if (args.reactor) {
  version = reactorVersion(repoRoot);
  resolved = resolveReactor(repoRoot);
} else {
  version = args.version;
  resolved = ARTIFACTS.map((artifact) => resolveArtifact(artifact, version, { m2, offline: args.offline }));
  if (resolved.some((r) => r === null)) {
    const missing = ARTIFACTS.filter((_, i) => resolved[i] === null);
    process.stderr.write(
      `[extract-api] could not resolve ${missing.join(", ")} ${version} from ${m2}` +
        (args.offline ? " (offline)" : " or Maven") +
        "\n",
    );
    process.exit(4);
  }
}

const built = buildSurface({ version, artifacts: resolved, javap: findJavap() });

// A public type that matched no rule is a hard error. A silent drop is exactly
// how the 2.0 module split removed three modules from the allow-list with
// nobody noticing, so "I do not know where this belongs" has to be loud.
if (built.unclassified.length) {
  process.stderr.write(
    `[extract-api] ${built.unclassified.length} public type(s) matched no classification rule.\n` +
      "  Every public type must be admitted to a surface or excluded with a reason.\n" +
      "  Add a rule in knowledge/tools/api-surface/lib/surfaces.mjs:\n\n" +
      built.unclassified.map((n) => `    ${n}\n`).join(""),
  );
  process.exit(5);
}

if (built.ambiguous.length) {
  process.stderr.write(
    `[extract-api] ${built.ambiguous.length} member key(s) collided with differing annotations:\n` +
      built.ambiguous.map((n) => `    ${n}\n`).join("") +
      "  Refusing to guess which overload carries the annotation.\n",
  );
  process.exit(5);
}

const files = [];
for (const surface of SURFACES) {
  const doc = surfaceDocument({
    surface,
    version,
    types: built.bySurface.get(surface),
    artifacts: resolved,
  });
  files.push([path.join(outDir, `${surface}.json`), `${JSON.stringify(doc, null, 2)}\n`]);
  files.push([path.join(outDir, `${surface}.md`), renderMarkdown(doc)]);
}

const excludedDoc = {
  schemaVersion: 2,
  verifiedAgainst: version,
  generator: "knowledge/tools/api-surface/extract-api.mjs",
  note:
    "Public types and members deliberately kept out of every surface. An " +
    "exclusion nobody can see is indistinguishable from a bug, so each one " +
    "records why.",
  count: built.excluded.length,
  excluded: [...built.excluded].sort((a, b) => a.binaryName.localeCompare(b.binaryName)),
};
files.push([path.join(outDir, "excluded.json"), `${JSON.stringify(excludedDoc, null, 2)}\n`]);

/**
 * The tracked manifest. Stable facts only.
 *
 * Nothing here changes unless a human changes something: no commit, no
 * timestamp, no digest. A tracked file cannot hold the SHA of the commit it is
 * part of — the value would be false the moment it is committed — and class
 * digests are not reproducible across machines, so a gate comparing them would
 * be red on every run. Those live in `target/knowledge/provenance.json`, which
 * is never committed.
 */
const manifest = {
  schemaVersion: 2,
  targetLibrary: "GraphCompose",
  targetVersion: `${version.split(".").slice(0, 2).join(".")}.x`,
  generator: "knowledge/tools/api-surface/extract-api.mjs",
  generatorVersion: GENERATOR_VERSION,
  note:
    "Stable contract. Provenance for a particular run (commit, timestamp, " +
    "artifact digests) is written to target/knowledge/provenance.json and is " +
    "deliberately not tracked.",
  surfaces: SURFACES.map((surface) => ({
    surface,
    json: `knowledge/api/${surface}.json`,
    markdown: `knowledge/api/${surface}.md`,
  })),
  classification: {
    stages: [
      "A admission: type @Internal | explicit SPI list | package @Internal | explicit exclusion | surface rule | hard error",
      "B stability: type @Beta | package @Beta | stable — resolved per type, nested types separately",
      "C members: member @Internal excluded | member @Beta | inherit the type",
    ],
    annotations: {
      internal: "com.demcha.compose.document.api.Internal",
      beta: "com.demcha.compose.document.api.Beta",
    },
    rules: "knowledge/tools/api-surface/lib/surfaces.mjs",
    excluded: "knowledge/api/excluded.json",
  },
};
files.push([path.join(outDir, "..", "manifest.json"), `${JSON.stringify(manifest, null, 2)}\n`]);

const totals = SURFACES.reduce((acc, surface) => {
  acc[surface] = built.bySurface.get(surface).length;
  return acc;
}, {});

if (args.check) {
  const drift = [];
  for (const [file, expected] of files) {
    const actual = fs.existsSync(file) ? fs.readFileSync(file, "utf8").replace(/\r\n/g, "\n") : null;
    if (actual !== expected) drift.push(path.relative(repoRoot, file));
  }
  if (args.json) {
    process.stdout.write(`${JSON.stringify({ version, mode, drift, totals }, null, 2)}\n`);
  } else if (drift.length) {
    process.stdout.write(
      `[extract-api] out of date: ${drift.join(", ")}\n` +
        "  regenerate: node knowledge/tools/api-surface/extract-api.mjs " +
        `${mode === "reactor" ? "--from-reactor" : `--from-release ${version}`}\n`,
    );
  } else {
    process.stdout.write(`[extract-api] ${version} surfaces are current (${mode})\n`);
  }
  process.exit(drift.length ? 1 : 0);
}

fs.mkdirSync(outDir, { recursive: true });
for (const [file, text] of files) fs.writeFileSync(file, text, "utf8");

// Provenance goes to target/, never beside the surfaces.
const provenanceDir = path.join(repoRoot, "target", "knowledge");
fs.mkdirSync(provenanceDir, { recursive: true });
fs.writeFileSync(
  path.join(provenanceDir, "provenance.json"),
  `${JSON.stringify(
    buildProvenance({
      repoRoot,
      version,
      mode,
      artifacts: resolved,
      generatorVersion: GENERATOR_VERSION,
      now: new Date().toISOString(),
    }),
    null,
    2,
  )}\n`,
  "utf8",
);

if (args.json) {
  process.stdout.write(
    `${JSON.stringify({ version, mode, outDir, totals, excluded: built.excluded.length }, null, 2)}\n`,
  );
} else {
  process.stdout.write(
    `[extract-api] ${version} (${mode}) -> ${path.relative(repoRoot, outDir)}\n` +
      SURFACES.map((surface) => `  ${surface.padEnd(14)} ${totals[surface]} types\n`).join("") +
      `  excluded       ${built.excluded.length}\n` +
      `  provenance     ${path.relative(repoRoot, provenanceDir)}/provenance.json\n`,
  );
}
