/**
 * tools/api-surface/lib/javap.mjs — read a type's real public members out of
 * compiled classes.
 *
 * Why bytecode and not source: GraphCompose declares a good part of its
 * authoring surface with Lombok. `DocumentHeaderFooter` is `@Getter
 * @Builder(toBuilder = true) @AllArgsConstructor(access = PRIVATE)` over eleven
 * private fields, and its source text contains exactly one public method. The
 * source-regex indexer therefore emitted a type with one member and no way to
 * construct it, while the class file carries `builder()`, `toBuilder()`, eleven
 * getters and a nested builder with eleven setters and `build()`. An allow-list
 * whose first rule is "a symbol absent here does not exist" made that API
 * unreachable.
 *
 * `javap` ships with the JDK the harness already requires, so reading the
 * pinned artifact needs nothing installed that a render does not.
 *
 * Bytecode has no parameter names (they are not in the class file unless
 * compiled with -parameters, and these are not), so names are merged in from
 * the sources jar afterwards — see source-names.mjs. Bytecode decides what
 * EXISTS; source only decides what things are CALLED.
 */

import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";

const MODIFIERS = new Set([
  "public",
  "protected",
  "private",
  "static",
  "final",
  "abstract",
  "default",
  "synchronized",
  "native",
  "strictfp",
  "transient",
  "volatile",
]);

/**
 * Locate javap: JAVA_HOME first (what CI sets), then PATH, then the usual
 * Windows install roots. Failing loudly with the places tried beats a tool that
 * silently produces an empty surface.
 */
export function findJavap({ env = process.env, platform = process.platform } = {}) {
  const exe = platform === "win32" ? "javap.exe" : "javap";
  const tried = [];

  for (const home of [env.JAVA_HOME, env.JDK_HOME].filter(Boolean)) {
    const candidate = path.join(home, "bin", exe);
    tried.push(candidate);
    if (fs.existsSync(candidate)) return candidate;
  }

  const onPath = spawnSync(platform === "win32" ? "where" : "which", ["javap"], {
    encoding: "utf8",
  });
  if (onPath.status === 0) {
    const first = onPath.stdout.split(/\r?\n/).find((line) => line.trim());
    if (first && fs.existsSync(first.trim())) return first.trim();
  }
  tried.push("javap on PATH");

  if (platform === "win32") {
    for (const root of ["C:/Program Files/Java", "C:/Program Files/Eclipse Adoptium"]) {
      if (!fs.existsSync(root)) continue;
      const jdks = fs
        .readdirSync(root)
        .filter((d) => /jdk/i.test(d))
        .sort()
        .reverse();
      for (const jdk of jdks) {
        const candidate = path.join(root, jdk, "bin", exe);
        tried.push(candidate);
        if (fs.existsSync(candidate)) return candidate;
      }
    }
  }

  throw new Error(
    `javap not found. It ships with the JDK; set JAVA_HOME or put it on PATH.\nTried:\n  ${tried.join("\n  ")}`,
  );
}

/** Split on `sep` at depth 0, so generic arguments stay in one piece. */
function splitTopLevel(text, sep) {
  const out = [];
  let depth = 0;
  let current = "";
  for (const ch of text) {
    if (ch === "<" || ch === "(") depth += 1;
    else if (ch === ">" || ch === ")") depth -= 1;
    if (ch === sep && depth === 0) {
      out.push(current);
      current = "";
      continue;
    }
    current += ch;
  }
  if (current.trim() !== "" || out.length) out.push(current);
  return out.map((s) => s.trim()).filter((s) => s !== "");
}

/** Tokenize a declaration head on spaces, keeping `<...>` groups whole. */
function tokenizeHead(head) {
  const tokens = [];
  let depth = 0;
  let current = "";
  for (const ch of head) {
    if (ch === "<") depth += 1;
    else if (ch === ">") depth -= 1;
    if (ch === " " && depth === 0) {
      if (current) tokens.push(current);
      current = "";
      continue;
    }
    current += ch;
  }
  if (current) tokens.push(current);
  return tokens;
}

/** `com.demcha.compose.document.output.Foo$Bar<java.lang.String>` → `Foo.Bar<String>`. */
export function simplifyType(type) {
  return type.replace(/\b(?:[a-z][\w]*\.)+([A-Z][\w$]*)/g, (_, name) => name).replace(/\$/g, ".");
}

