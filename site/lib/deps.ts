/**
 * Install snippets surfaced in the Cta footer. Coordinates match
 * the latest Maven Central release. Bump in lock-step with
 * `scripts/cut-release.ps1` at release time — the cut script does
 * not touch this file, so a stale version here would silently ship
 * to the showcase site even after a successful release.
 */
export const DEPS = {
  maven: `<dependency>
  <groupId>io.github.demchaav</groupId>
  <artifactId>graph-compose</artifactId>
  <version>1.6.8</version>
</dependency>`,
  gradle: `implementation("io.github.demchaav:graph-compose:1.6.8")`,
} as const;

export type DepKind = keyof typeof DEPS;
