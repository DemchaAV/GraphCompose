#!/usr/bin/env node
/**
 * knowledge/tools/api-surface/test/classifier.test.mjs — the classification
 * ladder, against classes this test compiles.
 *
 *   node knowledge/tools/api-surface/test/classifier.test.mjs
 *
 * Exit 0 all passed · 1 a case failed · 4 the toolchain is unusable.
 *
 * Why fixtures rather than assertions about the real surface: a test that
 * asserts `SvgIcon` is beta passes for as long as nobody edits `SvgIcon`, and
 * says nothing about the rung of the ladder it happens to exercise. These
 * fixtures are written for the rungs, so a rung that stops working fails here
 * with a name that says which one.
 *
 * Why compiled during the test rather than committed `.class` files: a checked-in
 * class file is a binary no reviewer can read, goes stale against the next
 * class-file version, and cannot be diffed when it starts failing. `javac` is
 * already required — the extractor needs `javap` from the same JDK.
 *
 * The real `@Internal` and `@Beta` are used, resolved from the built
 * `core/target/classes`, so this exercises the annotations the library actually
 * ships rather than look-alikes that could drift apart from them.
 */

import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

import { findJavap } from "../lib/javap.mjs";
import { readAnnotations, memberKey, descriptorParamTypes } from "../lib/annotations.mjs";
import { admit, stability, memberStability, INTERNAL, BETA } from "../lib/surfaces.mjs";
import { openDir } from "../lib/tree.mjs";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(HERE, "..", "..", "..", "..");
const CORE_CLASSES = path.join(REPO_ROOT, "core", "target", "classes");

const PKG = "fixture.knowledge";
const INTERNAL_PKG = "fixture.knowledge.sealed";

// --- the fixtures ------------------------------------------------------------

const SOURCES = {
  // A plain public type: admitted by a surface rule, stable.
  [`${PKG}/PlainType.java`]: `package ${PKG};
public final class PlainType {
    public String plain() { return "x"; }
}`,

  // Type-level @Beta, and a nested type that is also @Beta — the receiver of a
  // nested annotation is spelled Outer$Inner, which is the case that broke the
  // regex generator this pack replaced.
  [`${PKG}/BetaType.java`]: `package ${PKG};
import com.demcha.compose.document.api.Beta;
@Beta
public final class BetaType {
    public String betaMember() { return "x"; }
    @Beta
    public record NestedBeta(int n) {}
    public record NestedPlain(int n) {}
}`,

  // A stable type carrying one @Beta member and one @Internal member, plus two
  // overloads that share a name and an arity and differ only in parameter type —
  // the ShapeContainerBuilder.path shape. The annotation is written fully
  // qualified on one of them, as ShapeContainerBuilder writes it.
  [`${PKG}/MixedMembers.java`]: `package ${PKG};
import com.demcha.compose.document.api.Beta;
import com.demcha.compose.document.api.Internal;
import java.util.List;
public final class MixedMembers {
    public String stableMember() { return "x"; }
    @Beta
    public String betaMember() { return "x"; }
    @Internal
    public String internalMember() { return "x"; }
    public String overloaded(double a, double b, List<String> c) { return "list"; }
    @com.demcha.compose.document.api.Beta
    public String overloaded(double a, double b, CharSequence c) { return "chars"; }
}`,

  // A package marked @Internal, holding an unannotated type and a @Beta type.
  // The second is the NodeDefinition shape: deliberately opened inside a package
  // that is otherwise closed.
  [`${INTERNAL_PKG}/package-info.java`]: `@com.demcha.compose.document.api.Internal
package ${INTERNAL_PKG};`,

  [`${INTERNAL_PKG}/HiddenType.java`]: `package ${INTERNAL_PKG};
public final class HiddenType {
    public String hidden() { return "x"; }
}`,

  [`${INTERNAL_PKG}/OpenedSpi.java`]: `package ${INTERNAL_PKG};
import com.demcha.compose.document.api.Beta;
@Beta
public interface OpenedSpi {
    String implementMe();
}`,
};

