/**
 * knowledge/tools/api-surface/lib/annotations.mjs — read `@Internal` / `@Beta`
 * off a compiled class.
 *
 * Why not source text: the same annotation is written two ways in this
 * codebase. `document/layout/package-info.java` writes
 * `@com.demcha.compose.document.api.Internal` fully qualified;
 * `dsl/ShapeContainerBuilder.java` writes `@com.demcha...Beta` on a method the
 * same way, while `svg/SvgIcon.java` writes a bare `@Beta`. A grep for
 * `@Internal` finds one of the five annotated package-infos; a grep for
 * `api\.Internal` finds six, because `document/api/package-info.java` merely
 * *mentions* the annotation in a Javadoc link. One pattern under-counts, the
 * other over-counts, and neither can tell an annotation from a sentence about
 * one. A class file has no such ambiguity: the descriptor is one normalised
 * binary name.
 *
 * Why not `javap -v`: it does print annotations, but only inside a full
 * disassembly — 49 000 lines per 120 classes, with the annotation's own
 * descriptor also appearing in the constant-pool dump, so a line-based read has
 * to distinguish a declaration from its own string table while tracking which
 * member block it is inside. Reading the class file directly is exact, is about
 * a hundred times cheaper, and every attribute carries its own length, so
 * everything uninteresting is skipped without being understood.
 *
 * Scope: `RuntimeVisibleAnnotations` on the class and on its methods. That is
 * all the classifier consults, and both `@Internal` and `@Beta` are
 * `@Retention(RUNTIME)`, so both are there.
 */

/** Constant-pool tags whose entry is a fixed number of bytes after the tag. */
const FIXED_WIDTH = {
  3: 4, // Integer
  4: 4, // Float
  5: 8, // Long    — takes two pool slots
  6: 8, // Double  — takes two pool slots
  7: 2, // Class
  8: 2, // String
  9: 4, // Fieldref
  10: 4, // Methodref
  11: 4, // InterfaceMethodref
  12: 4, // NameAndType
  15: 3, // MethodHandle
  16: 2, // MethodType
  17: 4, // Dynamic
  18: 4, // InvokeDynamic
  19: 2, // Module
  20: 2, // Package
};

/** Long and Double occupy two entries; the JVM spec calls this a mistake. */
const DOUBLE_WIDTH_TAGS = new Set([5, 6]);

class Reader {
  constructor(buffer) {
    this.buf = buffer;
    this.pos = 0;
  }

  u1() {
    return this.buf.readUInt8(this.pos++);
  }

  u2() {
    const v = this.buf.readUInt16BE(this.pos);
    this.pos += 2;
    return v;
  }

  u4() {
    const v = this.buf.readUInt32BE(this.pos);
    this.pos += 4;
    return v;
  }

  skip(n) {
    this.pos += n;
  }
}

/** UTF8 entries by index; everything else is skipped but must be walked. */
function readConstantPool(r) {
  const count = r.u2();
  const utf8 = new Map();
  for (let i = 1; i < count; i += 1) {
    const tag = r.u1();
    if (tag === 1) {
      const length = r.u2();
      utf8.set(i, r.buf.toString("utf8", r.pos, r.pos + length));
      r.skip(length);
      continue;
    }
    const width = FIXED_WIDTH[tag];
    if (width === undefined) {
      throw new Error(`unknown constant-pool tag ${tag} at entry ${i}`);
    }
    r.skip(width);
    if (DOUBLE_WIDTH_TAGS.has(tag)) i += 1;
  }
  return utf8;
}

/**
 * Advance past one `element_value`. Only its length matters here — the
 * classifier asks whether an annotation is present, never what it says — but
 * the walk has to be exact or the next annotation starts at the wrong offset.
 */
function skipElementValue(r) {
  const tag = String.fromCharCode(r.u1());
  switch (tag) {
    case "B":
    case "C":
    case "D":
    case "F":
    case "I":
    case "J":
    case "S":
    case "Z":
    case "s":
      r.skip(2); // const_value_index
      return;
    case "e":
      r.skip(4); // type_name_index + const_name_index
      return;
    case "c":
      r.skip(2); // class_info_index
      return;
    case "@":
      skipAnnotation(r);
      return;
    case "[": {
      const n = r.u2();
      for (let i = 0; i < n; i += 1) skipElementValue(r);
      return;
    }
    default:
      throw new Error(`unknown element_value tag '${tag}'`);
  }
}

/** Read one annotation's type descriptor and step over its element pairs. */
function readAnnotation(r, utf8) {
  const typeIndex = r.u2();
  const pairs = r.u2();
  for (let i = 0; i < pairs; i += 1) {
    r.skip(2); // element_name_index
    skipElementValue(r);
  }
  return utf8.get(typeIndex) ?? null;
}

function skipAnnotation(r) {
  r.skip(2); // type_index
  const pairs = r.u2();
  for (let i = 0; i < pairs; i += 1) {
    r.skip(2);
    skipElementValue(r);
  }
}

