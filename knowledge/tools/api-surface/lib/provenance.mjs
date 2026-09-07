/**
 * knowledge/tools/api-surface/lib/provenance.mjs — what produced this run.
 *
 * Kept in its own module, and written to `target/`, because the two kinds of
 * fact must not share a file:
 *
 *   knowledge/manifest.json   tracked. Stable: schema, version line, generator
 *                             version, the surfaces, the classification rules.
 *                             Changes only when a human changes something.
 *   target/knowledge/provenance.json   never committed. Commit, dirty flag,
 *                             timestamp, per-artifact digest.
 *
 * A tracked file cannot hold the SHA of the commit it is part of — the value is
 * false the instant it is committed. Excluding the field from `--check` does not
 * make it true, only unenforced, so the split is physical rather than a rule
 * about which fields to compare.
 *
 * The digests are provenance, not identity. Class files are not byte-
 * reproducible across machines and JDK builds, so comparing them in CI would
 * turn the gate red on every run regardless of whether the API moved. What
 * `--check` compares is the semantic surface; this file exists to tell two
 * builds of the same `2.2.1-SNAPSHOT` apart afterwards, which the version string
 * alone cannot do.
 */

import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";

const git = (repoRoot, args) => {
  const run = spawnSync("git", ["-C", repoRoot, ...args], { encoding: "utf8" });
  return run.status === 0 ? run.stdout.trim() : null;
};

/** SHA-256 over a jar, or over a class directory's contents in a stable order. */
function digest(target) {
  const hash = crypto.createHash("sha256");
  const stat = fs.statSync(target);

  if (stat.isFile()) {
    hash.update(fs.readFileSync(target));
    return hash.digest("hex");
  }

  const files = [];
  const walk = (dir, prefix) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
      const relative = prefix ? `${prefix}/${entry.name}` : entry.name;
      if (entry.isDirectory()) walk(path.join(dir, entry.name), relative);
      else if (entry.isFile()) files.push(relative);
    }
  };
  walk(target, "");
  files.sort();

  // Names as well as bytes: two trees with the same content under different
  // names are not the same build.
  for (const name of files) {
    hash.update(name);
    hash.update(fs.readFileSync(path.join(target, ...name.split("/"))));
  }
  return hash.digest("hex");
}

/**
 * @param {object} args
 * @param {string} args.repoRoot
 * @param {string} args.version
 * @param {"reactor"|"release"} args.mode
 * @param {Array<{artifact: string, binary: string}>} args.artifacts
 * @param {string} args.generatorVersion
 * @param {string} args.now ISO timestamp, passed in so the caller owns the clock
 */
export function buildProvenance({ repoRoot, version, mode, artifacts, generatorVersion, now }) {
  const commit = git(repoRoot, ["rev-parse", "HEAD"]);
  const status = git(repoRoot, ["status", "--porcelain"]);

  return {
    schemaVersion: 1,
    note:
      "Provenance for one extraction run. Never committed: a tracked file " +
      "cannot hold the SHA of the commit it belongs to, and class digests are " +
      "not reproducible across machines, so neither belongs in a gate.",
    version,
    mode,
    generator: "knowledge/tools/api-surface/extract-api.mjs",
    generatorVersion,
    generatedAt: now,
    git: {
      commit,
      branch: git(repoRoot, ["rev-parse", "--abbrev-ref", "HEAD"]),
      // A dirty tree means the surfaces describe something that exists on one
      // machine and nowhere else. Worth knowing when a bundle is questioned.
      dirty: status === null ? null : status.length > 0,
    },
    artifacts: artifacts.map((a) => ({
      artifact: a.artifact,
      source: path.relative(repoRoot, a.binary).split(path.sep).join("/"),
      sha256: digest(a.binary),
    })),
  };
}
