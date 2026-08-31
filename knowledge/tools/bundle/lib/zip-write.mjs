/**
 * knowledge/tools/bundle/lib/zip-write.mjs — write the archive `zip.mjs` reads.
 *
 * The companion to `../../api-surface/lib/zip.mjs`, which already understands
 * this format from the reading side: stored (0) and deflate (8) entries with a
 * classic, non-zip64 central directory. Writing the same subset means the bundle
 * can be verified by round-tripping it through the reader this repository
 * already trusts, which is a better test than any assertion about bytes.
 *
 * Written rather than depended on for the same reason `zip.mjs` was: the
 * repository ships no dependencies, and a build tool that pulls one in to make
 * an archive puts a `node_modules` install on the critical path of a release.
 *
 * Deterministic on purpose. Entries are written in the order given, and every
 * timestamp is fixed rather than taken from the clock — two runs over the same
 * content must produce the same archive, or the checksum beside it describes the
 * moment it was built instead of what is in it.
 */

import zlib from "node:zlib";

const LOCAL_SIGNATURE = 0x04034b50;
const CENTRAL_SIGNATURE = 0x02014b50;
const EOCD_SIGNATURE = 0x06054b50;

/**
 * A fixed DOS timestamp: 1980-01-01 00:00:00, the epoch of the format itself.
 *
 * A real time here would make every build differ from the last, so the archive's
 * own checksum would change when nothing in it did — and a checksum that moves
 * on its own is one nobody can use to detect that something moved.
 */
const DOS_TIME = 0;
const DOS_DATE = 0x0021;

const CRC_TABLE = (() => {
  const table = new Int32Array(256);
  for (let i = 0; i < 256; i += 1) {
    let c = i;
    for (let k = 0; k < 8; k += 1) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    table[i] = c;
  }
  return table;
})();

function crc32(buffer) {
  let c = 0 ^ -1;
  for (let i = 0; i < buffer.length; i += 1) {
    c = (c >>> 8) ^ CRC_TABLE[(c ^ buffer[i]) & 0xff];
  }
  return (c ^ -1) >>> 0;
}

/**
 * Build a zip from `[name, contents]` pairs.
 *
 * @param {Array<[string, Buffer]>} entries slash-separated names, as a zip spells them
 * @returns {Buffer}
 */
export function writeZip(entries) {
  const locals = [];
  const centrals = [];
  let offset = 0;

  for (const [name, contents] of entries) {
    const nameBytes = Buffer.from(name, "utf8");
    const crc = crc32(contents);

    // Deflate unless it makes the entry bigger, which it does for very short
    // files. A stored entry is read by the same code path, so this costs nothing
    // but bytes saved.
    const deflated = zlib.deflateRawSync(contents, { level: 9 });
    const useDeflate = deflated.length < contents.length;
    const payload = useDeflate ? deflated : contents;
    const method = useDeflate ? 8 : 0;

    const local = Buffer.alloc(30);
    local.writeUInt32LE(LOCAL_SIGNATURE, 0);
    local.writeUInt16LE(20, 4); // version needed
    local.writeUInt16LE(0, 6); // flags
    local.writeUInt16LE(method, 8);
    local.writeUInt16LE(DOS_TIME, 10);
    local.writeUInt16LE(DOS_DATE, 12);
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(payload.length, 18);
    local.writeUInt32LE(contents.length, 22);
    local.writeUInt16LE(nameBytes.length, 26);
    local.writeUInt16LE(0, 28); // extra length

    locals.push(local, nameBytes, payload);

    const central = Buffer.alloc(46);
    central.writeUInt32LE(CENTRAL_SIGNATURE, 0);
    central.writeUInt16LE(20, 4); // version made by
    central.writeUInt16LE(20, 6); // version needed
    central.writeUInt16LE(0, 8); // flags
    central.writeUInt16LE(method, 10);
    central.writeUInt16LE(DOS_TIME, 12);
    central.writeUInt16LE(DOS_DATE, 14);
    central.writeUInt32LE(crc, 16);
    central.writeUInt32LE(payload.length, 20);
    central.writeUInt32LE(contents.length, 24);
    central.writeUInt16LE(nameBytes.length, 28);
    central.writeUInt16LE(0, 30); // extra
    central.writeUInt16LE(0, 32); // comment
    central.writeUInt16LE(0, 34); // disk
    central.writeUInt16LE(0, 36); // internal attributes
    central.writeUInt32LE(0, 38); // external attributes
    central.writeUInt32LE(offset, 42);

    centrals.push(central, nameBytes);
    offset += local.length + nameBytes.length + payload.length;
  }

  const centralDirectory = Buffer.concat(centrals);

  const eocd = Buffer.alloc(22);
  eocd.writeUInt32LE(EOCD_SIGNATURE, 0);
  eocd.writeUInt16LE(0, 4); // this disk
  eocd.writeUInt16LE(0, 6); // disk with central directory
  eocd.writeUInt16LE(entries.length, 8);
  eocd.writeUInt16LE(entries.length, 10);
  eocd.writeUInt32LE(centralDirectory.length, 12);
  eocd.writeUInt32LE(offset, 16);
  eocd.writeUInt16LE(0, 20); // comment length

  return Buffer.concat([...locals, centralDirectory, eocd]);
}