// --- harness -----------------------------------------------------------------

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

function compileFixtures(outDir) {
  const srcDir = path.join(outDir, "src");
  for (const [relative, body] of Object.entries(SOURCES)) {
    const file = path.join(srcDir, ...relative.split("/"));
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, `${body}\n`, "utf8");
  }

  const classesDir = path.join(outDir, "classes");
  fs.mkdirSync(classesDir, { recursive: true });

  const javac = path.join(path.dirname(findJavap()), process.platform === "win32" ? "javac.exe" : "javac");
  const files = Object.keys(SOURCES).map((r) => path.join(srcDir, ...r.split("/")));
  const run = spawnSync(javac, ["-nowarn", "-cp", CORE_CLASSES, "-d", classesDir, ...files], {
    encoding: "utf8",
  });
  if (run.status !== 0) {
    process.stderr.write(`[classifier.test] javac failed:\n${run.stderr || run.stdout}\n`);
    process.exit(4);
  }
  return classesDir;
}

/** The annotation facts for one compiled fixture, keyed as the classifier sees them. */
function factsFor(classesDir, binaryName) {
  const entry = `${binaryName.replace(/\./g, "/")}.class`;
  return readAnnotations(openDir(classesDir).read(entry));
}

function packageAnnotationsFor(classesDir, packageName) {
  const entry = `${packageName.replace(/\./g, "/")}/package-info.class`;
  try {
    return readAnnotations(openDir(classesDir).read(entry)).type;
  } catch {
    return [];
  }
}

/** Run one type through stages A and B exactly as the extractor does. */
function classify(classesDir, binaryName, packageName) {
  const annotations = factsFor(classesDir, binaryName);
  const type = {
    binaryName,
    packageName,
    annotations: annotations.type,
    packageAnnotations: packageAnnotationsFor(classesDir, packageName),
  };
  const verdict = admit(type);
  return {
    verdict,
    stability: verdict.admitted ? stability(type) : null,
    methods: annotations.methods,
    ambiguous: annotations.ambiguous,
  };
}

// --- cases -------------------------------------------------------------------

