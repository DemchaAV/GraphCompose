#!/usr/bin/env node
/**
 * scripts/release-notes.mjs — fit a CHANGELOG section into a GitHub Release body.
 *
 *   node scripts/release-notes.mjs <notes-file> <tag> <repository>
 *
 * Exit 0 the file fits (rewritten or untouched) · 2 usage.
 *
 * release.yml publishes a version's CHANGELOG section as the body of its GitHub
 * Release, and GitHub refuses a body longer than 125 000 characters with HTTP 422.
 * The 2.4.0 section was 156 808, so the tag's Release step failed after the build
 * and the knowledge bundle had both passed.
 *
 * A section that fits is left byte for byte. One that does not is rewritten as its
 * heading, every `###` subsection, and each entry cut to its bold lead sentence —
 * the line the CHANGELOG already writes to be read on its own — followed by a link
 * to the full section at the tag. Every entry stays listed; only the explanation
 * under it moves behind the link. An entry whose bold lead is only a name
 * (`SvgGlyph.fromFile(Path)`) keeps the rest of its first sentence, since the name
 * alone says nothing.
 */
import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

/** GitHub's limit is 125 000; the margin absorbs the counting difference for astral characters. */
export const BODY_LIMIT = 120_000;

/**
 * The body to publish for a CHANGELOG section.
 *
 * @param {string} notes the extracted section, heading first
 * @param {string} tag the tag being released, e.g. v2.4.0
 * @param {string} repository owner/name, e.g. DemchaAV/GraphCompose
 * @returns {string} {@code notes} itself when it fits, otherwise the condensed body
 */
export function releaseBody(notes, tag, repository) {
  if (notes.length <= BODY_LIMIT) {
    return notes;
  }
  const lines = notes.replace(/\r\n/g, "\n").split("\n");
  const out = [lines[0], ""];
  let i = 1;
  while (i < lines.length) {
    const line = lines[i];
    if (line.startsWith("### ")) {
      out.push("", line, "");
      i++;
      continue;
    }
    if (line.startsWith("- **")) {
      const [lead, next] = leadOf(lines, i);
      out.push(`- ${lead}`);
      i = next;
      continue;
    }
    i++;
  }
  const url = `https://github.com/${repository}/blob/${tag}/CHANGELOG.md`;
  out.push(
    "",
    "---",
    "",
    `Each entry above is the first line of its CHANGELOG entry. The full notes — what changed, why, and how it was verified — are longer than a GitHub Release body allows; read them in [CHANGELOG.md at ${tag}](${url}).`,
  );
  return out.join("\n").replace(/\n{3,}/g, "\n\n") + "\n";
}

/** The bold lead of the entry starting at {@code start}, and the index after the lines it read. */
function leadOf(lines, start) {
  let text = lines[start].slice(2);
  let next = start + 1;
  const continues = () => next < lines.length && lines[next].startsWith("  ") && lines[next].trim() !== "<br><br>";

  while (text.indexOf("**", 2) < 0 && continues()) {
    text += " " + lines[next].trim();
    next++;
  }
  const close = text.indexOf("**", 2);
  if (close < 0) {
    // No closing marker: publish the first line as written rather than guess.
    return [lines[start].slice(2), start + 1];
  }
  let lead = text.slice(0, close + 2);
  const nameOnly = /^\*\*`[^`]+`\.?\*\*$/.test(lead);
  if (nameOnly || !/[.:!?]\*\*$/.test(lead)) {
    let rest = text.slice(close + 2);
    while (!/[.:](\s|$)/.test(rest) && continues()) {
      rest += " " + lines[next].trim();
      next++;
    }
    const stop = rest.search(/[.:](\s|$)/);
    lead += (stop < 0 ? rest : rest.slice(0, stop + 1)).replace(/:$/, ".");
  }
  // Skip the remainder of this entry.
  while (continues()) {
    next++;
  }
  return [lead, next];
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  const [file, tag, repository] = process.argv.slice(2);
  if (!file || !tag || !repository) {
    console.error("usage: node scripts/release-notes.mjs <notes-file> <tag> <owner/repository>");
    process.exit(2);
  }
  const notes = fs.readFileSync(file, "utf8");
  const body = releaseBody(notes, tag, repository);
  if (body === notes) {
    console.log(`[release-notes] ${notes.length} characters — published as written`);
  } else {
    fs.writeFileSync(file, body);
    console.log(`::notice::CHANGELOG section for ${tag} is ${notes.length} characters, over the ${BODY_LIMIT} a Release body can take; published its ${(body.match(/^- /gm) || []).length} entry leads (${body.length} characters) with a link to the full notes.`);
  }
}
