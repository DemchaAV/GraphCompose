#!/usr/bin/env node
/**
 * knowledge/tools/routing/test/pack-version.test.mjs — version ordering, and the
 * one copy of it that ships.
 *
 *   node knowledge/tools/routing/test/pack-version.test.mjs
 *
 * Exit 0 all passed · 1 a case failed.
 *
 * Two things are pinned here.
 *
 * The first is the release-tag case, which is the whole reason the rule is not
 * string equality: at a tag the surfaces are re-stamped `2.4.0` while the routes
 * still say `2.4.0-SNAPSHOT`, and a gate that calls that stale calls every route
 * in the file stale on every release.
 *
 * The second is `api-query.mjs`'s private copy of the same rule. It cannot
 * import this module — `build-bundle.mjs` publishes that one file as
 * `bin/query.mjs` with nothing beside it — so the copy is held here instead, by
 * running the identical table through the shipped CLI's own function. A
 * duplicate nothing compares is a duplicate that drifts, and this one already
 * did: the CLI shipped raw string comparison while the gate compared numeric
 * heads.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { compareVersions, versionParts, packVersionOf } from "../lib/pack-version.mjs";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const API_QUERY = path.join(HERE, "..", "..", "api-query", "api-query.mjs");

let failures = 0;
let passes = 0;

function check(name, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a === e) {
    passes += 1;
    return;
  }
  failures += 1;
  process.stdout.write(`  FAIL  ${name}\n        expected ${e}\n        actual   ${a}\n`);
}

// ----------------------------------------------------------- versionParts ---

check("a release version parses", versionParts("2.4.0"), [2, 4, 0]);
check("a snapshot drops its qualifier", versionParts("2.4.0-SNAPSHOT"), [2, 4, 0]);
check("surrounding space is tolerated", versionParts(" 2.4.0 "), [2, 4, 0]);
check("a typo is not orderable", versionParts("2.4.O-SNPASHOT"), null);
check("a version line is not a version", versionParts("2.4.x"), null);
check("an empty string is not orderable", versionParts(""), null);

// --------------------------------------------------------- compareVersions ---

/** The table both implementations must agree on. */
const CASES = [
  // The release tag. If this is anything but 0, every route in the file is
  // reported stale the moment the qualifier is dropped.
  ["2.4.0-SNAPSHOT", "2.4.0", 0],
  ["2.4.0", "2.4.0-SNAPSHOT", 0],
  ["2.4.0", "2.4.0", 0],
  // Genuinely behind.
  ["2.2.3-SNAPSHOT", "2.4.0-SNAPSHOT", -1],
  ["2.3.9", "2.4.0", -1],
  ["1.9.0", "2.0.0", -1],
  // Ahead of the surfaces: not age, and not something to report as age.
  ["2.5.0-SNAPSHOT", "2.4.0-SNAPSHOT", 1],
  ["2.4.1", "2.4.0", 1],
  // Shorter and longer heads compare on the parts they have.
  ["2.4", "2.4.0", 0],
  ["2.4.0.1", "2.4.0", 1],
  // Unorderable on either side.
  ["2.4.O-SNPASHOT", "2.4.0", null],
  ["2.4.0", "2.4.x", null],
];

for (const [route, pack, expected] of CASES) {
  check(`compare ${route} against ${pack}`, compareVersions(route, pack), expected);
}

// ---------------------------------------------------------- packVersionOf ---

check(
  "the first stamped surface wins",
  packVersionOf([{ surface: "authoring" }, { verifiedAgainst: "2.4.0-SNAPSHOT" }]),
  "2.4.0-SNAPSHOT",
);
check("no stamp anywhere yields null", packVersionOf([{ targetVersion: "2.4.x" }, {}]), null);
check("an empty set yields null", packVersionOf([]), null);

// ------------------------------------------- the copy that ships in the bundle ---

// api-query.mjs is loaded as text and its comparison lifted out, because the
// file is a CLI that runs on import-time argv. Failing to find the function is
// itself a failure: a rename that silently ends this check would leave the
// shipped copy unpinned, which is the state that let it drift in the first place.
const source = fs.readFileSync(API_QUERY, "utf8");
const shipped = source.match(/function compareVersions\([\s\S]*?\n}/);
const shippedParts = source.match(/function versionParts\([\s\S]*?\n}/);

if (!shipped || !shippedParts) {
  failures += 1;
  process.stdout.write(
    "  FAIL  api-query.mjs no longer defines versionParts/compareVersions\n" +
      "        the shipped copy of the rule is no longer pinned by this test\n",
  );
} else {
  const shippedCompare = new Function(
    `${shippedParts[0]}\n${shipped[0]}\nreturn compareVersions;`,
  )();
  for (const [route, pack, expected] of CASES) {
    check(`[api-query copy] compare ${route} against ${pack}`, shippedCompare(route, pack), expected);
  }
}

process.stdout.write(
  failures ? `\n[pack-version.test] ${failures} failed, ${passes} passed\n` : `[pack-version.test] ${passes} passed\n`,
);
process.exit(failures ? 1 : 0);
