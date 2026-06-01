/**
 * Base-path prefixing for public/ assets.
 *
 * Next.js's `basePath` config auto-prefixes routes and `<Image>` /
 * `<Link>` href values, but plain `<img src>` / `<object data>` /
 * any string-literal asset path in our own code is left untouched
 * — it would resolve against the host root and 404 on a project
 * Pages deployment (e.g. `demchaav.github.io/GraphCompose/`).
 *
 * Use `withBasePath('/brand/logo.png')` so the same code works
 * locally (basePath = "") and on Pages (basePath = "/GraphCompose").
 *
 * The base path is sourced from `NEXT_PUBLIC_BASE_PATH` at build
 * time, which `next.config.mjs` keeps in lockstep with the
 * `basePath` / `assetPrefix` it sets on the Next.js config.
 */
const RAW = (process.env.NEXT_PUBLIC_BASE_PATH ?? "").replace(/\/$/, "");

export const basePath: string = RAW;

export function withBasePath(path: string): string {
  // External / hash / data URLs pass through untouched.
  if (!path) return path;
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  if (path.startsWith("#") || path.startsWith("mailto:") || path.startsWith("data:")) return path;
  // Avoid double-prefixing when callers already did the work.
  if (RAW && path.startsWith(RAW + "/")) return path;
  // Ensure a leading slash on inputs that lack one.
  const normalised = path.startsWith("/") ? path : `/${path}`;
  return `${RAW}${normalised}`;
}
