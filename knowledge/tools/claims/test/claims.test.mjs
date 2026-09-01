#!/usr/bin/env node
/**
 * knowledge/tools/claims/test/claims.test.mjs — the claim parser and the symbol
 * resolver.
 *
 *   node knowledge/tools/claims/test/claims.test.mjs
 *
 * Exit 0 all passed · 1 a case failed.
 *
 * The checker's value is entirely in what it *rejects*, so most of these are
 * negative cases. A parser that accepts everything and an index that resolves
 * everything would both report zero problems for ever, which is
 * indistinguishable from working.
 */

import { parseClaims, resolveSymbol } from "../lib/claims.mjs";

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

const parse = (body) => parseClaims(body, "x.md");
const messages = (body) => parse(body).errors.map((e) => e.message);
const claims = (body) => parse(body).claims.map(({ kind, value, proof }) => ({ kind, value, proof }));

// --- parsing -----------------------------------------------------------------

check(
  "a symbol claim parses",
  claims("<!-- claim: symbol=TableBuilder.zebra -->"),
  [{ kind: "symbol", value: "TableBuilder.zebra", proof: null }],
);

check(
  "a behaviour claim keeps its proof",
  claims("<!-- claim: behavior=table.zebra-loses proof=test:SomeTest -->"),
  [{ kind: "behavior", value: "table.zebra-loses", proof: "test:SomeTest" }],
);

// A behaviour is the one kind nothing else can hold up: it is invisible in every
// signature, so without a proof it is an opinion in an HTML comment.
check(
  "a behaviour without a proof is rejected",
  messages("<!-- claim: behavior=table.zebra-loses -->"),
  ['behavior claim "table.zebra-loses" needs a proof='],
);

check(
  "a capability needs no proof",
  claims("<!-- claim: capability=table.zebra -->"),
  [{ kind: "capability", value: "table.zebra", proof: null }],
);

// Two kinds in one marker read as one fact and are really two; the reverse index
// would then attribute a page to an intent it only mentioned in passing.
check(
  "one marker may not carry two kinds",
  messages("<!-- claim: symbol=A.b capability=c.d -->"),
  ["claim mixes symbol and capability — use one marker each"],
);

check("an empty value is rejected", messages("<!-- claim: symbol= -->"), ["symbol= is empty"]);

check(
  "a proof must name a scheme",
  messages("<!-- claim: behavior=a.b proof=wibble -->"),
  ['proof must be snippet:|probe:|render:|test:<id>, got "wibble"'],
);

check("a comment that is not a claim is ignored", claims("<!-- doc-example: id=x -->"), []);
check("prose mentioning claim: is ignored", claims("Write `claim:` in prose."), []);

// The nearest heading is carried so a report can say where in the page a claim
// sits, without the claim itself holding an anchor that breaks when the heading
// is edited.
check(
  "a claim records the heading above it",
  parse("# Top\n\n## Zebra\n\n<!-- claim: capability=x.y -->").claims.map((c) => c.heading),
  ["Zebra"],
);

// --- resolution --------------------------------------------------------------

const index = {
  types: new Map([
    ["TableBuilder", {
      name: "TableBuilder", binaryName: "com.demcha.TableBuilder", surface: "authoring",
      stability: "stable",
      methods: [{ name: "zebra" }, { name: "zebra" }, { name: "headerRow" }],
      constants: ["DEFAULT_GAP"],
    }],
    ["GraphCompose", {
      name: "GraphCompose", binaryName: "com.demcha.GraphCompose", surface: "authoring",
      stability: "stable", methods: [{ name: "document" }], constants: [],
    }],
    ["GraphCompose.DocumentBuilder", {
      name: "GraphCompose.DocumentBuilder", binaryName: "com.demcha.GraphCompose$DocumentBuilder",
      surface: "authoring", stability: "stable", methods: [{ name: "pageSize" }], constants: [],
    }],
  ]),
};

check("a member resolves", resolveSymbol(index, "TableBuilder.zebra").found, true);
check("both overloads come back", resolveSymbol(index, "TableBuilder.zebra").overloads.length, 2);
check("a constant resolves", resolveSymbol(index, "TableBuilder.DEFAULT_GAP").constant, true);
check("a bare type resolves", resolveSymbol(index, "TableBuilder").found, true);

check(
  "a missing member is refused, and says so about the member",
  resolveSymbol(index, "TableBuilder.noSuch").reason,
  'no member "noSuch" on TableBuilder',
);
check("a missing type is refused", resolveSymbol(index, "NoSuchType.method").found, false);

// The defect that motivated this entire pack: splitting on the first dot answers
// about `GraphCompose` for a symbol whose receiver is the nested builder — a
// confident, authoritative, wrong answer from the one tool whose "no" must hold.
check(
  "a nested receiver resolves to the nested type, not its outer",
  // `?.` deliberately: when this regresses the resolver returns a shape with no
  // type at all, and a test that dies on a null dereference reports a stack
  // trace where it should report which receiver it got.
  resolveSymbol(index, "GraphCompose.DocumentBuilder.pageSize").type?.name ?? null,
  "GraphCompose.DocumentBuilder",
);
check(
  "the outer type is not credited with the nested member",
  resolveSymbol(index, "GraphCompose.pageSize").found,
  false,
);

process.stdout.write(
  failures ? `\n[claims.test] ${failures} failed, ${passes} passed\n` : `[claims.test] ${passes} passed\n`,
);
process.exit(failures ? 1 : 0);
