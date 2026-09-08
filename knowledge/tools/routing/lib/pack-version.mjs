/**
 * knowledge/tools/routing/lib/pack-version.mjs — what a pack version is, and
 * when a route is genuinely behind one.
 *
 * Comparing these as strings is the bug this module exists to stop. The surfaces
 * are stamped from the reactor pom, so during a cycle they read
 * `2.4.0-SNAPSHOT` and at the tag `extract-api` re-stamps them `2.4.0` — while
 * `tasks.json` still says `2.4.0-SNAPSHOT`, because a route is signed when it is
 * written. String inequality calls every route in the file stale at every
 * release, which is the same as calling none of them stale.
 *
 * So versions are ordered by their numeric head, and the qualifier is dropped:
 * `2.4.0-SNAPSHOT` and `2.4.0` are one line of development.
 *
 * **This module has one deliberate duplicate.** `api-query.mjs` carries its own
 * copy of {@link compareVersions}, because `build-bundle.mjs` ships that file
 * alone as `bin/query.mjs` with no siblings beside it — an import here would
 * make the published bundle fail to start. The copy is not free-floating:
 * `test/pack-version.test.mjs` runs the same table through both, so the two
 * cannot drift without a red build.
 */

/**
 * The numeric head of a version, or null when it is not orderable.
 *
 * @param {string} version e.g. `2.4.0-SNAPSHOT`
 * @returns {number[]|null} e.g. `[2, 4, 0]`
 */
export function versionParts(version) {
  const head = String(version).trim().split("-")[0];
  const parts = head.split(".");
  if (!parts.length || parts.some((p) => !/^\d+$/.test(p))) return null;
  return parts.map(Number);
}

/**
 * Order a route's version against the pack's.
 *
 * @param {string} routeVersion the route's `verifiedAgainst`
 * @param {string} packVersion the version stamped on the surfaces
 * @returns {-1|0|1|null} -1 route behind · 0 same line · 1 route ahead · null unorderable
 */
export function compareVersions(routeVersion, packVersion) {
  const a = versionParts(routeVersion);
  const b = versionParts(packVersion);
  if (!a || !b) return null;
  for (let i = 0; i < Math.max(a.length, b.length); i += 1) {
    const diff = (a[i] ?? 0) - (b[i] ?? 0);
    if (diff !== 0) return diff < 0 ? -1 : 1;
  }
  return 0;
}

/**
 * The version a set of surface documents was extracted from.
 *
 * Reads them in a caller-supplied order and takes the first stamp. The order
 * has to be stable: taking whichever file the filesystem happens to yield first
 * made the gate's verdict depend on directory iteration, which differs between
 * NTFS name order and the runner's.
 *
 * `targetVersion` is deliberately not a fallback here. It is a line (`2.4.x`),
 * not a version, and nothing can order it — accepting it would put a value into
 * the comparison that {@link versionParts} has to reject.
 *
 * @param {object[]} surfaceDocs parsed surface JSON, in a stable order
 * @returns {string|null} the stamped version, or null when none carries one
 */
export function packVersionOf(surfaceDocs) {
  for (const doc of surfaceDocs) {
    if (doc?.verifiedAgainst) return doc.verifiedAgainst;
  }
  return null;
}
