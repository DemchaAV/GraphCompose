#!/usr/bin/env node
/**
 * knowledge/tools/api-surface/test/stability-doc.test.mjs — which `@Beta`
 * markers the stability document has to name.
 *
 *   node knowledge/tools/api-surface/test/stability-doc.test.mjs
 *
 * Exit 0 all passed · 1 a case failed.
 *
 * The gate has two ways to become useless and a green run tells them apart from
 * working in neither direction. Report too much and the document grows a line
 * per inherited marker — fifteen PPTX handlers instead of the one package that
 * declares them — until nobody finishes reading it. Report too little and it
 * passes for ever, which is what it looked like before the gate existed. So the
 * cases below come in pairs: what must be named, and what must not be demanded.
 */

import { suite } from "../../lib/fixtures.mjs";

import { originatingBetas, unnamedOrigins, countByKind } from "../lib/stability-doc.mjs";

const { check, done } = suite("stability-doc.test");

/** A surface document with one package, spelled the way `extract-api` emits it. */
const surface = (pkg) => ({ packages: [pkg] });
const names = (origins) => origins.map((o) => `${o.kind}:${o.what}`).sort();

// --- what originates a marker ------------------------------------------------

check(
  "a beta package is an origin",
  names(originatingBetas([surface({ name: "a.b.pptx", stability: "beta", types: [] })])),
  ["package:a.b.pptx"],
);

// The inheritance case the document's readability depends on: the package entry
// already tells the reader everything its types are.
check(
  "a type inside a beta package is not an origin",
  names(
    originatingBetas([
      surface({
        name: "a.b.pptx",
        stability: "beta",
        types: [{ name: "PptxImageHandler", stability: "beta", members: [] }],
      }),
    ]),
  ),
  ["package:a.b.pptx"],
);

check(
  "a beta type in a stable package is an origin",
  names(
    originatingBetas([
      surface({ name: "a.b", types: [{ name: "NodeDefinition", stability: "beta", members: [] }] }),
    ]),
  ),
  ["type:NodeDefinition"],
);

check(
  "a member of a beta type is not an origin",
  names(
    originatingBetas([
      surface({
        name: "a.b",
        types: [
          { name: "NodeDefinition", stability: "beta", members: [{ name: "emit", stability: "beta" }] },
        ],
      }),
    ]),
  ),
  ["type:NodeDefinition"],
);

// The case that started this: `PdfFixedLayoutBackend` is Stable, four of its
// members are not, and nothing named them.
check(
  "a beta member of a stable type is an origin, qualified by its type",
  names(
    originatingBetas([
      surface({
        name: "a.b",
        types: [
          {
            name: "PdfFixedLayoutBackend",
            members: [
              { name: "renderSections", stability: "beta" },
              { name: "writeSections", stability: "beta" },
              { name: "build" },
            ],
          },
        ],
      }),
    ]),
  ),
  ["member:PdfFixedLayoutBackend.renderSections", "member:PdfFixedLayoutBackend.writeSections"],
);

check(
  "a surface with nothing beta originates nothing",
  originatingBetas([surface({ name: "a.b", types: [{ name: "Stable", members: [{ name: "x" }] }] })]),
  [],
);

// A package admitted by two surfaces (`…fixed.pptx` is in both `backends` and
// `extension-spi`) is one thing to name, not two.
check(
  "the same package in two surfaces is one origin",
  names(
    originatingBetas([
      surface({ name: "a.b.pptx", stability: "beta", types: [] }),
      surface({ name: "a.b.pptx", stability: "beta", types: [] }),
    ]),
  ),
  ["package:a.b.pptx"],
);

// --- what counts as named ----------------------------------------------------

const origins = [
  { what: "com.demcha.a.pptx", kind: "package" },
  { what: "NodeDefinition", kind: "type" },
];

check(
  "a fully-qualified mention names it",
  unnamedOrigins(origins, "the com.demcha.a.pptx package, and NodeDefinition"),
  [],
);

// The document writes `…pptx.handlers` and `toPptxBytes`; demanding the binary
// name would fail on prose that is perfectly clear.
check(
  "a short-form mention names it",
  unnamedOrigins(origins, "the `…a.pptx` package moves, and so does NodeDefinition"),
  [],
);

check(
  "an unmentioned origin is reported",
  unnamedOrigins(origins, "NodeDefinition is experimental").map((o) => o.what),
  ["com.demcha.a.pptx"],
);

// Sorted so a failing run reads the same twice and diffs cleanly.
check(
  "the report is sorted by name",
  unnamedOrigins(
    [
      { what: "Zebra", kind: "type" },
      { what: "Alpha", kind: "type" },
      { what: "Middle", kind: "type" },
    ],
    "",
  ).map((o) => o.what),
  ["Alpha", "Middle", "Zebra"],
);

// A substring match is what makes short forms work, and it is also its limit:
// this is a reminder that the gate proves a name is present, not that the prose
// around it is true.
check(
  "a name that is a substring of a longer word counts as named",
  unnamedOrigins([{ what: "Beta", kind: "type" }], "BetaAnnotationDocumentationTest"),
  [],
);

// --- the summary line --------------------------------------------------------

check(
  "kinds are counted separately",
  countByKind([
    { what: "p", kind: "package" },
    { what: "q", kind: "package" },
    { what: "T", kind: "type" },
  ]),
  { package: 2, type: 1 },
);

check("counting nothing is an empty tally", countByKind([]), {});

done();
