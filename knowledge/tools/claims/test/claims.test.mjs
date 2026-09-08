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

import {
  declaresTestMethod,
  isTestProofId,
  parseClaims,
  resolveSymbol,
  stripCommentsAndLiterals,
} from "../lib/claims.mjs";
import { suite } from "../../lib/fixtures.mjs";
import fsSync from "node:fs";
import { fileURLToPath } from "node:url";
import nodePath from "node:path";

const { check, done } = suite("claims.test");

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

// --- a proof names a test that really runs ------------------------------------

// The checker used to search the whole test file for `method(`, so a claim whose
// assertion had been deleted kept reading as proven for as long as the name
// survived anywhere in the file — including in the comment left behind by the
// person who deleted it.
const JAVA = `
class T {
    @Test
    void declared() throws Exception {
        helper();
    }

    // Former test: removedButMentioned()
    @Test
    void other() {
        String s = "alsoMentioned() in a string";
        this.qualified();
        int n = compute(1);
    }

    private int compute(int x) { return x; }

    @TestFactory
    List<DynamicTest> generic() { return null; }

    @Test
    byte[] arrayReturn() { return null; }
}
`;

check("a declared test is accepted", declaresTestMethod(JAVA, "declared"), true);
check("a method absent from the file is rejected", declaresTestMethod(JAVA, "neverThere"), false);
check("a name only in a comment is not a test", declaresTestMethod(JAVA, "removedButMentioned"), false);
check("a name only in a string literal is not a test", declaresTestMethod(JAVA, "alsoMentioned"), false);
check("an unqualified call is not a test", declaresTestMethod(JAVA, "helper"), false);
check("a qualified call is not a test", declaresTestMethod(JAVA, "qualified"), false);
// `compute` is declared AND called here — and is still not a proof, because it is
// a helper: only a method JUnit would run can hold a claim up.
check("an unannotated helper is not a test", declaresTestMethod(JAVA, "compute"), false);
check("a generic return type still reads as a test", declaresTestMethod(JAVA, "generic"), true);
check("an array return type still reads as a test", declaresTestMethod(JAVA, "arrayReturn"), true);
check(
  "a longer identifier containing the name is not a match",
  declaresTestMethod("class T { @Test void recompute() {} }", "compute"),
  false,
);
check(
  "a name inside a block comment is not a test",
  declaresTestMethod("class T { /* @Test void ghost() {} */ }", "ghost"),
  false,
);

// The name reached a RegExp by interpolation, so a metacharacter in it matched
// more than the literal name it was meant to be.
check("a name that is a regex pattern matches nothing", declaresTestMethod(JAVA, "declare."), false);
check("a regex alternation in the name matches nothing", declaresTestMethod(JAVA, "declared|other"), false);

// A proof asserts that a named test HOLDS a claim. A method JUnit does not run
// holds nothing, so dropping the annotation or disabling the test has to read the
// same as deleting it — otherwise the check only moves the false confirmation up
// one level.
check(
  "a method whose @Test was removed no longer proves anything",
  declaresTestMethod("class T { void held() { assertTrue(x); } }", "held"),
  false,
);
check(
  "a disabled test proves nothing",
  declaresTestMethod('class T { @Test @Disabled("flaky") void held() { assertTrue(x); } }', "held"),
  false,
);
// The anchor is the TEST annotation, not any annotation: a lifecycle method or
// an override runs, but neither is a test, and neither can hold a claim.
check(
  "a lifecycle method is not a test",
  declaresTestMethod("class T { @BeforeEach void setUp() {} }", "setUp"),
  false,
);
check(
  "an overridden method is not a test",
  declaresTestMethod("class T { @Override public String toStringy() { return null; } }", "toStringy"),
  false,
);
// `@Disabled` can be written on either side of `@Test`, and on the class — all
// three mean the same thing to JUnit, so all three have to mean the same thing
// here. A run is read whole, from its first annotation, for exactly this reason.
check(
  "a disabled test proves nothing when @Disabled comes first",
  declaresTestMethod('class T { @Disabled("x") @Test void held() {} }', "held"),
  false,
);
check(
  "a test in a disabled class proves nothing",
  declaresTestMethod("@Disabled class T { @Test void held() {} }", "held"),
  false,
);
check(
  "a test in a disabled nested class proves nothing",
  declaresTestMethod("class T { @Disabled static class I { @Test void held() {} } }", "held"),
  false,
);
check(
  "a helper in a dead nested class is not a test",
  declaresTestMethod("class T { static class H { void ghost() {} } }", "ghost"),
  false,
);
check(
  "a constructor is not a test",
  declaresTestMethod("class Ghost { public Ghost(int x) {} }", "Ghost"),
  false,
);
check(
  "an annotation element is not a test",
  declaresTestMethod("@interface A { String ghost(); }", "ghost"),
  false,
);