function parseHeader(line) {
  const head = line.replace(/\s*\{\s*$/, "");
  // `extends` / `implements` decide record and enum, and are otherwise noise:
  // the allow-list describes what a type offers, not its hierarchy.
  const declaration = head.split(/\s+(?:extends|implements)\s+/)[0];
  const supers = head.slice(declaration.length);

  const tokens = tokenizeHead(declaration);
  const modifiers = [];
  while (tokens.length && MODIFIERS.has(tokens[0])) modifiers.push(tokens.shift());

  let kind = tokens.shift(); // class | interface
  const binaryName = (tokens.shift() ?? "").replace(/<.*$/, "");
  // javap has no `record`, `enum` or `@interface` keyword — it prints the
  // erased form and leaves the supertype to say which it was.
  if (/\bextends java\.lang\.Record\b/.test(supers)) kind = "record";
  if (/\bextends java\.lang\.Enum</.test(supers)) kind = "enum";
  if (/\bextends java\.lang\.annotation\.Annotation\b/.test(supers)) kind = "annotation";

  return {
    kind,
    binaryName,
    isPublic: modifiers.includes("public"),
    modifiers: modifiers.filter((m) => m !== "public"),
  };
}

function parseMember(line, ownerBinaryName) {
  const text = line.replace(/;\s*$/, "").trim();
  if (!text) return null;

  const open = text.indexOf("(");
  if (open === -1) {
    // A field: `public static final <type> <NAME>`
    const tokens = tokenizeHead(text);
    const modifiers = [];
    while (tokens.length && MODIFIERS.has(tokens[0])) modifiers.push(tokens.shift());
    if (tokens.length < 2) return null;
    const name = tokens.pop();
    return {
      kind: modifiers.includes("static") && modifiers.includes("final") ? "constant" : "field",
      name,
      static: modifiers.includes("static"),
      type: tokens.join(" "),
    };
  }

  // Everything from the matching `)` onwards is a throws clause.
  let depth = 0;
  let close = -1;
  for (let i = open; i < text.length; i += 1) {
    if (text[i] === "(") depth += 1;
    else if (text[i] === ")") {
      depth -= 1;
      if (depth === 0) {
        close = i;
        break;
      }
    }
  }
  if (close === -1) return null;

  const paramText = text.slice(open + 1, close);
  const tokens = tokenizeHead(text.slice(0, open));
  const modifiers = [];
  while (tokens.length && MODIFIERS.has(tokens[0])) modifiers.push(tokens.shift());

  let typeParameters = null;
  if (tokens.length && tokens[0].startsWith("<")) typeParameters = tokens.shift();

  if (!tokens.length) return null;
  const name = tokens.pop();
  const returns = tokens.join(" ");

  const isConstructor = returns === "" && name === ownerBinaryName;
  return {
    kind: isConstructor ? "constructor" : "method",
    name: isConstructor ? ownerBinaryName.split(/[.$]/).pop() : name,
    static: modifiers.includes("static"),
    typeParameters,
    returns: isConstructor ? null : returns,
    params: splitTopLevel(paramText, ",").map((type) => ({ type, name: null })),
  };
}

/**
 * Parse the output of `javap -public` over one or more classes.
 *
 * @returns {Array<{kind, binaryName, modifiers, members}>}
 */
export function parseJavap(output) {
  const types = [];
  let current = null;
  for (const raw of output.split(/\r?\n/)) {
    const line = raw.trim();
    if (!line || line.startsWith("Compiled from")) continue;
    if (line === "}") {
      current = null;
      continue;
    }
    if (line.endsWith("{")) {
      current = { ...parseHeader(line), members: [] };
      types.push(current);
      continue;
    }
    if (!current) continue;
    const member = parseMember(line, current.binaryName);
    if (member) current.members.push(member);
  }
  return types;
}

/**
 * Run javap over a list of binary class names, in batches small enough for a
 * Windows command line.
 */
export function readTypes({ javap, classpath, classNames, batchSize = 120 }) {
  const types = [];
  for (let i = 0; i < classNames.length; i += batchSize) {
    const batch = classNames.slice(i, i + batchSize);
    const run = spawnSync(javap, ["-public", "-cp", classpath, ...batch], {
      encoding: "utf8",
      maxBuffer: 64 * 1024 * 1024,
    });
    if (run.status !== 0) {
      throw new Error(`javap failed on batch ${i / batchSize}: ${(run.stderr || run.stdout || "").trim()}`);
    }
    types.push(...parseJavap(run.stdout));
  }
  return types;
}
