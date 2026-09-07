#!/usr/bin/env node
/**
 * knowledge/tools/routing/test/anchors.test.mjs — the anchor rule the routing
 * gate resolves references with.
 *
 *   node knowledge/tools/routing/test/anchors.test.mjs
 *
 * Exit 0 all passed · 1 a case failed.
 *
 * Every case here is one the gate previously got wrong, in one of the two
 * directions that matter:
 *
 *   accepts too little — CI red on a reference that works in a browser;
 *   accepts too much   — CI green on a reference that 404s.
 *
 * The second is the dangerous one, and it is the reason this file exists at all:
 * a gate that accepts everything reports zero problems for ever, which reads
 * exactly like a gate that is working. Stub `anchorRefsIn` to return `[]` and
 * `check-routes` still prints "5 routes hold up" — nothing but a fixture notices.
 */

import { anchorOf, anchorsIn, anchorRefsIn } from "../lib/anchors.mjs";
import { suite } from "../../lib/fixtures.mjs";

const { check, done } = suite("anchors.test");

// --------------------------------------------------------------- anchorOf ---

check("a plain heading lowercases and hyphenates", anchorOf("The four tools"), "the-four-tools");

// The divergence that motivated the module. House style spaces its em dashes,
// so GitHub sees two whitespace characters and emits two hyphens. Collapsing
// runs produced a single hyphen and disagreed with the rendered page on 121 of
// the 786 headings under docs/.
check(
  "a spaced em dash yields two hyphens, as on GitHub",
  anchorOf("Zebra — alternating row fills"),
  "zebra--alternating-row-fills",
);
check(
  "so does any other double space",
  anchorOf("Row span  merge a cell"),
  "row-span--merge-a-cell",
);


// Every expected value below was read off GitHub, by posting the heading to
// api.github.com/markdown and taking the id it generated — not derived from the
// implementation, and not reasoned about. Two earlier versions of this rule were
// wrong precisely because they were reasoned about: one collapsed whitespace
// runs, the next hyphenated all Unicode whitespace and kept non-decimal
// numerals. GitHub does none of those things.
//
// The same table is asserted from the Java twin
// (DocumentationLinkGuardTest.theAnchorRuleAgreesWithItsJavaScriptTwin), which
// the markdown link guard resolves with; neither can call the other across the
// language boundary, so change one rule and its own fixture goes red. Escapes,
// not literals: these characters are invisible or indistinguishable in an editor.
check(
  "a non-breaking space is dropped, not hyphenated",
  anchorOf("Zebra alternating rows"),
  "zebraalternating-rows",
);
check("an em space is dropped too", anchorOf("Row span"), "rowspan");
check("and a tab — only the ASCII space becomes a hyphen", anchorOf("A\tB"), "ab");
check(
  "a circled numeral is not a decimal digit, so it goes and its spaces stay",
  anchorOf("Item ① first"),
  "item--first",
);
check(
  "a decomposed accent is a combining mark and stays with its letter",
  anchorOf("Café rules"),
  "café-rules",
);
check("letters of any script survive", anchorOf("Привет мир"), "привет-мир");
check("a letter numeral is kept, unlike the circled one above", anchorOf("Chapter Ⅰ here"), "chapter-ⅰ-here");
check("connector punctuation is kept — that is where the underscore lives", anchorOf("a‿f b"), "a‿f-b");
check(
  "a non-ASCII space at an edge is dropped and its ASCII neighbour still hyphenates",
  anchorOf("  A"),
  "-a",
);
check("the same at the trailing edge", anchorOf("A  "), "a-");
check("so do digits, while the dots between them go", anchorOf("v1.8.0 fonts"), "v180-fonts");
check("an underscore survives", anchorOf("snake_case name"), "snake_case-name");
check("a dropped character leaves its spaces behind", anchorOf("A & B"), "a--b");

check("punctuation is dropped, not hyphenated", anchorOf("Sidebar: page background vs. row"), "sidebar-page-background-vs-row");
check("backticks are dropped", anchorOf("`fill()` and the slot"), "fill-and-the-slot");
check("a link in a heading contributes its text only", anchorOf("See [the guide](x.md)"), "see-the-guide");
check("html is stripped", anchorOf("A <em>strong</em> claim"), "a-strong-claim");
check("trailing space does not become a hyphen", anchorOf("  Trimmed  "), "trimmed");

// --------------------------------------------------------------- anchorsIn ---

check(
  "headings become anchors",
  [...anchorsIn("# One\n\n## Two words\n")],
  ["one", "two-words"],
);

// A `#` line inside a fence renders as code. Treating it as a heading let a
// route cite a phantom anchor and pass: docs/templates/v2-layered/
// contributor-guide.md has `# 3. Normal run (...)` inside a bash fence.
check(
  "a comment inside a fenced block is not a heading",
  [...anchorsIn("# Real\n\n```bash\n# 3. Normal run (defends against drift):\nmvn verify\n```\n")],
  ["real"],
);
check(
  "an indented fence is still a fence",
  [...anchorsIn("# Real\n\n  ```\n  # not a heading\n  ```\n")],
  ["real"],
);

check(
  "a repeated heading gets GitHub's numeric suffix",
  [...anchorsIn("## Notes\n\n## Notes\n\n## Notes\n")],
  ["notes", "notes-1", "notes-2"],
);

check(
  "a hand-written anchor counts",
  [...anchorsIn("<a name=\"legacy-id\"></a>\n\n# Title\n")],
  ["title", "legacy-id"],
);

// ------------------------------------------------------------ anchorRefsIn ---

check(
  "a repo-relative reference is found",
  anchorRefsIn("see docs/recipes/tables.md#zebra for the rest"),
  ["docs/recipes/tables.md#zebra"],
);
check(
  "several references in one field are all found",
  anchorRefsIn("docs/a.md#one and docs/b.md#two"),
  ["docs/a.md#one", "docs/b.md#two"],
);
check(
  "a reference inside a markdown link is found",
  anchorRefsIn("[the guide](docs/recipes/tables.md#zebra)"),
  ["docs/recipes/tables.md#zebra"],
);

// The URL case. Without the leading boundary a match starts after the colon and
// yields `//github.com/...`, which fails existsSync — so the gate rejects the
// route for "no such page" when the author wrote a perfectly good link.
check(
  "an absolute URL is not mistaken for a repo path",
  anchorRefsIn("https://github.com/DemchaAV/GraphCompose/blob/develop/docs/recipes/tables.md#zebra"),
  [],
);
check(
  "nor is a bare host path",
  anchorRefsIn("see //cdn.example.com/docs/x.md#y"),
  [],
);
// The matcher has to accept what anchorOf can emit. An ASCII-only anchor group
// truncated this to "docs/recipes/tables.md#caf", and the gate then rejected the
// route as "a heading was renamed" for a link that opens.
check(
  "a non-ASCII anchor is matched whole",
  anchorRefsIn("see docs/recipes/tables.md#café-rules"),
  ["docs/recipes/tables.md#café-rules"],
);
check(
  "and one in another script",
  anchorRefsIn("see docs/recipes/tables.md#привет-мир"),
  ["docs/recipes/tables.md#привет-мир"],
);

check("a field with no reference yields none", anchorRefsIn("plain prose, no citation"), []);
check("a null field is not an error", anchorRefsIn(null), []);

done();
