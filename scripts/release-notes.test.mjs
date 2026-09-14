/**
 * node scripts/release-notes.test.mjs — exit 0 when every case holds.
 *
 * The oversized case is the real 2.4.0 CHANGELOG section, the one GitHub refused,
 * extracted the way release.yml extracts it.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { BODY_LIMIT, releaseBody } from "./release-notes.mjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const repository = "DemchaAV/GraphCompose";

/** release.yml's awk: the "## <tag> " heading and every line up to the next "## v". */
function section(changelog, tag) {
  const out = [];
  let on = false;
  for (const line of changelog.replace(/\r\n/g, "\n").split("\n")) {
    if (line.startsWith(`## ${tag} `)) { on = true; out.push(line); continue; }
    if (on && line.startsWith("## v")) break;
    if (on) out.push(line);
  }
  return out.join("\n") + "\n";
}

const changelog = fs.readFileSync(path.join(root, "CHANGELOG.md"), "utf8");
let failures = 0;
function check(name, body) {
  try { body(); console.log(`  ok    ${name}`); }
  catch (error) { failures++; console.log(`  FAIL  ${name}\n        ${error.message.split("\n")[0]}`); }
}

// 1. A section that fits is published exactly as written.
check("a section under the limit is returned unchanged", () => {
  const notes = section(changelog, "v2.3.0");
  assert.ok(notes.length > 1000, "fixture: the v2.3.0 section was not found");
  assert.equal(releaseBody(notes, "v2.3.0", repository), notes);
});

// 2. The section GitHub refused now fits, and nothing is dropped from its list.
const big = section(changelog, "v2.4.0");
const body = releaseBody(big, "v2.4.0", repository);
check("fixture: the v2.4.0 section is over the limit", () => {
  assert.ok(big.length > BODY_LIMIT, `v2.4.0 is ${big.length} characters — the oversized case no longer exercises anything`);
});
check("the condensed body fits a GitHub Release", () => {
  assert.ok(body.length <= BODY_LIMIT, `${body.length} characters`);
  assert.ok(body.length < 125_000);
});
check("it opens with the section heading", () => {
  assert.equal(body.split("\n")[0], big.split("\n")[0]);
});
check("every subsection heading survives, in order", () => {
  const headings = (text) => text.split("\n").filter((l) => l.startsWith("### "));
  assert.deepEqual(headings(body), headings(big));
});
check("every entry survives as one line", () => {
  const entries = (text) => text.split("\n").filter((l) => l.startsWith("- **")).length;
  assert.equal(entries(body), entries(big));
});
check("every entry keeps its bold lead", () => {
  for (const line of body.split("\n").filter((l) => l.startsWith("- "))) {
    assert.match(line, /^- \*\*.+\*\*/, line);
  }
});
check("a lead that is only a name carries its first sentence", () => {
  const line = body.split("\n").find((l) => l.startsWith("- **`SvgGlyph.fromFile(Path)`"));
  assert.ok(line, "the SvgGlyph.fromFile entry is missing");
  assert.match(line, /^- \*\*`SvgGlyph\.fromFile\(Path\)`\.\*\* The classpath variant covers/);
});
check("it links to the full section at the tag", () => {
  assert.ok(body.includes("(https://github.com/DemchaAV/GraphCompose/blob/v2.4.0/CHANGELOG.md)"));
});

// 3. A lead with no closing marker is published as its first line, not dropped.
check("an unterminated lead is kept as written", () => {
  const notes = "## v9.9.9 — Planned\n\n### Public API\n\n- **Broken lead without a close\n  continuation\n" + "x".repeat(BODY_LIMIT);
  const out = releaseBody(notes, "v9.9.9", repository);
  assert.ok(out.includes("- **Broken lead without a close\n"));
  assert.ok(out.length <= BODY_LIMIT);
});

// 4. The tag's workflow still runs it, between the extraction and the Release.
check("release.yml fits the notes after extracting them and before publishing", () => {
  const workflow = fs.readFileSync(path.join(root, ".github/workflows/release.yml"), "utf8");
  const extract = workflow.indexOf("' CHANGELOG.md > \"${NOTES_FILE}\"");
  const fit = workflow.search(/^\s*node scripts\/release-notes\.mjs "\$\{NOTES_FILE\}"/m);
  const publish = workflow.indexOf("gh release create");
  assert.ok(extract >= 0 && publish >= 0, "release.yml no longer extracts the section or creates the Release the way this test reads it");
  assert.ok(fit > extract && fit < publish, "release.yml does not run scripts/release-notes.mjs between the extraction and gh release create");
});

if (failures > 0) {
  console.log(`release-notes: ${failures} case(s) failed`);
  process.exit(1);
}
console.log("release-notes: all cases passed");
