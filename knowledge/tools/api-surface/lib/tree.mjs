/**
 * knowledge/tools/api-surface/lib/tree.mjs — read a built module the way
 * `zip.mjs` reads a jar.
 *
 * The extractor was written to describe a *published* release: resolve a jar
 * from Maven, read its class files. That cannot describe the tree it is sitting
 * in, which is what a CI drift gate has to do — a gate that checks the last
 * release tells you nothing about the commit under review.
 *
 * A jar and a `target/classes` directory hold the same thing, so rather than
 * teach the extractor a second way to read class files, this presents a
 * directory through the interface `openJar` already returns: `{names, read}`,
 * with entry names relative and slash-separated exactly as a zip spells them.
 * Everything downstream — the annotation reader, the parameter-name indexer,
 * the class lister — is unchanged and cannot tell the difference.
 *
 * The same adapter serves `src/main/java`, which stands in for the sources jar:
 * a local build has no sources jar, and parameter names have to come from
 * somewhere. Reading the source tree keeps that free, whereas compiling with
 * `-parameters` would change every published class file to serve a docs tool.
 */

import fs from "node:fs";
import path from "node:path";

/**
 * A directory tree, presented as `openJar` presents an archive.
 *
 * @param {string} root directory to walk
 * @param {(name: string) => boolean} [accept] filter on the relative entry name
 * @returns {{names: string[], read(name: string): Buffer, root: string}}
 */
export function openDir(root, accept = () => true) {
  const names = [];

  const walk = (dir, prefix) => {
    let entries;
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch {
      return;
    }
    for (const entry of entries) {
      const relative = prefix ? `${prefix}/${entry.name}` : entry.name;
      if (entry.isDirectory()) {
        walk(path.join(dir, entry.name), relative);
      } else if (entry.isFile() && accept(relative)) {
        names.push(relative);
      }
    }
  };

  walk(root, "");
  names.sort();

  return {
    root,
    names,
    read: (name) => fs.readFileSync(path.join(root, ...name.split("/"))),
  };
}

/** Whether a path is a directory, so the caller can pick the right reader. */
export function isDirectory(target) {
  try {
    return fs.statSync(target).isDirectory();
  } catch {
    return false;
  }
}

/**
 * The `<version>` of the reactor, read from the root `pom.xml`.
 *
 * The first `<version>` element in that file is the project's own — the parent
 * declaration precedes any dependency or plugin version — so this does not need
 * an XML parser to be correct, and a build tool that pulls one in for a single
 * field would be a dependency on the critical path of a docs check.
 */
export function reactorVersion(repoRoot) {
  const pom = fs.readFileSync(path.join(repoRoot, "pom.xml"), "utf8");
  const match = pom.match(/<version>([^<]+)<\/version>/);
  if (!match) throw new Error(`no <version> in ${path.join(repoRoot, "pom.xml")}`);
  return match[1].trim();
}