/** `Lcom/demcha/compose/document/api/Beta;` → `com.demcha.compose.document.api.Beta`. */
function descriptorToBinaryName(descriptor) {
  if (!descriptor || descriptor[0] !== "L" || !descriptor.endsWith(";")) return null;
  return descriptor.slice(1, -1).replace(/\//g, ".");
}

/**
 * Read the annotation list out of one attribute table, and return it. Every
 * other attribute is stepped over using its own declared length, so this stays
 * correct against class-file features it has never heard of.
 */
function readAttributesForAnnotations(r, utf8) {
  const found = [];
  const count = r.u2();
  for (let i = 0; i < count; i += 1) {
    const nameIndex = r.u2();
    const length = r.u4();
    const name = utf8.get(nameIndex);
    if (name !== "RuntimeVisibleAnnotations") {
      r.skip(length);
      continue;
    }
    const end = r.pos + length;
    const n = r.u2();
    for (let a = 0; a < n; a += 1) {
      const binary = descriptorToBinaryName(readAnnotation(r, utf8));
      if (binary) found.push(binary);
    }
    r.pos = end; // trust the declared length over our own walk
  }
  return found;
}

const PRIMITIVES = {
  B: "byte", C: "char", D: "double", F: "float",
  I: "int", J: "long", S: "short", Z: "boolean", V: "void",
};

/**
 * The erased parameter types a JVM method descriptor declares, spelled the way
 * `simplifyType` spells them: package dropped, nested types joined with a dot.
 *
 * Arity alone is not enough to identify a method here.
 * `ShapeContainerBuilder` declares `path(double, double, List<DocumentPathSegment>)`
 * and `path(double, double, SvgPath)` — same name, same arity, and only the
 * second is `@Beta`. Keying on arity would have to either union the two (marking
 * a stable overload beta) or refuse. Erased parameter types separate them, and
 * are derivable exactly from both the descriptor and javap's rendering.
 */
export function descriptorParamTypes(descriptor) {
  const close = descriptor.lastIndexOf(")");
  if (descriptor[0] !== "(" || close === -1) return [];
  const types = [];
  let i = 1;
  while (i < close) {
    let arrayDepth = 0;
    while (descriptor[i] === "[") {
      arrayDepth += 1;
      i += 1;
    }
    let name;
    if (descriptor[i] === "L") {
      const end = descriptor.indexOf(";", i);
      name = descriptor.slice(i + 1, end).replace(/\//g, ".");
      name = name.slice(name.lastIndexOf(".") + 1).replace(/\$/g, ".");
      i = end + 1;
    } else {
      name = PRIMITIVES[descriptor[i]] ?? descriptor[i];
      i += 1;
    }
    types.push(name + "[]".repeat(arrayDepth));
  }
  return types;
}

/**
 * The same key, built from a member as `javap` renders it. Generic arguments
 * are dropped because a descriptor has none, and a vararg is an array.
 */
export function memberKey(name, paramTypes) {
  const erased = paramTypes.map((raw) =>
    raw
      .replace(/<[^<>]*(?:<[^<>]*>)?[^<>]*>/g, "")
      .replace(/\.\.\.$/, "[]")
      .trim(),
  );
  return `${name}(${erased.join(",")})`;
}

/**
 * A class file always calls a constructor `<init>`. `javap` does not — it
 * renames one to the simple type name.
 *
 * So a member read from javap arrives as `Foo(int)` while the annotations
 * recorded against it sit under `<init>(int)`, and those keys can never meet.
 * The failure is silent and in the worst direction: an `@Internal` constructor
 * reaches the surface as public API, and a `@Beta` one reads as settled. Both
 * annotations list `ElementType.CONSTRUCTOR`, so this is a declared part of the
 * contract, not a corner of it.
 *
 * Callers go through this rather than calling `memberKey` directly, so the rule
 * lives in one place instead of being re-derived per call site — which is how it
 * came to be missing at the only call site that mattered.
 */
export const CONSTRUCTOR_NAME = "<init>";

export function memberKeyForMember(member) {
  const name = member.kind === "constructor" ? CONSTRUCTOR_NAME : member.name;
  const params = (member.params ?? []).map((p) => (typeof p === "string" ? p : p.type));
  return memberKey(name, params);
}

/**
 * Runtime-visible annotations declared on a class and on its methods.
 *
 * Methods are keyed `name(ErasedParam,…)` rather than by raw descriptor,
 * because the caller matches against members read by `javap`, whose rendering
 * carries generic arguments a descriptor does not. Both sides reduce to the same
 * erased spelling — see `descriptorParamTypes` and `memberKey`.
 *
 * `ambiguous` should stay empty; a key landing in it means two members reduced
 * to one spelling and disagreed about their annotations, which the caller must
 * refuse to guess about rather than silently pick a winner.
 *
 * @returns {{type: string[], methods: Map<string, string[]>, ambiguous: string[]}}
 */
export function readAnnotations(classBytes) {
  const r = new Reader(classBytes);

  if (r.u4() !== 0xcafebabe) throw new Error("not a class file");
  r.skip(4); // minor + major

  const utf8 = readConstantPool(r);

  r.skip(6); // access_flags + this_class + super_class
  const interfaces = r.u2();
  r.skip(interfaces * 2);

  // Fields: walked, not collected. A field carries no classification of its own
  // that the surface reads — constants inherit their declaring type's.
  const fields = r.u2();
  for (let i = 0; i < fields; i += 1) {
    r.skip(6); // access_flags + name_index + descriptor_index
    readAttributesForAnnotations(r, utf8);
  }

  const methods = new Map();
  const seen = new Map();
  const ambiguous = new Set();
  const methodCount = r.u2();
  for (let i = 0; i < methodCount; i += 1) {
    r.skip(2); // access_flags
    const nameIndex = r.u2();
    const descriptorIndex = r.u2();
    const annotations = readAttributesForAnnotations(r, utf8);

    const name = utf8.get(nameIndex);
    const descriptor = utf8.get(descriptorIndex) ?? "";
    const key = memberKey(name, descriptorParamTypes(descriptor));

    const previous = seen.get(key);
    const signature = annotations.slice().sort().join(",");
    if (previous !== undefined && previous !== signature) ambiguous.add(key);
    seen.set(key, signature);

    if (annotations.length) {
      const merged = new Set([...(methods.get(key) ?? []), ...annotations]);
      methods.set(key, [...merged]);
    }
  }

  const type = readAttributesForAnnotations(r, utf8);

  return { type, methods, ambiguous: [...ambiguous] };
}
