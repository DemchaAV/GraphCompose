/**
 * tools/api-surface/lib/zip.mjs — read entries out of a jar.
 *
 * A jar is a zip, and the two things the extractor needs from one are the list
 * of class names it contains and the text of a `.java` file inside a sources
 * jar. Both come from the central directory plus `inflateRaw`, which node has.
 *
 * Written rather than depended on because the repository root ships no
 * dependencies at all: adding one so a build tool can open a zip would put a
 * node_modules on the critical path of `api-query`, which is supposed to answer
 * in milliseconds from a clean checkout.
 *
 * Only what a jar actually uses is supported: stored (0) and deflate (8), with
 * the classic (non-zip64) central directory. A jar too large for that is a jar
 * this project does not produce.
 */

import fs from "node:fs";
import zlib from "node:zlib";

const EOCD_SIGNATURE = 0x06054b50;
const CENTRAL_SIGNATURE = 0x02014b50;
const MAX_COMMENT = 0xffff;

function findEndOfCentralDirectory(buf) {
  // The record is at the end, after a comment of unknown length, so it is found
  // by scanning backwards for its signature.
  const from = Math.max(0, buf.length - MAX_COMMENT - 22);
  for (let i = buf.length - 22; i >= from; i -= 1) {
    if (buf.readUInt32LE(i) === EOCD_SIGNATURE) return i;
  }
  throw new Error("not a zip archive: no end-of-central-directory record");
}

/**
 * Open a jar and index its entries.
 *
 * @returns {{names: string[], read(name: string): Buffer}}
 */
export function openJar(file) {
  const buf = fs.readFileSync(file);
  const eocd = findEndOfCentralDirectory(buf);
  const count = buf.readUInt16LE(eocd + 10);
  let offset = buf.readUInt32LE(eocd + 16);

  const entries = new Map();
  for (let i = 0; i < count; i += 1) {
    if (buf.readUInt32LE(offset) !== CENTRAL_SIGNATURE) {
      throw new Error(`corrupt central directory in ${file} at entry ${i}`);
    }
    const method = buf.readUInt16LE(offset + 10);
    const compressedSize = buf.readUInt32LE(offset + 20);
    const nameLength = buf.readUInt16LE(offset + 28);
    const extraLength = buf.readUInt16LE(offset + 30);
    const commentLength = buf.readUInt16LE(offset + 32);
    const localHeader = buf.readUInt32LE(offset + 42);
    const name = buf.toString("utf8", offset + 46, offset + 46 + nameLength);
    entries.set(name, { method, compressedSize, localHeader });
    offset += 46 + nameLength + extraLength + commentLength;
  }

  return {
    names: [...entries.keys()],
    read(name) {
      const entry = entries.get(name);
      if (!entry) throw new Error(`no such entry in ${file}: ${name}`);
      // The local header repeats the name and extra fields with its own
      // lengths, which are the ones that decide where the data starts.
      const localNameLength = buf.readUInt16LE(entry.localHeader + 26);
      const localExtraLength = buf.readUInt16LE(entry.localHeader + 28);
      const start = entry.localHeader + 30 + localNameLength + localExtraLength;
      const raw = buf.subarray(start, start + entry.compressedSize);
      if (entry.method === 0) return Buffer.from(raw);
      if (entry.method === 8) return zlib.inflateRawSync(raw);
      throw new Error(`unsupported compression method ${entry.method} for ${name}`);
    },
  };
}