function run() {
  if (!fs.existsSync(CORE_CLASSES)) {
    process.stderr.write(
      `[classifier.test] ${path.relative(REPO_ROOT, CORE_CLASSES)} is not built.\n` +
        "  The fixtures compile against the real @Internal / @Beta, so core must be built first.\n",
    );
    process.exit(4);
  }

  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), "gc-classifier-"));
  try {
    const classes = compileFixtures(tmp);

    // The fixture package matches no surface rule, so admission has to be forced
    // for the cases that are about stability rather than about admission. A
    // rule-matching package would make these tests depend on the surface list.
    const asIfAdmitted = (binaryName, packageName) => {
      const annotations = factsFor(classes, binaryName);
      return {
        annotations: annotations.type,
        packageAnnotations: packageAnnotationsFor(classes, packageName),
        methods: annotations.methods,
        ambiguous: annotations.ambiguous,
      };
    };

    // A6 — a type matching no rule is unclassified, which is what makes the
    // extractor exit non-zero rather than quietly shipping a smaller surface.
    check(
      "A6 unruled package is unclassified (not merely excluded)",
      classify(classes, `${PKG}.PlainType`, PKG).verdict,
      { admitted: false, reason: null },
    );

    // A3 — a package-@Internal type with no annotation of its own.
    check(
      "A3 package @Internal excludes an unannotated type",
      classify(classes, `${INTERNAL_PKG}.HiddenType`, INTERNAL_PKG).verdict,
      { admitted: false, reason: `package @Internal (${INTERNAL_PKG})` },
    );

    // A1 — the type's own @Internal, tested through a member below; here the
    // package-level path is the one that must not swallow an SPI type.
    // A2 beats A3: without an SPI rule this @Beta type stays out.
    check(
      "A2 absent: @Beta inside an @Internal package is NOT admitted by its @Beta",
      classify(classes, `${INTERNAL_PKG}.OpenedSpi`, INTERNAL_PKG).verdict,
      { admitted: false, reason: `package @Internal (${INTERNAL_PKG})` },
    );

    // B1 — type-level @Beta.
    check(
      "B1 type-level @Beta",
      stability(asIfAdmitted(`${PKG}.BetaType`, PKG)),
      "beta",
    );

    // B1 on a NESTED type, whose annotation receiver is Outer$Inner. Its
    // enclosing type is beta here, but the point is that the nested class file
    // carries its own annotation and is read separately.
    check(
      "B1 nested @Beta (receiver is Outer$Inner)",
      stability(asIfAdmitted(`${PKG}.BetaType$NestedBeta`, PKG)),
      "beta",
    );

    // B3 — a nested type with no annotation of its own is not beta because its
    // enclosing type is. Stability is per type, never per file.
    check(
      "B3 nested type is NOT infected by its enclosing type",
      stability(asIfAdmitted(`${PKG}.BetaType$NestedPlain`, PKG)),
      "stable",
    );

    // B2 — package-level @Beta would be inherited. The fixture package carries
    // @Internal rather than @Beta, so this asserts the mechanism directly.
    check(
      "B2 package annotation is visible to the classifier",
      packageAnnotationsFor(classes, INTERNAL_PKG),
      [INTERNAL],
    );

    // B3 — a plain type.
    check("B3 unannotated type is stable", stability(asIfAdmitted(`${PKG}.PlainType`, PKG)), "stable");

    // --- Stage C ---
    const mixed = asIfAdmitted(`${PKG}.MixedMembers`, PKG);
    const memberAnnotations = (name, params) => mixed.methods.get(memberKey(name, params)) ?? [];

    check(
      "C1 member @Internal is dropped",
      memberStability(memberAnnotations("internalMember", []), "stable"),
      null,
    );
    check(
      "C2 member @Beta on a stable type",
      memberStability(memberAnnotations("betaMember", []), "stable"),
      "beta",
    );
    check(
      "C3 unannotated member inherits the type",
      memberStability(memberAnnotations("stableMember", []), "beta"),
      "beta",
    );
    check(
      "C3 unannotated member of a stable type stays stable",
      memberStability(memberAnnotations("stableMember", []), "stable"),
      "stable",
    );

    // The overload pair: same name, same arity, one annotated — and the
    // annotation written fully qualified, as ShapeContainerBuilder writes it.
    check(
      "C2 overload discrimination: List overload is stable",
      memberStability(memberAnnotations("overloaded", ["double", "double", "List<String>"]), "stable"),
      "stable",
    );
    check(
      "C2 overload discrimination: CharSequence overload is beta",
      memberStability(memberAnnotations("overloaded", ["double", "double", "CharSequence"]), "stable"),
      "beta",
    );
    check(
      "erased-type keying leaves no ambiguity to guess about",
      mixed.ambiguous,
      [],
    );

    // The keying itself, since both sides of it have to agree for the above to
    // mean anything.
    check(
      "descriptor erasure matches javap's rendering",
      memberKey("overloaded", descriptorParamTypes("(DDLjava/util/List;)Ljava/lang/String;")),
      memberKey("overloaded", ["double", "double", "List<String>"]),
    );

    // The annotation constants the ladder is keyed on must be the ones the
    // library ships; a rename would otherwise silently classify everything stable.
    check("@Internal binary name is the shipped one", INTERNAL, "com.demcha.compose.document.api.Internal");
    check("@Beta binary name is the shipped one", BETA, "com.demcha.compose.document.api.Beta");
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }

  process.stdout.write(
    failures
      ? `\n[classifier.test] ${failures} failed, ${passes} passed\n`
      : `[classifier.test] ${passes} passed\n`,
  );
  process.exit(failures ? 1 : 0);
}

run();