// Every JUnit annotation that makes a method run, and the shapes that sit between
// one and the method name: further annotations, modifiers, a type-parameter list,
// a generic or annotated return type.
check(
  "@ParameterizedTest counts",
  declaresTestMethod("class T { @ParameterizedTest @ValueSource(ints = {1, 2}) void p(int n) {} }", "p"),
  true,
);
check("@RepeatedTest counts", declaresTestMethod("class T { @RepeatedTest(3) void r() {} }", "r"), true);
check("@TestTemplate counts", declaresTestMethod("class T { @TestTemplate void t() {} }", "t"), true);
check(
  "a second annotation between the anchor and the name is skipped",
  declaresTestMethod('class T { @Test @DisplayName("x") void named() {} }', "named"),
  true,
);
check(
  "modifiers between the anchor and the name are skipped",
  declaresTestMethod("class T { @Test public static void mods() {} }", "mods"),
  true,
);
check(
  "a type-parameter list is skipped",
  declaresTestMethod("class T { @Test <X> void generic(X x) {} }", "generic"),
  true,
);
check(
  "a nested generic return type is still a test",
  declaresTestMethod("class T { @TestFactory Map<K, List<V>> real() { return null; } }", "real"),
  true,
);
check(
  "a wildcard generic return type is still a test",
  declaresTestMethod("class T { @TestFactory List<? extends Number> real() { return null; } }", "real"),
  true,
);
check(
  "a type-use annotation carrying arguments is still a test",
  declaresTestMethod("class T { @TestFactory List<@Size(min = 1) String> real() { return null; } }", "real"),
  true,
);
check(
  "a test inside a @Nested class counts",
  declaresTestMethod("class T { @Nested class Inner { @Test void nested() {} } }", "nested"),
  true,
);

// Reading BACKWARD from `name(` is what the first attempt did, and it cannot be
// made right: `Map<K, V> name(` and `a < b ? c > name(1)` are the same characters,
// `?` is both a wildcard and a ternary, and `{` `}` `;` all appear inside argument
// lists. Every one of these is a CALL, and every one defeated a backward scan.
for (const [label, body] of [
  ["a lambda arrow", "list.forEach(x -> ghost(x));"],
  ["a switch arrow", "switch (v) { case A -> ghost(1); }"],
  ["a relational operator", "if (n > ghost(1)) {}"],
  ["an explicit type witness", "Collections.<String>ghost();"],
  ["a method reference with a type witness", "var f = this::<String>ghost;"],
  ["a comparison pair in an argument list", "assertThat(lo < hi && count > ghost(1)).isTrue();"],
  ["a comparison pair with a comma", "check(a < b, c > ghost(1));"],
  ["a comparison pair after a lambda block", "run(() -> { helper(); }, a < b, c > ghost(1));"],
  ["a comparison pair after an array initializer", "check(new int[]{1}, a < b, c > ghost(1));"],
  ["a comparison pair after an anonymous class", "run(new R() { int f = 1; }, a < b, c > ghost(1));"],
  ["a statement-level ternary", "Object x = a < b ? c > ghost(1) : 2;"],
  ["a ternary in a condition", "if (a < b ? c > ghost(1) : x) {}"],
]) {
  check(
    `${label} before the name is not a test`,
    declaresTestMethod(`class T { @Test void t() { ${body} } }`, "ghost"),
    false,
  );
}
check(
  "a ternary in a field initializer is not a test",
  declaresTestMethod("class T { Object f = a < b ? c > ghost(1) : 2; }", "ghost"),
  false,
);

