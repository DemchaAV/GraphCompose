/** @type {import('next').NextConfig} */

// Sub-path under which the site is hosted on GitHub Pages.
// This is a PROJECT page (demchaav.github.io/<repo>/), so every
// emitted URL needs the /GraphCompose prefix — otherwise `_next/`
// static chunks, the logo, and every public/ asset 404 against
// the host root. Set the same value on `basePath`, `assetPrefix`,
// and `NEXT_PUBLIC_BASE_PATH` so our own `withBasePath(...)` helper
// (lib/base-path.ts) sees it at runtime.
//
// Override to "" for local `next dev` if you prefer accessing the
// site at http://localhost:5173/ instead of /GraphCompose/.
const BASE_PATH = process.env.NEXT_PUBLIC_BASE_PATH ?? "/GraphCompose";

const nextConfig = {
  // Fully static build — `next build` emits ./out, deployable to
  // GitHub Pages / Vercel / any static host. No SSR, no server runtime.
  output: "export",
  images: { unoptimized: true },
  basePath: BASE_PATH || undefined,
  assetPrefix: BASE_PATH ? `${BASE_PATH}/` : undefined,
  env: {
    NEXT_PUBLIC_BASE_PATH: BASE_PATH,
  },
  trailingSlash: true,
};

export default nextConfig;
