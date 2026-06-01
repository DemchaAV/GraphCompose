/** @type {import('next').NextConfig} */
const nextConfig = {
  // Fully static build — `next build` emits ./out, deployable to
  // GitHub Pages / Vercel / any static host. No SSR, no server runtime.
  output: "export",
  images: { unoptimized: true },
  // If you deploy under a sub-path on GitHub Pages (e.g. user.github.io/graph-compose),
  // uncomment and set these:
  // basePath: "/graph-compose",
  // assetPrefix: "/graph-compose/",
  trailingSlash: true,
};

export default nextConfig;