// --- the blanking every check above rests on ---------------------------------

// A comment or a literal is blanked while every other character keeps the index
// it already had, so a match found afterwards still points at the same place in
// the real source.
const sameLength = (src) => stripCommentsAndLiterals(src).length === src.length;

check("blanking preserves offsets", sameLength('class T { String s = "x"; /* c */ }'), true);
check(
  "blanking preserves offsets over a text block",
  sameLength('class T { String s = """\nvoid ghost() {}\n"""; }'),
  true,
);
check("blanking preserves offsets over CRLF", sameLength("class T {\r\n  // c\r\n}"), true);
check("a line comment is blanked", stripCommentsAndLiterals("a // b\nc").includes("b"), false);
check(
  "an escaped quote does not end the string early",
  stripCommentsAndLiterals('String s = "a\\"b"; int real;').includes("real"),
  true,
);
check(
  "an escaped backslash does end the string",
  stripCommentsAndLiterals('String s = "a\\\\"; int real;').includes("real"),
  true,
);
check(
  "a quote inside a char literal does not open a string",
  stripCommentsAndLiterals("char c = '\"'; int real;").includes("real"),
  true,
);
check(
  "an escaped quote char literal is handled",
  stripCommentsAndLiterals("char c = '\\''; int real;").includes("real"),
  true,
);
check(
  "a comment marker inside a string is not a comment",
  stripCommentsAndLiterals('String s = "// not a comment"; int real;').includes("real"),
  true,
);
check(
  "a division is not a comment",
  stripCommentsAndLiterals("int n = a / b; int real;").includes("real"),
  true,
);
check(
  "a text block hides the code inside it",
  declaresTestMethod('class T { String s = """\n@Test void ghost() {}\n"""; }', "ghost"),
  false,
);

check("test:Class is a valid proof id", isTestProofId("LineWidthUnitsContractTest"), true);
check("test:Class#method is a valid proof id", isTestProofId("SomeTest#someMethod"), true);
check("a proof id with a regex metacharacter is refused", isTestProofId("SomeTest#a.b"), false);
check("a proof id with two hashes is refused", isTestProofId("SomeTest#a#b"), false);
check("an empty method half is refused", isTestProofId("SomeTest#"), false);

// The older `test:Class` form carries no method and must keep parsing.
check(
  "a bare test:Class proof still parses",
  claims("<!-- claim: behavior=x.y proof=test:SomeTest -->"),
  [{ kind: "behavior", value: "x.y", proof: "test:SomeTest" }],
);
check(
  "a test proof whose method half is a pattern is rejected at parse time",
  messages("<!-- claim: behavior=x.y proof=test:SomeTest#a.b -->"),
  [
    'proof "test:SomeTest#a.b" is not a test reference — expected test:Class or ' +
      "test:Class#method with Java identifiers.",
  ],
);

// --- the gate is actually wired to it ----------------------------------------

// Everything above tests the library. Nothing above notices if the gate stops
// calling it: reverting check-claims.mjs to the old `new RegExp(method)` search
// leaves every check here green and the repository check green, because the old
// search is strictly more permissive. This is the only thing standing between
// that revert and a silent return to false confirmation.
const GATE = fsSync.readFileSync(
  nodePath.join(nodePath.dirname(fileURLToPath(import.meta.url)), "..", "check-claims.mjs"),
  "utf8",
);

check("the gate asks the library about a proof method", GATE.includes("declaresTestMethod(source, method)"), true);
check("the gate builds no pattern from a proof name", /new RegExp/.test(GATE), false);

done();
